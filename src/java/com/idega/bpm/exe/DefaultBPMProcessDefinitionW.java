package com.idega.bpm.exe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.db.GraphSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.variables.Variable;
import com.idega.bpm.BPMConstants;
import com.idega.bpm.xformsview.XFormsView;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.event.ProcessInstanceCreatedEvent;
import com.idega.jbpm.event.VariableCreatedEvent;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.17 $ Last modified: $Date: 2009/02/16 22:02:37 $ by $Author: donatas $
 */
@Service("defaultPDW")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DefaultBPMProcessDefinitionW extends DefaultSpringBean implements ProcessDefinitionW {

	private Long processDefinitionId;
	private ProcessDefinition processDefinition;

	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMContext bpmContext;
	@Autowired
	private VariablesHandler variablesHandler;
	@Autowired
	private VariableInstanceQuerier querier;

	protected void notifyAboutNewProcess(String procDefName, Long procInstId, Map<String, Object> variables) {
		ELUtil.getInstance().publishEvent(new ProcessInstanceCreatedEvent(procDefName, procInstId, variables));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Variable> getTaskVariableList(final String taskName) {
		return getBpmContext().execute(new JbpmCallback<List<Variable>>() {
			@Override
			public List<Variable> doInJbpm(JbpmContext context) throws JbpmException {
				ProcessDefinition pdef = getProcessDefinition(context);
				Task task = pdef.getTaskMgmtDefinition().getTask(taskName);
				TaskController tiController = task.getTaskController();

				if (tiController == null) {
					return null;
				}

				@SuppressWarnings("unchecked")
				List<VariableAccess> variableAccesses = tiController.getVariableAccesses();
				ArrayList<Variable> variables = new ArrayList<Variable>(variableAccesses.size());

				for (VariableAccess variableAccess : variableAccesses) {
					Variable variable = Variable.parseDefaultStringRepresentation(variableAccess.getVariableName());
					variables.add(variable);
				}

				return variables;
			}
		});
	}

	@Override
	@Transactional(readOnly = true)
	public List<Variable> getTaskVariableWithAccessesList(final String taskName) {
		return getBpmContext().execute(new JbpmCallback<List<Variable>>() {
			@Override
			public List<Variable> doInJbpm(JbpmContext context) throws JbpmException {
				ProcessDefinition pdef = getProcessDefinition(context);
				Task task = pdef.getTaskMgmtDefinition().getTask(taskName);
				TaskController tiController = task.getTaskController();

				if (tiController == null) {
					return null;
				}

				@SuppressWarnings("unchecked")
				List<VariableAccess> variableAccesses = tiController.getVariableAccesses();
				ArrayList<Variable> variables = new ArrayList<Variable>(variableAccesses.size());

				for (VariableAccess variableAccess : variableAccesses) {
					Variable variable = Variable.parseDefaultStringRepresentationWithAccess(variableAccess.getVariableName(), variableAccess.getAccess().toString());
					variables.add(variable);
				}

				return variables;
			}
		});
	}

	@Override
	@Transactional(readOnly = false)
	public <T extends Serializable> T startProcess(IWContext iwc, final ViewSubmission viewSubmission) {
		Long processDefinitionId = viewSubmission.getProcessDefinitionId();

		if (!processDefinitionId.equals(getProcessDefinitionId())) {
			throw new IllegalArgumentException("View submission was for different process definition id than tried to submit to");
		}

		getLogger().info("Starting process for process definition id = " + processDefinitionId);

		Map<String, String> parameters = viewSubmission.resolveParameters();
		getLogger().info("Params " + parameters);

		final Map<String, Object> variables = new HashMap<String, Object>();

		Long piId = null;
		try {
			piId = getBpmContext().execute(new JbpmCallback<Long>() {
				@Override
				public Long doInJbpm(JbpmContext context) throws JbpmException {
					ProcessDefinition pd = getProcessDefinition(context);
					ProcessInstance pi = new ProcessInstance(pd);
					TaskInstance ti = pi.getTaskMgmtInstance().createStartTaskInstance();

					View view = getBpmFactory().getView(viewSubmission.getViewId(), viewSubmission.getViewType(), false);

					// binding view to task instance
					view.getViewToTask().bind(view, ti);

					getLogger().info("New process instance created for the process " + pd.getName());

					pi.setStart(new Date());

					context.getSession().flush();

					variables.putAll(viewSubmission.resolveVariables());
					submitVariablesAndProceedProcess(context, ti, variables, true);

					Long piId = pi.getId();
					return piId;
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		String procDefName = null;
		try {
			procDefName = getBpmFactory().getBPMDAO().getProcessDefinitionNameByProcessDefinitionId(processDefinitionId);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error while trying to get proc. def. name for process instance " + piId, e);
		}

		try {
			@SuppressWarnings("unchecked")
			T result = (T) piId;
			return result;
		} finally {
			if (procDefName != null) {
				notifyAboutNewProcess(procDefName, piId, variables);
			}
		}
	}

	@Override
	public View loadInitView(Integer initiatorId) {
		return loadInitView(initiatorId, null);
	}

	@Override
	@Transactional(readOnly = false)
	public View loadInitView(Integer initiatorId, String identifier) {
		try {
			return getBpmContext().execute(new JbpmCallback<View>() {
				@Override
				public View doInJbpm(JbpmContext context) throws JbpmException {
					Long processDefinitionId = getProcessDefinitionId();
					ProcessDefinition pd = getProcessDefinition(context);

					Long startTaskId = pd.getTaskMgmtDefinition().getStartTask().getId();

					List<String> preferred = new ArrayList<String>();
					preferred.add(XFormsView.VIEW_TYPE);
					View view = getBpmFactory().getViewByTask(startTaskId, true, preferred);
					view.takeView();

					Map<String, String> parameters = new HashMap<String, String>();
					parameters.put(ProcessConstants.START_PROCESS, ProcessConstants.START_PROCESS);
					parameters.put(ProcessConstants.PROCESS_DEFINITION_ID, String.valueOf(processDefinitionId));
					parameters.put(ProcessConstants.VIEW_ID, view.getViewId());
					parameters.put(ProcessConstants.VIEW_TYPE, view.getViewType());
					view.populateParameters(parameters);

					return view;
				}
			});
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> getRolesCanStartProcess(Object context) {
		return null;
	}

	/**
	 * sets roles, whose users can start process (and see application).
	 *
	 * @param roles
	 *            - idega roles keys (<b>not</b> process roles)
	 * @param context
	 *            - some context depending implementation, e.g., roles can start process using
	 *            applications - then context will be application id
	 */
	@Override
	public void setRolesCanStartProcess(List<String> roles, Object context) {
	}

	@Transactional(readOnly = false)
	protected void submitVariablesAndProceedProcess(JbpmContext context, TaskInstance ti, Map<String, Object> variables, boolean proceed) {
		Integer usrId = null;
		try {
			BPMUser bpmUser = getBpmFactory().getBpmUserFactory().getCurrentBPMUser();
			if (bpmUser != null) {
				usrId = bpmUser.getIdToUse();
			} else {
				if (variables.containsKey(BPMConstants.USER_ID)) {
					usrId = Integer.valueOf(variables.get(BPMConstants.USER_ID).toString());
				}
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting ID of a current user", e);
		}
		if (usrId != null) {
			ti.setActorId(usrId.toString());
		}

		getVariablesHandler().submitVariables(context, variables, ti.getId(), true);
		context.getSession().flush();	//	if we are not flushing here, BPM data will be missing for the other nodes
		getLogger().info("Variables were submitted");

		//	Indexing variables
		Long piId = null;
		ProcessInstance pi = null;
		try {
			pi = ti.getProcessInstance();
			piId = pi.getId();

			getVariableInstanceQuerier().doIndexVariables(piId);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error indexing variables " + variables + " for task instance: " + ti + ", proc. inst. ID: " + piId, e);
		}

		if (proceed) {
			String actionTaken = (String) ti.getVariable(ProcessConstants.actionTakenVariableName);
			if (!StringUtil.isEmpty(actionTaken)) {
				getLogger().info("Taken action: " + actionTaken);
			}

			ti.end();
		} else {
			ti.setEnd(new Date());
		}

		getLogger().info("Task instance (name=" + ti.getName() + ", ID=" + ti.getId() + ") was executed");

		try {
			ApplicationContext appContext = ELUtil.getInstance().getApplicationContext();
			appContext.publishEvent(new VariableCreatedEvent(this, pi.getProcessDefinition().getName(), pi.getId(), ti.getId(), variables));
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error publishing VariableCreatedEvent for task instance: " + ti, e);
		}
	}

	private VariableInstanceQuerier getVariableInstanceQuerier() {
		if (querier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return querier;
	}

	@Override
	@Transactional(readOnly = true)
	public String getStartTaskName() {
		Locale locale = getCurrentLocale();
		if (locale == null) {
			locale = ICLocaleBusiness.getLocaleFromLocaleString("is_IS");
		}
		final Locale l = locale;
		return getBpmContext().execute(new JbpmCallback<String>() {

			@Override
			public String doInJbpm(JbpmContext context) throws JbpmException {
				List<String> preferred = new ArrayList<String>(1);
				preferred.add(XFormsView.VIEW_TYPE);

				ProcessDefinition pd = getProcessDefinition(context);
				if (pd == null) {
					return null;
				}
				Long taskId = pd.getTaskMgmtDefinition().getStartTask().getId();

				View view = getBpmFactory().getViewByTask(taskId, false, preferred);

				return view.getDisplayName(l);
			}
		});
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<String> getTaskNodeTransitionsNames(final String taskName) {
		return getBpmContext().execute(new JbpmCallback<Collection<String>>() {

			@Override
			public Collection<String> doInJbpm(JbpmContext context) throws JbpmException {
				ProcessDefinition pdef = getProcessDefinition(context);
				Task task = pdef.getTaskMgmtDefinition().getTask(taskName);

				TaskNode taskNode = task.getTaskNode();
				if (taskNode != null) {
					@SuppressWarnings("unchecked")
					Map<String, Transition> leavingTransitions = taskNode.getLeavingTransitionsMap();
					return leavingTransitions != null ? leavingTransitions.keySet() : null;
				} else {
					// task node is null, when task in start node
					return null;
				}
			}
		});
	}

	@Override
	public <T extends Serializable> T getProcessDefinitionId() {
		@SuppressWarnings("unchecked")
		T id = (T) processDefinitionId;
		return id;
	}

	@Override
	public void setProcessDefinitionId(Serializable processDefinitionId) {
		if (processDefinitionId instanceof Number) {
			this.processDefinitionId = ((Number) processDefinitionId).longValue();
		}
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
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
	public <T> T getProcessDefinition() {
		processDefinition = getBpmContext().execute(new JbpmCallback<ProcessDefinition>() {

			@Override
			public ProcessDefinition doInJbpm(JbpmContext context) throws JbpmException {
				return getProcessDefinition(context);
			}

		});

		if (processDefinition == null) {
			return null;
		}

		@SuppressWarnings("unchecked")
		T definition = (T) processDefinition;
		return definition;
	}

	@Override
	@Transactional(readOnly = false)
	public <T> T getProcessDefinition(JbpmContext context) {
		Serializable id = getProcessDefinitionId();
		if (!(id instanceof Number)) {
			return null;
		}

		GraphSession jBPMSession = context.getGraphSession();
		if (jBPMSession != null) {
			try {
				processDefinition = jBPMSession.getProcessDefinition(((Number) id).longValue());
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "No process definition found in JBPM context, probably it is BPMN 2 process...", e);
			}
		}

		@SuppressWarnings("unchecked")
		T definition = (T) processDefinition;
		return definition;
	}

	@Override
	public String getProcessName(Locale locale) {
		ProcessDefinition processDefinition = getProcessDefinition();
		return processDefinition == null ? null : processDefinition.getName();
	}

	@Override
	public Object doPrepareProcess(Map<String, Object> parameters) {
		getLogger().info("This method is not implemented");
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.jbpm.exe.ProcessDefinitionW#hasManagerRole(com.idega.user.data.User)
	 */
	@Override
	public boolean hasManagerRole(User user) {
		if (user == null) {
			getLogger().warning("User is not provided");
			return false;
		}

		ProcessDefinition processDefinition = getProcessDefinition();
		String procDefName = processDefinition == null ? null : processDefinition.getName();
		Collection<VariableInstanceInfo> vars = getVariableInstanceQuerier().getVariablesByProcessDefAndVariableName(
				procDefName,
				BPMConstants.VAR_MANAGER_ROLE
		);
		if (ListUtil.isEmpty(vars)) {
			getLogger().warning("No data found");
			return Boolean.FALSE;
		}

		AccessController accessController = IWMainApplication.getDefaultIWMainApplication().getAccessController();
		if (accessController == null) {
			getLogger().warning("Access controller is not available");
			return Boolean.FALSE;
		}

		for (VariableInstanceInfo var: vars) {
			if (var == null) {
				continue;
			}
			String value = var.getValue();
			if (StringUtil.isEmpty(value)) {
				getLogger().warning("Value is unknown for variable: " + var);
				continue;
			}

			return accessController.hasRole(user, value);
		}

		getLogger().warning(user + " (ID: " + user.getId() + ") does not have manager role for proc. def.: " + procDefName);
		return false;
	}

	@Override
	public boolean isAvailable(IWContext iwc) {
		return true;
	}

	@Override
	public String getNotAvailableLink(IWContext iwc) {
		return null;
	}

	@Override
	public String getProcessDefinitionName() {
		ProcessDefinition processDefinition = getProcessDefinition();
		return processDefinition == null ? null : processDefinition.getName();
	}

}