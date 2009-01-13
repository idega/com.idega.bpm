package com.idega.bpm.exe;

import java.io.InputStream;
import java.net.URI;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.variables.Variable;
import com.idega.bpm.xformsview.XFormsView;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.cache.IWCacheManager2;
import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessException;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMAccessControlException;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.variables.impl.BinaryVariableImpl;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.34 $
 * 
 *          Last modified: $Date: 2009/01/13 15:58:36 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("defaultTIW")
@Transactional(readOnly = false, noRollbackFor = {
		AccessControlException.class, BPMAccessControlException.class })
public class DefaultBPMTaskInstanceW implements TaskInstanceW {

	private static final String allowSigningVariableRepresentation = "system_allowSigning";

	public static final int PRIORITY_HIDDEN = -21;

	/**
	 * shared task means it can be opened by multiple users at the same time.
	 * New tokens and task instances will be created for each user
	 */
	public static final int PRIORITY_SHARED_TASK = -1;

	private ProcessManager processManager;

	@Autowired
	private TmpFilesManager fileUploadManager;
	@Autowired
	@TmpFileResolverType("defaultResolver")
	private TmpFileResolver uploadedResourceResolver;

	private Long taskInstanceId;
	private TaskInstance taskInstance;

	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMContext bpmContext;
	@Autowired
	private VariablesHandler variablesHandler;

	@Autowired
	private PermissionsFactory permissionsFactory;

	private static final String CASHED_TASK_NAMES = "defaultBPM_taskinstance_names";

	public TaskInstance getTaskInstance() {

		if (true || (taskInstance == null && getTaskInstanceId() != null)) {

			taskInstance = getBpmContext().execute(new JbpmCallback() {

				public Object doInJbpm(JbpmContext context)
						throws JbpmException {
					return context.getTaskInstance(getTaskInstanceId());
				}

			});
		} else if (taskInstance != null) {

			taskInstance = getBpmContext().execute(new JbpmCallback() {

				public Object doInJbpm(JbpmContext context)
						throws JbpmException {

					// USE CONTAINS IN THE ENTITY MANAGER

					return getBpmContext().mergeProcessEntity(taskInstance);
				}
			});
		}
		return taskInstance;
	}

	public void setTaskInstance(TaskInstance taskInstance) {
		this.taskInstance = taskInstance;
	}

	public void assign(User usr) {

		Object pk = usr.getPrimaryKey();
		Integer userId;

		if (pk instanceof Integer)
			userId = (Integer) pk;
		else
			userId = new Integer(pk.toString());

		assign(userId);
	}

