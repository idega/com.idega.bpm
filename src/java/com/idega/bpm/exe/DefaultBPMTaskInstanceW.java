package com.idega.bpm.exe;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.faces.component.UIComponent;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.variables.Variable;
import com.idega.bpm.BPMConstants;
import com.idega.bpm.security.TaskPermissionManager;
import com.idega.bpm.xformsview.XFormsView;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.cache.IWCacheManager2;
import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.core.persistence.Param;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.data.MetaData;
import com.idega.data.MetaDataHome;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.event.TaskInstanceSubmitted;
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
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.variables.impl.BinaryVariableImpl;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.presentation.IWContext;
import com.idega.presentation.PDFRenderedComponent;
import com.idega.repository.RepositoryService;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.DBUtil;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.50 $ Last modified: $Date: 2009/05/05 09:04:30 $ by $Author: civilis $
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Service("defaultTIW")
@Transactional(readOnly = false)
public class DefaultBPMTaskInstanceW extends DefaultSpringBean implements TaskInstanceW {

	private static final Logger LOGGER = Logger.getLogger(DefaultBPMTaskInstanceW.class.getName());

	private static final String allowSigningVariableRepresentation = "system_allowSigning";

	/**
	 * task instance is hidden in the task list, _and_ when resolving task instances by task name
	 * (see processInstanceW)
	 */
	public static final int PRIORITY_HIDDEN = -21;
	/**
	 * task instance is hidden in the task list, _but not_ when resolving task instances by task
	 * name (see processInstanceW).
	 */
	public static final int PRIORITY_VALID_HIDDEN = -22;

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

	private static final String CACHED_TASK_NAMES = "defaultBPM_taskinstance_names";

	private Boolean submitted = null;

	@Override
	@Transactional(readOnly = true)
	public TaskInstance getTaskInstance(JbpmContext context) {
		Long tiId = getTaskInstanceId();
		if (tiId == null) {
			LOGGER.warning("ID of task instance is unknown!");
			return null;
		}

		taskInstance = context.getTaskInstance(tiId);
		if (taskInstance == null) {
			LOGGER.warning("Error getting task instance by ID: " + tiId + " from context, will try to load directly from DB");
			taskInstance = getBpmFactory().getBPMDAO().getSingleResultByInlineQuery("from " + TaskInstance.class.getName() + " t where t.id = :id", TaskInstance.class, new Param("id", tiId));
		}
		if (taskInstance == null) {
			LOGGER.warning("Failed to resolve task instance by ID: " + tiId);
		}

		return taskInstance;
	}

	@Override
	@Transactional(readOnly = true)
	public TaskInstance getTaskInstance() {
		taskInstance = getBpmContext().execute(new JbpmCallback<TaskInstance>() {
			@Override
			public TaskInstance doInJbpm(JbpmContext context) throws JbpmException {
				return getTaskInstance(context);
			}
		});
		return taskInstance;
	}

	@Override
	public void setTaskInstance(TaskInstance taskInstance) {
		this.taskInstance = taskInstance;
	}

	@Override
	public void assign(User usr) {

		Object pk = usr.getPrimaryKey();
		Integer userId;

		if (pk instanceof Integer)
			userId = (Integer) pk;
		else
			userId = new Integer(pk.toString());

		assign(userId);
	}

