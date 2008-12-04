package com.idega.bpm.exe;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.bpm.xformsview.XFormsView;
import com.idega.core.cache.IWCacheManager2;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.BPMTypedPermission;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.rights.Right;
import com.idega.jbpm.view.View;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.user.util.UserComparator;
import com.idega.util.CoreUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 * 
 *          Last modified: $Date: 2008/12/04 10:13:14 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("defaultPIW")
public class DefaultBPMProcessInstanceW implements ProcessInstanceW {

	private Long processInstanceId;
	private ProcessInstance processInstance;

	private BPMContext idegaJbpmContext;
	private ProcessManager processManager;
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private PermissionsFactory permissionsFactory;
	@Autowired
	private BPMDAO bpmDAO;

	private static final String CASHED_TASK_NAMES = "defaultBPM_taskinstance_names";

	public List<TaskInstanceW> getAllTaskInstances() {

		return encapsulateInstances(getAllTaskInstancesPRVT());
	}

	@Transactional(readOnly = true)
	Collection<TaskInstance> getAllTaskInstancesPRVT() {

		ProcessInstance processInstance = getProcessInstance();

		List<ProcessInstance> subProcessInstances = getBpmDAO()
				.getSubprocessInstancesOneLevel(processInstance.getId());

		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = processInstance
				.getTaskMgmtInstance().getTaskInstances();

		// resolving task instances from subprocesses

		if (!subProcessInstances.isEmpty()) {

			JbpmContext jctx = getIdegaJbpmContext().createJbpmContext();

			try {
				for (ProcessInstance subProcessInstance : subProcessInstances) {

					// hopefully temporal solution. The entity should be in
					// transaction and persistent (not transient)
					subProcessInstance = (ProcessInstance) jctx.getSession()
							.merge(subProcessInstance);

					@SuppressWarnings("unchecked")
					Collection<TaskInstance> subTaskInstances = subProcessInstance
							.getTaskMgmtInstance().getTaskInstances();
					taskInstances.addAll(subTaskInstances);
				}

			} finally {
				getIdegaJbpmContext().closeAndCommit(jctx);
			}
		}

		return taskInstances;
	}

	public List<TaskInstanceW> getSubmittedTaskInstances() {

		Collection<TaskInstance> taskInstances = getAllTaskInstancesPRVT();

		for (Iterator<TaskInstance> iterator = taskInstances.iterator(); iterator
				.hasNext();) {
			TaskInstance taskInstance = iterator.next();

			if (!taskInstance.hasEnded())
				iterator.remove();
		}

		return encapsulateInstances(taskInstances);
	}

	public List<TaskInstanceW> getUnfinishedTaskInstances(Token rootToken) {

		ProcessInstance processInstance = rootToken.getProcessInstance();

		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = processInstance
				.getTaskMgmtInstance().getUnfinishedTasks(rootToken);

		return encapsulateInstances(taskInstances);
	}

	public List<TaskInstanceW> getAllUnfinishedTaskInstances() {

		ProcessInstance processInstance = getProcessInstance();

		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = processInstance
				.getTaskMgmtInstance().getUnfinishedTasks(
						processInstance.getRootToken());

		@SuppressWarnings("unchecked")
		List<Token> tokens = processInstance.findAllTokens();

		for (Token token : tokens) {

			// root token task instances already in the list
			if (!token.equals(processInstance.getRootToken())) {

				@SuppressWarnings("unchecked")
				Collection<TaskInstance> tis = processInstance
						.getTaskMgmtInstance().getUnfinishedTasks(token);
				taskInstances.addAll(tis);
			}

			ProcessInstance subProcessInstance = token.getSubProcessInstance();

			if (subProcessInstance != null) {

				// add unfinished task instances from subprocesses.
				// HINT: if we need to have more than one level depth of finding
				// those (i.e. process->subprocess->subprocess->task)
				// then we need to do it recursively here

				@SuppressWarnings("unchecked")
				List<Token> subTokens = subProcessInstance.findAllTokens();

				for (Token subToken : subTokens) {

					@SuppressWarnings("unchecked")
					Collection<TaskInstance> tis = subProcessInstance
							.getTaskMgmtInstance().getUnfinishedTasks(subToken);
					taskInstances.addAll(tis);
				}
			}
		}

		// removing hidden task instances
		for (Iterator<TaskInstance> iterator = taskInstances.iterator(); iterator
				.hasNext();) {
			TaskInstance ti = iterator.next();

			if (ti.getPriority() == DefaultBPMTaskInstanceW.PRIORITY_HIDDEN)
				iterator.remove();
		}

		return encapsulateInstances(taskInstances);
	}