	public void assign(final int userId) {

		try {
			getBpmContext().execute(new JbpmCallback() {

				public Object doInJbpm(JbpmContext context)
						throws JbpmException {

					Long taskInstanceId = getTaskInstanceId();
					RolesManager rolesManager = getBpmFactory()
							.getRolesManager();
					rolesManager.hasRightsToAssignTask(taskInstanceId, userId);

					getTaskInstance().setActorId(String.valueOf(userId));
					context.save(getTaskInstance());
					return null;
				}
			});

		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());
		}
	}

	public User getAssignedTo() {

		try {
			return getBpmContext().execute(new JbpmCallback() {

				public Object doInJbpm(JbpmContext context)
						throws JbpmException {

					TaskInstance taskInstance = getTaskInstance();
					String actorId = taskInstance.getActorId();

					User usr;

					if (actorId != null) {

						try {
							int assignedTo = Integer.parseInt(actorId);
							usr = getUserBusiness().getUser(assignedTo);

						} catch (Exception e) {
							Logger.getLogger(getClass().getName()).log(
									Level.SEVERE,
									"Exception while resolving assigned user name for actor id: "
											+ actorId, e);
							usr = null;
						}
					} else
						usr = null;

					return usr;
				}
			});

		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());

		}
	}

	public void start(final int userId) {

		try {
			getBpmContext().execute(new JbpmCallback() {

				public Object doInJbpm(JbpmContext context)
						throws JbpmException {
					Long taskInstanceId = getTaskInstanceId();
					RolesManager rolesManager = getBpmFactory()
							.getRolesManager();
					rolesManager.hasRightsToStartTask(taskInstanceId, userId);

					TaskInstance taskInstance = getTaskInstance();
					taskInstance.start();

					context.save(taskInstance);
					return null;
				}
			});

		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());
		}
	}

	public void submit(ViewSubmission view) {
		submit(view, true);
	}

	public void submit(final ViewSubmission viewSubmission,
			final boolean proceedProcess) {

		getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {
				TaskInstance taskInstance = getTaskInstance();

				if (taskInstance.hasEnded())
					throw new ProcessException("Task instance ("
							+ taskInstance.getId() + ") is already submitted",
							"Task instance is already submitted");

				submitVariablesAndProceedProcess(taskInstance, viewSubmission
						.resolveVariables(), proceedProcess);

				// if priority was hidden, then setting to default priority
				// after
				// submission
				if (taskInstance.getPriority() == PRIORITY_HIDDEN) {
					taskInstance.setPriority(Task.PRIORITY_NORMAL);
				}

				context.save(taskInstance);
				return null;
			}
		});
	}

	protected void submitVariablesAndProceedProcess(TaskInstance ti,
			Map<String, Object> variables, boolean proceed) {

		getVariablesHandler().submitVariables(variables, ti.getId(), true);

		if (proceed) {

			String actionTaken = (String) ti
					.getVariable(ProcessConstants.actionTakenVariableName);
			// TODO
			boolean takeTransitionAction = false;
			if (actionTaken != null) {
				for (Object transition : ti.getAvailableTransitions()) {

					Transition trans = (Transition) transition;

					if (actionTaken.equals(trans.getName()))
						takeTransitionAction = true;
				}
			}

			if (actionTaken != null && actionTaken.length() != 0
					&& takeTransitionAction)
				ti.end(actionTaken);
			else
				ti.end();

		} else {
			ti.setEnd(new Date());
		}

		BPMUser bpmUser = getBpmFactory().getBpmUserFactory()
				.getCurrentBPMUser();

		if (bpmUser != null) {

			Integer usrId = bpmUser.getIdToUse();

			if (usrId != null)
				ti.setActorId(usrId.toString());
		}
	}

	public View loadView() {

		return loadView(true);
	}

	protected View loadView(final boolean loadForDisplay) {

		try {
			return getBpmContext().execute(new JbpmCallback() {

				public Object doInJbpm(JbpmContext context)
						throws JbpmException {

					Long taskInstanceId = getTaskInstanceId();
					TaskInstance taskInstance = getTaskInstance();

					List<String> preferred = new ArrayList<String>(1);
					preferred.add(XFormsView.VIEW_TYPE);

					View view;

					if (taskInstance.hasEnded()) {

						view = getBpmFactory().getViewByTaskInstance(
								taskInstanceId, false, preferred);

					} else {

						if (loadForDisplay) {

							if (taskInstance.getPriority() == PRIORITY_SHARED_TASK) {

								if (true) {
									taskInstanceId = createSharedTask(context,
											taskInstance);
								} else {

									// forever task. The original task instance
									// is
									// kept
									// intact, while new token and task
									// instances
									// are created

									Token currentToken = taskInstance
											.getToken();

									String keepMeAliveTknName = "KeepMeAlive";
									Token keepMeAliveTkn = currentToken
											.getChild(keepMeAliveTknName);

									if (keepMeAliveTkn == null) {

										keepMeAliveTkn = new Token(
												currentToken,
												keepMeAliveTknName);

										context.save(keepMeAliveTkn);
									}

									// providing millis as token unique
									// identifier
									// for
									// parent.
									// This is needed, because parent holds
									// children
									// tokens
									// in the map where key is token name
									Token individualInstanceToken = new Token(
											currentToken,
											"sharedTask_"
											// + taskInstance.getTask()
													// .getName()
													+ System
															.currentTimeMillis());

									context.save(individualInstanceToken);

									taskInstance = taskInstance
											.getTaskMgmtInstance()
											.createTaskInstance(
													taskInstance.getTask(),
													individualInstanceToken);

									// TODO: populate token with
									// currentToken.getParent()
									// variables

									// setting hidden priority, so the task
									// wouldn't
									// appear
									// in the tasks list
									taskInstance.setPriority(PRIORITY_HIDDEN);

									// System.out.println("__saving in asdasd");
									// context.save(taskInstance);

									taskInstanceId = taskInstance.getId();
								}
							}
						}

						if (loadForDisplay) {

							// if full load, then we take view before displaying
							// it
							view = getBpmFactory().takeView(taskInstanceId,
									true, preferred);
						} else {

							// if not full load, then we just get view by task
							view = getBpmFactory().getViewByTask(
									getTaskInstance().getTask().getId(), true,
									preferred);
						}
					}
					if (loadForDisplay) {
						Map<String, String> parameters = new HashMap<String, String>(
								1);
						parameters.put(ProcessConstants.TASK_INSTANCE_ID,
								String.valueOf(taskInstanceId));
						view.populateParameters(parameters);
						view.populateVariables(getVariablesHandler()
								.populateVariables(taskInstanceId));
					}
					view.setTaskInstanceId(taskInstanceId);

					return view;
				}
			});

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Long createSharedTask(JbpmContext context, TaskInstance taskInstance) {

		// forever task. The original task instance is
		// kept
		// intact, while new token and task instances
		// are created

		Token currentToken = taskInstance.getToken();

		String keepMeAliveTknName = "KeepMeAlive";
		Token keepMeAliveTkn = currentToken.getChild(keepMeAliveTknName);

		if (keepMeAliveTkn == null) {

			keepMeAliveTkn = new Token(currentToken, keepMeAliveTknName);

			context.save(keepMeAliveTkn);
		}

		// providing millis as token unique identifier
		// for
		// parent.
		// This is needed, because parent holds children
		// tokens
		// in the map where key is token name
		Token individualInstanceToken = new Token(currentToken, "sharedTask_"
		// + taskInstance.getTask()
				// .getName()
				+ System.currentTimeMillis());

		context.save(individualInstanceToken);

		taskInstance = taskInstance.getTaskMgmtInstance().createTaskInstance(
				taskInstance.getTask(), individualInstanceToken);

		// TODO: populate token with
		// currentToken.getParent()
		// variables

		// setting hidden priority, so the task wouldn't
		// appear
		// in the tasks list
		taskInstance.setPriority(PRIORITY_HIDDEN);

		// System.out.println("__saving in asdasd");
		// context.save(taskInstance);

		return taskInstance.getId();
	}

	public View getView() {

		return loadView(false);
	}

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	protected UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil
					.getIWContext(), UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public String getName(Locale locale) {

		final IWMainApplication iwma = getIWMA();
		@SuppressWarnings("unchecked")
		Map<Long, Map<Locale, String>> cashTaskNames = IWCacheManager2
				.getInstance(iwma).getCache(CASHED_TASK_NAMES);
		final Map<Locale, String> names;
		final Long taskInstanceId = getTaskInstanceId();

		// synchronized (cashTaskNames) {
		// synchronizing on CASHED_TASK_NAMES map, as it's accessed from
		// multiple threads

		if (cashTaskNames.containsKey(taskInstanceId)) {

			names = cashTaskNames.get(getTaskInstanceId());
		} else {

			names = new HashMap<Locale, String>(5);
			cashTaskNames.put(taskInstanceId, names);
		}
		// }

		final String name;

		if (names.containsKey(locale))
			name = names.get(locale);
		else {

			List<String> preferred = new ArrayList<String>(1);
			preferred.add(XFormsView.VIEW_TYPE);

			View taskInstanceView = getView();
			name = taskInstanceView.getDisplayName(locale);
			names.put(locale, name);
		}

		return name;
	}

	public void setTaskRolePermissions(Role role,
			boolean setSameForAttachments, String variableIdentifier) {

		getBpmFactory().getRolesManager().setTaskRolePermissionsTIScope(role,
				getTaskInstanceId(), setSameForAttachments, variableIdentifier);
	}

	private IWMainApplication getIWMA() {

		final IWContext iwc = IWContext.getCurrentInstance();
		final IWMainApplication iwma;
		// trying to get iwma from iwc, if available, downgrading to default
		// iwma, if not

		if (iwc != null) {

			iwma = iwc.getIWMainApplication();

		} else {
			iwma = IWMainApplication.getDefaultIWMainApplication();
		}

		return iwma;
	}

	public BPMContext getBpmContext() {
		return bpmContext;
	}

	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}

	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}

	public BinaryVariable addAttachment(Variable variable, String fileName,
			String description, InputStream is) {

		String filesFolder = getTaskInstanceId() + System.currentTimeMillis()
				+ CoreConstants.SLASH;

		getFileUploadManager().uploadToTmpDir(filesFolder, fileName, is,
				getUploadedResourceResolver());

		Collection<URI> uris = getFileUploadManager().getFilesUris(filesFolder,
				null, getUploadedResourceResolver());
		URI uri = uris.iterator().next();

		List<BinaryVariable> binVars = getVariablesHandler()
				.resolveBinaryVariables(getTaskInstanceId(), variable);

		if (binVars == null)
			binVars = new ArrayList<BinaryVariable>(1);

		Map<String, Object> vars = new HashMap<String, Object>(1);
		String variableName = variable.getDefaultStringRepresentation();

		final BinaryVariableImpl binVar = new BinaryVariableImpl();
		binVar.setUri(uri);
		binVar.setDescription(description);

		binVars.add(binVar);
		vars.put(variableName, binVars);
		vars = getVariablesHandler().submitVariablesExplicitly(vars,
				getTaskInstanceId());

		getFileUploadManager().cleanup(filesFolder, null,
				getUploadedResourceResolver());

		return binVar;
	}

	TmpFilesManager getFileUploadManager() {
		return fileUploadManager;
	}

	TmpFileResolver getUploadedResourceResolver() {
		return uploadedResourceResolver;
	}

	public List<BinaryVariable> getAttachments() {

		List<BinaryVariable> variableList = getVariablesHandler()
				.resolveBinaryVariables(getTaskInstanceId());
		List<BinaryVariable> returnList = new ArrayList<BinaryVariable>();
		if (variableList == null || variableList.size() == 0)
			return returnList;

		RolesManager rolesManager = getBpmFactory().getRolesManager();

		for (BinaryVariable variable : variableList) {
			try {
				Permission permission = getPermissionsFactory()
						.getTaskVariableViewPermission(true, getTaskInstance(),
								variable.getHash().toString());
				rolesManager.checkPermission(permission);
				returnList.add(variable);
			} catch (BPMAccessControlException e) {
				continue;
			}
		}

		return returnList;
	}

	public BinaryVariable getAttachment(Variable variable) {

		for (BinaryVariable binaryVariable : getAttachments()) {

			if (binaryVariable.getVariable().equals(variable)) {
				return binaryVariable;
			}
		}

		return null;
	}

	public boolean isSignable() {
		Map<String, Object> variablesMap = getVariablesHandler()
				.populateVariables(getTaskInstanceId());
		if (variablesMap.keySet().contains(allowSigningVariableRepresentation)) {
			return true;
		}
		return false;
	}

	public ProcessManager getProcessManager() {
		return processManager;
	}

	public void setProcessManager(ProcessManager processManager) {
		this.processManager = processManager;
	}

	public ProcessInstanceW getProcessInstanceW() {

		return getProcessManager().getProcessInstance(
				getTaskInstance().getProcessInstance().getId());
	}

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}
}