	@Override
	@Transactional(readOnly = false)
	public void assign(final int userId) {
		try {
			getBpmContext().execute(new JbpmCallback<Void>() {

				@Override
				public Void doInJbpm(JbpmContext context) throws JbpmException {
					Long taskInstanceId = getTaskInstanceId();
					RolesManager rolesManager = getBpmFactory().getRolesManager();
					rolesManager.hasRightsToAssignTask(taskInstanceId, userId);

					TaskInstance ti = getTaskInstance(context);
					ti.setActorId(String.valueOf(userId));
					context.save(ti);
					return null;
				}
			});
		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public User getAssignedTo() {
		try {
			return getBpmContext().execute(new JbpmCallback<User>() {

				@Override
				public User doInJbpm(JbpmContext context) throws JbpmException {
					TaskInstance taskInstance = getTaskInstance(context);
					String actorId = taskInstance.getActorId();

					User usr;
					if (actorId != null) {
						try {
							int assignedTo = Integer.parseInt(actorId);
							usr = getUserBusiness().getUser(assignedTo);
						} catch (Exception e) {
							LOGGER.log(Level.SEVERE, "Exception while resolving assigned user name for actor id: " + actorId, e);
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

	@Override
	@Transactional(readOnly = false)
	public void start(final int userId) {
		try {
			getBpmContext().execute(new JbpmCallback<Void>() {

				@Override
				public Void doInJbpm(JbpmContext context) throws JbpmException {
					Long taskInstanceId = getTaskInstanceId();
					RolesManager rolesManager = getBpmFactory().getRolesManager();
					rolesManager.hasRightsToStartTask(taskInstanceId, userId);

					TaskInstance taskInstance = getTaskInstance(context);
					taskInstance.start();

					context.save(taskInstance);
					return null;
				}
			});
		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());
		}
	}

	@Override
	public void submit(ViewSubmission view) {
		submit(view, true);
	}

	@Override
	public void submit(JbpmContext context, ViewSubmission view) {
		submit(context, view, true);
	}

	@Override
	public void submit(final ViewSubmission viewSubmission, final boolean proceedProcess) {
		getBpmContext().execute(new JbpmCallback<Void>() {

			@Override
			public Void doInJbpm(JbpmContext context) throws JbpmException {
				submit(context, viewSubmission, proceedProcess);
				return null;
			}
		});
	}

	@Transactional(readOnly = false)
	private void submit(JbpmContext context, ViewSubmission viewSubmission, boolean proceedProcess) {
		TaskInstance taskInstance = getTaskInstance(context);
		if (taskInstance == null) {
			throw new ProcessException("Task instance (ID: " + getTaskInstanceId() + ") does not exist!", "Task instance does not exist");
		}
		if (taskInstance.hasEnded()) {
			throw new ProcessException("Task instance (ID: " + taskInstance.getId() + ") is already submitted", "Task instance is already submitted");
		}

		boolean success = true;
		Long piId = null, tiId = null;
		Map<String, Object> variables = null;
		try {
			tiId = taskInstance.getId();

			ProcessInstance pi = taskInstance.getProcessInstance();
			if (pi == null) {
				piId = viewSubmission.getProcessInstanceId();
				if (piId == null) {
					success = false;
					throw new ProcessException("Process instance is unknown for task instance (ID: " + tiId + ")", "Process instance is unknown for task instance (ID: " + tiId + ")");
				}
			}
			if (piId == null && pi != null) {
				piId = pi.getId();
			}

			if (!canSubmit(context, pi, piId, tiId)) {
				success = false;
				throw new ProcessException("Task instance (ID: " + tiId + ") can not be submited. Process instance ID: " + piId, "Task instance (ID: " + tiId + ") can not be submited. Process instance ID: " + piId);
			}

			variables = viewSubmission.resolveVariables();
			if (variables == null) {
				LOGGER.warning("Variables are unknown, resolving them for task instance: " + tiId);
				TaskInstance ti = context.getTaskInstance(tiId);
				@SuppressWarnings("unchecked")
				Map<String, Object> taskVariables = ti.getVariables();
				viewSubmission.populateVariables(taskVariables);
				variables = viewSubmission.resolveVariables();
			}

			submitVariablesAndProceedProcess(context, piId, taskInstance, variables, proceedProcess);

			// if priority was hidden, then setting to default priority after submission
			if (taskInstance.getPriority() == PRIORITY_HIDDEN) {
				taskInstance.setPriority(Task.PRIORITY_NORMAL);
			}
			context.save(taskInstance);
		} finally {
			if (success) {
				ELUtil.getInstance().publishEvent(new TaskInstanceSubmitted(this, piId, tiId, variables));
			}
		}
	}

	private boolean canSubmit(JbpmContext context, ProcessInstance pi, Long piId, Long tiId) {
		Map<String, TaskPermissionManager> tasksManagers = getBeansOfType(TaskPermissionManager.class);
		if (MapUtil.isEmpty(tasksManagers)) {
			return true;
		}

		Boolean canSubmit = true;
		pi = pi == null ? context.getProcessInstance(piId) : pi;
		String procDefName = context.getGraphSession().getProcessDefinition(pi.getProcessDefinition().getId()).getName();
		for (Iterator<TaskPermissionManager> tasksManagersIter = tasksManagers.values().iterator(); (canSubmit && tasksManagersIter.hasNext());) {
			canSubmit = tasksManagersIter.next().canSubmitTask(tiId, procDefName);
		}
		return canSubmit;
	}

	@Transactional(readOnly = false)
	protected void submitVariablesAndProceedProcess(TaskInstance ti, Map<String, Object> variables, boolean proceed) {
		submitVariablesAndProceedProcess(null, null, ti, variables, proceed);
	}

	@Transactional(readOnly = false)
	private void submitVariablesAndProceedProcess(JbpmContext context, Long piId, TaskInstance ti, Map<String, Object> variables, boolean proceed) {
		if (context == null) {
			getVariablesHandler().submitVariables(variables, ti.getId(), true);
		} else {
			getVariablesHandler().submitVariables(context, variables, ti.getId(), piId, true);
		}

		if (proceed) {
			String actionTaken = (String) ti.getVariable(ProcessConstants.actionTakenVariableName);

			boolean takeTransitionAction = false;
			@SuppressWarnings("unchecked")
			List<Transition> availableTransitions = ti.getAvailableTransitions();
			if (actionTaken != null && !ListUtil.isEmpty(availableTransitions)) {
				for (Iterator<Transition> transIter = availableTransitions.iterator(); (transIter.hasNext() && !takeTransitionAction);) {
					Transition trans = transIter.next();

					if (actionTaken.equals(trans.getName()))
						takeTransitionAction = true;
				}
			}

			if (!StringUtil.isEmpty(actionTaken) && takeTransitionAction) {
				if (hasLeavingTransition(ti, actionTaken)) {
					ti.end(actionTaken);
				} else {
					String allTransitions = CoreConstants.EMPTY;
					if (!ListUtil.isEmpty(availableTransitions)) {
						for (Transition transition: availableTransitions) {
							allTransitions += "Transition name: " + transition.getName() + ", ID: " + transition.getId() + "; ";
						}
					}
					throw new RuntimeException("There is no leaving transition '" + actionTaken + "' for task instance " + ti + ", ID: " + ti.getId() +
							", unable to end task. All available transitions: " + allTransitions);
				}
			} else {
				ti.end();
			}
		} else {
			ti.setEnd(new Date());
		}

		BPMUser bpmUser = getBpmFactory().getBpmUserFactory().getCurrentBPMUser();
		if (bpmUser != null) {
			Integer usrId = bpmUser.getIdToUse();
			if (usrId != null)
				ti.setActorId(usrId.toString());
		}
	}

	private boolean hasLeavingTransition(TaskInstance ti, String transitionName) {
		if (ti == null) {
			LOGGER.warning("Task instance is not provided");
			return false;
		}
		if (StringUtil.isEmpty(transitionName)) {
			LOGGER.warning("Transtion name is not provided for task instance " + ti + ", ID: " + ti.getId());
			return false;
		}

		try {
			Task task = ti.getTask();
			TaskNode node = task.getTaskNode();
			Transition transition = node.getLeavingTransition(transitionName);
			return transition != null;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while verifying if there is leaving transition '" + transitionName + "' for task instnace " + ti +
					", ID: " + ti.getId(), e);
		}
		return false;
	}

	@Override
	public View loadView() {
		return loadView(true, XFormsView.VIEW_TYPE);
	}

	@Override
	public View loadView(String viewType) {
		return loadView(true, viewType, new String[] {viewType});
	}

	@Transactional(readOnly = true)
	protected View loadView(final boolean loadForDisplay, final String viewType, final String... forcedTypes) {
		try {
			return getBpmContext().execute(new JbpmCallback<View>() {

				@Override
				public View doInJbpm(JbpmContext context) throws JbpmException {
					Long taskInstanceId = getTaskInstanceId();
					TaskInstance taskInstance = getTaskInstance(context);

					List<String> preferred = new ArrayList<String>(1);
					preferred.add(XFormsView.VIEW_TYPE);

					View view;

					if (taskInstance.hasEnded()) {
						view = getBpmFactory().getViewByTaskInstance(taskInstanceId, false, preferred, forcedTypes);
					} else {
						if (loadForDisplay) {
							if (taskInstance.getPriority() == ProcessConstants.PRIORITY_SHARED_TASK) {
								taskInstanceId = createSharedTask(taskInstance.getId());
							}
						}

						if (loadForDisplay) {
							// if full load, then we take view before displaying it
							view = getBpmFactory().getViewByTaskInstance(taskInstanceId, true, preferred);
						} else {
							// if not full load, then we just get view by task
							TaskInstance tmpTaskInst = context.getTaskInstance(getTaskInstanceId());
							Task task = tmpTaskInst.getTask();
							view = getBpmFactory().getViewByTask(task.getId(), true, preferred);
						}
					}
					if (loadForDisplay) {
						Map<String, String> parameters = new HashMap<String, String>(1);
						parameters.put(ProcessConstants.TASK_INSTANCE_ID, String.valueOf(taskInstanceId));
						view.populateParameters(parameters);

						view.populateVariables(getVariablesHandler().populateVariables(taskInstanceId));
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

	/**
	 * Shared task. The original task instance is kept intact, while new token and task instances  are created
	 *
	 * @param context
	 * @param taskInstance
	 * @return
	 */
	@Transactional(readOnly = false)
	Long createSharedTask(final Long tiId) {
		return getBpmContext().execute(new JbpmCallback<Long>() {

			@Override
			public Long doInJbpm(JbpmContext context) throws JbpmException {
				TaskInstance taskInstance = context.getTaskInstance(tiId);
				Token currentToken = taskInstance.getToken();

				String keepMeAliveTknName = "KeepMeAlive";
				Token keepMeAliveTkn = currentToken.getChild(keepMeAliveTknName);

				if (keepMeAliveTkn == null) {
					keepMeAliveTkn = new Token(currentToken, keepMeAliveTknName);
					context.save(keepMeAliveTkn);
				}

				// providing millisecond as token unique identifier for parent.
				// This is needed, because parent holds children tokens in the map where key is token name
				Token individualInstanceToken = new Token(currentToken, "sharedTask_" + System.currentTimeMillis());

				taskInstance = taskInstance.getTaskMgmtInstance().createTaskInstance(taskInstance.getTask(), individualInstanceToken);
				// TODO: populate token with currentToken.getParent() variables
				// setting hidden priority, so the task wouldn't appear in the tasks list
				taskInstance.setPriority(PRIORITY_HIDDEN);
				context.save(taskInstance);

				return taskInstance.getId();
			}
		});
	}

	@Override
	public View getView() {
		return loadView(false, XFormsView.VIEW_TYPE);
	}

	@Override
	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	@Override
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
			return IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	@Override
	public String getName(Locale locale) {
		IWMainApplication iwma = getIWMA();
		Map<Long, Map<Locale, String>> cachedTaskNames = IWCacheManager2.getInstance(iwma).getCache(CACHED_TASK_NAMES);
		Map<Locale, String> names = null;
		Long taskInstanceId = getTaskInstanceId();

		if (cachedTaskNames.containsKey(taskInstanceId)) {
			names = cachedTaskNames.get(getTaskInstanceId());
		} else {
			names = new HashMap<Locale, String>(5);
			cachedTaskNames.put(taskInstanceId, names);
		}

		String name = null;
		if (names.containsKey(locale)) {
			name = names.get(locale);
		} else if (!StringUtil.isEmpty(name = getNameFromMetaData(taskInstanceId))) {
			names.put(locale, name);
		} else if (!StringUtil.isEmpty(name = getNameFromMetaData(taskInstanceId, locale))) {
			names.put(locale, name);
		} else {
			name = getNameFromView(locale);
			names.put(locale, name);
		}

		return name;
	}

	private String getNameFromMetaData(final Long taskInstanceId, final Locale locale) {
		return getBpmContext().execute(new JbpmCallback<String>() {

			@Override
			public String doInJbpm(JbpmContext context) throws JbpmException {
				try {
					TaskInstance ti = context.getTaskInstance(taskInstanceId);
					Long procDefId = ti.getProcessInstance().getProcessDefinition().getId();

					String key = BPMConstants.TASK_CUSTOM_NAME_META_DATA.concat(CoreConstants.HASH)
								.concat(ti.getName()).concat(CoreConstants.HASH)
								.concat(String.valueOf(procDefId)).concat(CoreConstants.HASH)
								.concat(locale.toString());

					MetaDataHome metaDataHome = getMetadata();

					Collection<MetaData> data = metaDataHome.findAllByMetaDataNameAndType(key, String.class.getName());
					if (ListUtil.isEmpty(data)) {
						String nameFromView = getNameFromView(locale);
						doCreateMetadata(metaDataHome, key, nameFromView);
						return nameFromView;
					} else {
						return data.iterator().next().getMetaDataValue();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}

	private MetaDataHome getMetadata() {
		MetaDataHome metaDataHome = null;
		try {
			metaDataHome = (MetaDataHome) IDOLookup.getHome(MetaData.class);
		} catch (IDOLookupException e) {
			e.printStackTrace();
		}
		return metaDataHome;
	}

	private void doCreateMetadata(MetaDataHome metaDataHome, String key, String value) throws Exception {
		MetaData nameMeta = metaDataHome.create();
		nameMeta.setName(key);
		nameMeta.setValue(value);
		nameMeta.setType(String.class.getName());
		nameMeta.store();
	}

	private String getValueFromMetadata(String key) {
		try {
			MetaDataHome metaDataHome = getMetadata();

			Collection<MetaData> data = metaDataHome.findAllByMetaDataNameAndType(key, String.class.getName());
			if (!ListUtil.isEmpty(data)) {
				return data.iterator().next().getMetaDataValue();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getNameFromMetaData(Long taskInstanceId) {
		try {
			MetaDataHome metaDataHome = (MetaDataHome) IDOLookup.getHome(MetaData.class);
			Collection<MetaData> data = metaDataHome.findAllByMetaDataNameAndType(
					BPMConstants.TASK_CUSTOM_NAME_META_DATA.concat(BPMConstants.TASK_CUSTOM_NAME_SEPARATOR).concat(String.valueOf(taskInstanceId)),
					String.class.getName()
			);
			return ListUtil.isEmpty(data) ? null : data.iterator().next().getMetaDataValue();
		} catch (FinderException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Transactional(readOnly = true)
	String getNameFromView(Locale locale) {
		View taskInstanceView = getView();
		taskInstanceView.setTaskInstanceId(getTaskInstanceId());
		return taskInstanceView.getDisplayName(locale);
	}

	@Override
	@Transactional(readOnly = false)
	public void setTaskRolePermissions(Role role,
	        boolean setSameForAttachments, String variableIdentifier) {

		getBpmFactory().getRolesManager().setTaskRolePermissionsTIScope(role,
		    getTaskInstanceId(), setSameForAttachments, variableIdentifier);
	}

	@Override
	@Transactional(readOnly = false)
	public void setTaskPermissionsForActors(
	        List<Actor> actorsToSetPermissionsTo, List<Access> accesses,
	        boolean setSameForAttachments, String variableIdentifier) {

		getBpmFactory().getRolesManager().setTaskPermissionsTIScopeForActors(
		    actorsToSetPermissionsTo, accesses, getTaskInstanceId(),
		    setSameForAttachments, variableIdentifier);
	}

	private IWMainApplication getIWMA() {
		final IWContext iwc = CoreUtil.getIWContext();
		final IWMainApplication iwma;

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

	@Override
	public BinaryVariable addAttachment(Variable variable, String fileName, String description, String pathInRepository) {
		InputStream stream = null;
		try {
			RepositoryService repository = ELUtil.getInstance().getBean(RepositoryService.BEAN_NAME);
			stream = repository.getInputStreamAsRoot(pathInRepository);
			return addAttachment(variable, fileName, description, stream);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error adding attachment from repository: " +	pathInRepository, e);
		} finally {
			IOUtil.close(stream);
		}
		return null;
	}

	@Override
	public BinaryVariable addAttachment(Variable variable, String fileName, String description, InputStream is) {
		return addAttachment(variable, fileName, description, is, getTaskInstanceId() + System.currentTimeMillis() + CoreConstants.SLASH);
	}
	@Override
	public BinaryVariable addAttachment(Variable variable, String fileName, String description, InputStream is, String filesFolder) {
		return addAttachment(variable, fileName, description, is, filesFolder, true);
	}

	@Override
	@Transactional(readOnly = false)
	public BinaryVariable addAttachment(Variable variable, String fileName, String description, InputStream is, String filesFolder, boolean overwrite) {
		Collection<URI> uris = getLinksToVariables(is, filesFolder, fileName, overwrite);
		if (ListUtil.isEmpty(uris)) {
			return null;
		}

		URI uri = uris.iterator().next();

		List<BinaryVariable> binVars = getVariablesHandler().resolveBinaryVariables(getTaskInstanceId(), variable);

		if (binVars == null) {
			binVars = new ArrayList<BinaryVariable>(1);
		}

		Map<String, Object> vars = new HashMap<String, Object>(1);
		String variableName = variable.getDefaultStringRepresentation();

		final BinaryVariableImpl binVar = new BinaryVariableImpl();
		binVar.setUri(uri);
		binVar.setDescription(description);
		String mimeType = MimeTypeUtil.resolveMimeTypeFromFileName(fileName);
		binVar.setMimeType(mimeType);
		if (!overwrite) {
			Map<String, Object> metadata = new HashMap<String, Object>();
			metadata.put(JBPMConstants.OVERWRITE, Boolean.FALSE);
			metadata.put(JBPMConstants.PATH_IN_REPOSITORY, filesFolder + fileName);
			binVar.setMetadata(metadata);
			binVar.setPersistedToRepository(true);
		}

		binVars.add(binVar);
		vars.put(variableName, binVars);
		vars = getVariablesHandler().submitVariablesExplicitly(vars, getTaskInstanceId());

		getFileUploadManager().cleanup(filesFolder, null, getUploadedResourceResolver());

		return binVar;
	}

	private Collection<URI> getURIsFromTmpLocation(String folder, String name, InputStream is) {
		getUploadedResourceResolver().uploadToTmpLocation(folder, name, is, false);
		return getFileUploadManager().getFilesUris(folder, null, getUploadedResourceResolver());
	}

	private Collection<URI> getURIsFromRepository(String folder, String name, InputStream stream) {
		try {
			RepositoryService repository = ELUtil.getInstance().getBean(RepositoryService.BEAN_NAME);

			String path = folder + name;
			if (!repository.getExistence(path)) {
				if (!repository.uploadFile(folder, name, null, stream))
					return null;
			}

			URI uri = new URI(repository.getURI(path));
			return Arrays.asList(uri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private Collection<URI> getLinksToVariables(InputStream is, String folder, String name, boolean overwrite) {
		Collection<URI> uris = overwrite ?
				getURIsFromTmpLocation(folder, name, is) :
				getURIsFromRepository(folder, name, is);

		if (ListUtil.isEmpty(uris) && !overwrite)
			uris = getURIsFromTmpLocation(folder, name, is);

		if (ListUtil.isEmpty(uris)) {
			String fixedName = StringHandler.removeWhiteSpace(name);
			fixedName = StringHandler.stripNonRomanCharacters(fixedName, new char[] {'.', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
			if (fixedName.equals(name))
				return null;

			return getLinksToVariables(is, folder, fixedName, overwrite);
		}

		return uris;
	}

	TmpFilesManager getFileUploadManager() {
		return fileUploadManager;
	}

	TmpFileResolver getUploadedResourceResolver() {
		return uploadedResourceResolver;
	}

	@Override
	@Transactional(readOnly = true)
	public List<BinaryVariable> getAttachments() {
		List<BinaryVariable> variableList = getVariablesHandler().resolveBinaryVariables(getTaskInstanceId());
		List<BinaryVariable> returnList = new ArrayList<BinaryVariable>();
		if (ListUtil.isEmpty(variableList)) {
			return returnList;
		}

		RolesManager rolesManager = getBpmFactory().getRolesManager();

		for (BinaryVariable variable: variableList) {
			try {
				Permission permission = getPermissionsFactory().getTaskInstanceVariableViewPermission(
						true,
						getTaskInstance(),
						variable.getHash().toString()
				);
				rolesManager.checkPermission(permission);
				returnList.add(variable);
			} catch (BPMAccessControlException e) {
				continue;
			}
		}

		return returnList;
	}

	@Override
	@Transactional(readOnly = true)
	public List<BinaryVariable> getAttachments(Variable variable) {
		List<BinaryVariable> allAttachments = getAttachments();
		List<BinaryVariable> attachmentsForVariable = new ArrayList<BinaryVariable>(allAttachments.size());

		for (BinaryVariable binaryVariable: allAttachments) {
			if (binaryVariable.getVariable().equals(variable)) {
				attachmentsForVariable.add(binaryVariable);
			}
		}

		return attachmentsForVariable;
	}

	private Boolean signable = null;

	@Override
	@Transactional(readOnly = true)
	public boolean isSignable() {
		if (signable != null) {
			return signable;
		}

		signable = getBpmContext().execute(new JbpmCallback<Boolean>() {

			@Override
			public Boolean doInJbpm(JbpmContext context) throws JbpmException {
				TaskInstance ti = context.getTaskInstance(getTaskInstanceId());
				Object allowedSigning = ti.getVariable(allowSigningVariableRepresentation);
				return allowedSigning == null ? false : Boolean.TRUE.toString().equals(allowedSigning.toString());
			}
		});
		return signable;
	}

	@Override
	public boolean isSubmitted() {
		if (submitted != null)
			return submitted;

		submitted = getTaskInstance().getEnd() == null ? Boolean.FALSE : Boolean.TRUE;

		return submitted;
	}

	public ProcessManager getProcessManager() {
		return processManager;
	}

	@Override
	public void setProcessManager(ProcessManager processManager) {
		this.processManager = processManager;
	}

	@Override
	@Transactional(readOnly = true)
	public ProcessInstanceW getProcessInstanceW() {
		return getBpmContext().execute(new JbpmCallback<ProcessInstanceW>() {

			@Override
			public ProcessInstanceW doInJbpm(JbpmContext context) throws JbpmException {
				return getProcessInstanceW(context);
			}
		});
	}

	@Override
	@Transactional(readOnly = true)
	public ProcessInstanceW getProcessInstanceW(JbpmContext context) {
		TaskInstance ti = getTaskInstance(context);
		if (ti == null) {
			return null;
		}

		Long piId = ti.getProcessInstance().getId();
		return getProcessManager().getProcessInstance(piId);
	}

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	@Override
	public Collection<Role> getRolesPermissions() {
		Collection<Role> roles = getBpmFactory().getRolesManager() .getRolesPermissionsForTaskInstance(getTaskInstanceId(), null);
		return roles;
	}

	@Override
	public Collection<Role> getAttachmentRolesPermissions(String attachmentHashValue) {
		if (StringUtil.isEmpty(attachmentHashValue))
			throw new IllegalArgumentException("Attachment hash value not provided");

		Collection<Role> roles = getBpmFactory().getRolesManager().getRolesPermissionsForTaskInstance(getTaskInstanceId(), attachmentHashValue);

		return roles;
	}

	@Override
	public Object getVariable(String variableName) {
		return getVariablesHandler().populateVariables(getTaskInstanceId()).get(variableName);
	}

	@Override
	public void hide() {
		getTaskInstance().setPriority(PRIORITY_HIDDEN);
	}

	@Override
	public boolean addVariable(Variable variable, Object value) {
		Map<String, Object> variables = new HashMap<String, Object>(1);
		variables.put(variable.getName(), value);
		Map<String, Object> taskVariables = getVariablesHandler().submitVariablesExplicitly(variables, getTaskInstanceId());
		return !MapUtil.isEmpty(taskVariables) && taskVariables.containsKey(variable.getName());
	}

	private Integer order;
	private boolean orderLoaded;
	@Override
	public Integer getOrder() {
		if (!orderLoaded) {
			List<ViewTaskBind> viewTaskBinds = getBpmContext().execute(new JbpmCallback<List<ViewTaskBind>>() {

				@Override
				public List<ViewTaskBind> doInJbpm(JbpmContext context) throws JbpmException {
					TaskInstance ti = getTaskInstance(context);
					return ti == null ? null : getBpmFactory().getBPMDAO().getViewTaskBindsByTaskId(ti.getTask().getId());
				}
			});

			try {
				if (ListUtil.isEmpty(viewTaskBinds))
					return order;

				for (ViewTaskBind bind: viewTaskBinds)
					order = bind.getViewOrder();
			} finally {
				orderLoaded = Boolean.TRUE;
			}
		}

		return order;
	}

	@Override
	public String toString() {
		return "Task instance, ID: " + getTaskInstanceId();
	}

	@Override
	public boolean hasAttachment(String identifier, String variableName) {
		if (StringUtil.isEmpty(identifier) || StringUtil.isEmpty(variableName))
			return false;

		List<BinaryVariable> variables = getVariablesHandler().resolveBinaryVariables(getTaskInstanceId());
		if (ListUtil.isEmpty(variables))
			return false;

		String filesVarPrefix = VariableInstanceType.BYTE_ARRAY.getPrefix();
		if (variableName.startsWith(filesVarPrefix))
			variableName = variableName.substring(variableName.indexOf(filesVarPrefix) + filesVarPrefix.length());

		for (BinaryVariable variable: variables) {
			String varIdentifier = variable.getIdentifier();
			String varName = variable.getVariable().getName();
			if (identifier.equals(varIdentifier) && variableName.equals(varName))
				return true;
		}

		return false;
	}

	private List<Long> getTokensIds(Token token, List<Long> ids) {
		if (ids == null) {
			ids = new ArrayList<Long>();
		}

		if (token == null) {
			return ids;
		}

		token = DBUtil.getInstance().initializeAndUnproxy(token);
		if (token != null) {
			ids.add(token.getId());
		}

		return getTokensIds(token.getParent(), ids);
	}

	@Override
	public Map<String, Object> getVariables(Token token) {
		List<Long> tokensIds = getTokensIds(token, null);
		List<com.idega.jbpm.data.Variable> vars = getBpmFactory().getBPMDAO().getVariablesByTokens(tokensIds);
		if (ListUtil.isEmpty(vars)) {
			return Collections.emptyMap();
		}

		Map<String, Object> variables = new HashMap<String, Object>();
		for (com.idega.jbpm.data.Variable var: vars) {
			if (var == null) {
				continue;
			}

			String name = var.getName();
			if (StringUtil.isEmpty(name)) {
				continue;
			}

			Serializable value = var.getValue();
			variables.put(name, value);
		}
		return variables;
	}

	@Override
	public String getPDFName(Locale locale) {
		String taskName = null;
		try {
			taskName = getName(locale);
			if (StringUtil.isEmpty(taskName)) {
				return null;
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting name for task instance by ID: " + taskInstance + " and locale: " + locale, e);
			return null;
		}

		String caseIdentifier = null;
		try {
			caseIdentifier = getProcessInstanceW().getProcessIdentifier();
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting case identifier for task instance: " + taskInstanceId, e);
		}
		if (StringUtil.isEmpty(caseIdentifier)) {
			return taskName;
		}

		return new StringBuilder(taskName).append(CoreConstants.MINUS).append(caseIdentifier).toString();
	}

	@Override
	public boolean isRenderable() {
		final Map<String, String> renderableCache = getCache("renderableTaskInstancesCache", 604800, 1000);

		AdvancedProperty result = getBpmContext().execute(new JbpmCallback<AdvancedProperty>() {

			@Override
			public AdvancedProperty doInJbpm(JbpmContext context) throws JbpmException {
				AdvancedProperty result = null;
				try {
					TaskInstance ti = context.getTaskInstance(taskInstanceId);
					Long procDefId = ti.getProcessInstance().getProcessDefinition().getId();
					String key = "TASK_IS_RENDERABLE_" + ti.getName() + "_" + procDefId;
					if (key.length() >= 255) {
						key = key.substring(0, 254);
					}
					result = new AdvancedProperty(key);

					if (renderableCache.containsKey(result.getId())) {
						result.setValue(renderableCache.get(result.getId()));
						return result;
					}

					String hasView = getValueFromMetadata(result.getId());
					if (!StringUtil.isEmpty(hasView)) {
						result.setValue(hasView);
						renderableCache.put(result.getId(), hasView);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return result;
			}
		});

		if (result != null && !StringUtil.isEmpty(result.getValue())) {
			return Boolean.valueOf(result.getValue());
		}

		Boolean renderable = isTaskInstanceRenderable(this);

		try {
			MetaDataHome metaDataHome = getMetadata();
			doCreateMetadata(metaDataHome, result.getId(), String.valueOf(renderable));
		} catch (Exception e) {
			e.printStackTrace();
		}

		renderableCache.put(result.getId(), String.valueOf(renderable));
		return renderable;
	}

	private Boolean isTaskInstanceRenderable(TaskInstanceW taskInstance) {
		UIComponent component = null;
		try {
			View view = taskInstance.getView();
			view.setSubmitted(taskInstance.isSubmitted());

			component = view.getViewForDisplay();
			if (component instanceof PDFRenderedComponent) {
				return !((PDFRenderedComponent) component).isPdfViewer();
			}

			return view.hasViewForDisplay();
		} catch(Exception e) {}
		return Boolean.FALSE;
	}

	@Override
	public boolean hasViewForDisplay() {
		final Map<String, String> hasViewForDisplayCache = getCache("hasViewForDisplayTaskInstancesCache", 604800, 1000);

		AdvancedProperty result = getBpmContext().execute(new JbpmCallback<AdvancedProperty>() {

			@Override
			public AdvancedProperty doInJbpm(JbpmContext context) throws JbpmException {
				AdvancedProperty result = null;
				try {
					TaskInstance ti = context.getTaskInstance(taskInstanceId);
					Long procDefId = ti.getProcessInstance().getProcessDefinition().getId();
					String key = "TASK_HAS_VIEW_" + ti.getName() + "_" + procDefId;
					if (key.length() >= 255) {
						key = key.substring(0, 254);
					}
					result = new AdvancedProperty(key);

					if (hasViewForDisplayCache.containsKey(result.getId())) {
						result.setValue(hasViewForDisplayCache.get(result.getId()));
						return result;
					}

					String hasView = getValueFromMetadata(result.getId());
					if (!StringUtil.isEmpty(hasView)) {
						result.setValue(hasView);
						hasViewForDisplayCache.put(result.getId(), hasView);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return result;
			}
		});

		if (result != null && !StringUtil.isEmpty(result.getValue())) {
			return Boolean.valueOf(result.getValue());
		}

		View view = getView();
		boolean hasView = view.hasViewForDisplay();

		try {
			MetaDataHome metaDataHome = getMetadata();
			doCreateMetadata(metaDataHome, result.getId(), String.valueOf(hasView));
		} catch (Exception e) {
			e.printStackTrace();
		}

		hasViewForDisplayCache.put(result.getId(), String.valueOf(hasView));
		return hasView;
	}

}