	private ArrayList<TaskInstanceW> encapsulateInstances(
			Collection<TaskInstance> taskInstances) {
		ArrayList<TaskInstanceW> instances = new ArrayList<TaskInstanceW>(
				taskInstances.size());

		for (TaskInstance instance : taskInstances) {
			TaskInstanceW tiw = getProcessManager().getTaskInstance(
					instance.getId());
			instances.add(tiw);
		}

		return instances;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Required
	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public ProcessManager getProcessManager() {
		return processManager;
	}

	@Required
	@Resource(name = "defaultBpmProcessManager")
	public void setProcessManager(ProcessManager processManager) {
		this.processManager = processManager;
	}

	public ProcessInstance getProcessInstance() {

		if (processInstance == null && getProcessInstanceId() != null) {

			JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();

			try {
				processInstance = ctx
						.getProcessInstance(getProcessInstanceId());

			} finally {
				getIdegaJbpmContext().closeAndCommit(ctx);
			}
		}
		return processInstance;
	}

	public void assignHandler(Integer handlerUserId) {
	}

	public String getProcessDescription() {

		return null;
	}

	public String getProcessIdentifier() {

		return null;
	}

	public ProcessDefinitionW getProcessDefinitionW() {

		Long pdId = getProcessInstance().getProcessDefinition().getId();
		return getProcessManager().getProcessDefinition(pdId);
	}

	public Integer getHandlerId() {

		return null;
	}

	public List<User> getUsersConnectedToProcess() {

		final Collection<User> users;

		try {
			Long processInstanceId = getProcessInstanceId();
			BPMTypedPermission perm = (BPMTypedPermission) getPermissionsFactory()
					.getRoleAccessPermission(processInstanceId, null, false);
			users = getBpmFactory().getRolesManager().getAllUsersForRoles(null,
					processInstanceId, perm);

		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
					"Exception while resolving all process instance users", e);
			return null;
		}

		if (users != null && !users.isEmpty()) {

			// using separate list, as the resolved one could be cashed (shared)
			// and so
			ArrayList<User> connectedPeople = new ArrayList<User>(users);

			for (Iterator<User> iterator = connectedPeople.iterator(); iterator
					.hasNext();) {

				User user = iterator.next();
				String hideInContacts = user
						.getMetaData(BPMUser.HIDE_IN_CONTACTS);

				if (hideInContacts != null)
					// excluding ones, that should be hidden in contacts list
					iterator.remove();
			}

			try {
				Collections.sort(connectedPeople, new UserComparator(CoreUtil
						.getIWContext().getCurrentLocale()));
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(
						Level.SEVERE,
						"Exception while sorting contacts list ("
								+ connectedPeople + ")", e);
			}

			return connectedPeople;
		}

		return null;
	}

	public boolean hasHandlerAssignmentSupport() {

		return false;
	}

	public void setContactsPermission(Role role, Integer userId) {

		Long processInstanceId = getProcessInstanceId();

		getBpmFactory().getRolesManager().setContactsPermission(role,
				processInstanceId, userId);
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	public void setPermissionsFactory(PermissionsFactory permissionsFactory) {
		this.permissionsFactory = permissionsFactory;
	}

	public ProcessWatch getProcessWatcher() {
		return null;
	}

	public String getName(Locale locale) {
		final IWMainApplication iwma = getIWma();
		@SuppressWarnings("unchecked")
		Map<Long, Map<Locale, String>> cashTaskNames = IWCacheManager2
				.getInstance(iwma).getCache(CASHED_TASK_NAMES);
		final Map<Locale, String> names;
		final Long taskInstanceId = getProcessDefinitionW()
				.getProcessDefinition().getTaskMgmtDefinition().getStartTask()
				.getId();

		if (cashTaskNames.containsKey(taskInstanceId)) {
			names = cashTaskNames.get(taskInstanceId);
		} else {
			names = new HashMap<Locale, String>(5);
			cashTaskNames.put(taskInstanceId, names);
		}
		final String name;

		if (names.containsKey(locale))
			name = names.get(locale);
		else {
			View taskInstanceView = loadView();
			name = taskInstanceView.getDisplayName(locale);
			names.put(locale, name);
		}

		return name;
	}

	private IWMainApplication getIWma() {
		final IWContext iwc = CoreUtil.getIWContext();
		final IWMainApplication iwma;

		if (iwc != null) {
			iwma = iwc.getIWMainApplication();
		} else {
			iwma = IWMainApplication.getDefaultIWMainApplication();
		}

		return iwma;
	}

	public View loadView() {
		Long taskId = getProcessDefinitionW().getProcessDefinition()
				.getTaskMgmtDefinition().getStartTask().getId();
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();

		try {
			List<String> preferred = new ArrayList<String>(1);
			preferred.add(XFormsView.VIEW_TYPE);

			View view = getBpmFactory().getViewByTask(taskId, false, preferred);

			return view;

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	/**
	 * checks right for process instance and current logged in user
	 * 
	 * @param right
	 * @return
	 */
	public boolean hasRight(Right right) {

		return hasRight(right, null);
	}

	/**
	 * checks right for process instance and user provided
	 * 
	 * @param right
	 * @param user
	 *            to check right against
	 * @return
	 */
	public boolean hasRight(Right right, User user) {

		switch (right) {
		case processHandler:

			try {
				Permission perm = getPermissionsFactory().getAccessPermission(
						getProcessInstanceId(), Access.caseHandler, user);
				getBpmFactory().getRolesManager().checkPermission(perm);

				return true;

			} catch (AccessControlException e) {
				return false;
			}

		default:
			throw new IllegalArgumentException("Right type " + right
					+ " not supported for cases process instance");
		}
	}

	BPMDAO getBpmDAO() {
		return bpmDAO;
	}
}