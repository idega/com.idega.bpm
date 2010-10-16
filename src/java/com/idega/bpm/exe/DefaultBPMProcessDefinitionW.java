package com.idega.bpm.exe;

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
import org.jbpm.context.def.VariableAccess;
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
import com.idega.bpm.xformsview.XFormsView;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.events.VariableCreatedEvent;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.17 $ Last modified: $Date: 2009/02/16 22:02:37 $ by $Author: donatas $
 */
@Service("defaultPDW")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DefaultBPMProcessDefinitionW implements ProcessDefinitionW {
	
	private static Logger LOGGER;
	
	private Long processDefinitionId;
	private ProcessDefinition processDefinition;
	
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMContext bpmContext;
	@Autowired
	private VariablesHandler variablesHandler;
	
	protected Logger getLogger() {
		if (LOGGER == null) {
			LOGGER = Logger.getLogger(this.getClass().getName());
		}
		return LOGGER;
	}
	
	@Transactional(readOnly = true)
	public List<Variable> getTaskVariableList(final String taskName) {
		return getBpmContext().execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				ProcessDefinition pdef = getProcessDefinition();
				Task task = pdef.getTaskMgmtDefinition().getTask(taskName);
				TaskController tiController = task.getTaskController();
				
				if (tiController == null)
					return null;
				
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
	
	@Transactional(readOnly = true)
	public List<Variable> getTaskVariableWithAccessesList(final String taskName) {
		return getBpmContext().execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				ProcessDefinition pdef = getProcessDefinition();
				Task task = pdef.getTaskMgmtDefinition().getTask(taskName);
				TaskController tiController = task.getTaskController();
				
				if (tiController == null)
					return null;
				
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
	
	@Transactional(readOnly = false)
	public void startProcess(final ViewSubmission viewSubmission) {
		Long processDefinitionId = viewSubmission.getProcessDefinitionId();
		
		if (!processDefinitionId.equals(getProcessDefinitionId())) {
			throw new IllegalArgumentException("View submission was for different process definition id than tried to submit to");
		}
		
		getLogger().finer("Starting process for process definition id = " + processDefinitionId);
		
		Map<String, String> parameters = viewSubmission.resolveParameters();
		
		getLogger().finer("Params " + parameters);
		
		try {
			getBpmContext().execute(new JbpmCallback() {
				public Object doInJbpm(JbpmContext context) throws JbpmException {
					ProcessDefinition pd = getProcessDefinition();
					ProcessInstance pi = new ProcessInstance(pd);
					TaskInstance ti = pi.getTaskMgmtInstance().createStartTaskInstance();
					
					View view = getBpmFactory().getView( viewSubmission.getViewId(), viewSubmission.getViewType(), false);
					
					// binding view to task instance
					view.getViewToTask().bind(view, ti);
					
					getLogger().info("New process instance created for the process " + pd.getName());
					
					pi.setStart(new Date());
					
					submitVariablesAndProceedProcess(ti, viewSubmission.resolveVariables(), true);
					
					return null;
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Transactional(readOnly = false)
	public View loadInitView(Integer initiatorId) {
		try {
			return getBpmContext().execute(new JbpmCallback() {
				public Object doInJbpm(JbpmContext context) throws JbpmException {
					Long processDefinitionId = getProcessDefinitionId();
					ProcessDefinition pd = getProcessDefinition();
					
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
	public void setRolesCanStartProcess(List<String> roles, Object context) {
	}
	
	@Transactional(readOnly = false)
	protected void submitVariablesAndProceedProcess(TaskInstance ti, Map<String, Object> variables, boolean proceed) {
		Integer usrId = getBpmFactory().getBpmUserFactory().getCurrentBPMUser().getIdToUse();
		
		if (usrId != null)
			ti.setActorId(usrId.toString());
		
		getVariablesHandler().submitVariables(variables, ti.getId(), true);
		
		if (proceed) {
			String actionTaken = (String) ti.getVariable(ProcessConstants.actionTakenVariableName);
			if (!StringUtil.isEmpty(actionTaken)) {
				getLogger().fine("Taken action: " + actionTaken);
			}
			
//			if (actionTaken != null && !CoreConstants.EMPTY.equals(actionTaken) && false)
//				ti.end(actionTaken);
//			else
				ti.end();
		} else {
			ti.setEnd(new Date());
		}
		
		try {
			ApplicationContext appContext = ELUtil.getInstance().getApplicationContext();
			ProcessInstance pi = ti.getProcessInstance();
			appContext.publishEvent(new VariableCreatedEvent(this, pi.getProcessDefinition().getName(), pi.getId(), variables));
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error publishing VariableCreatedEvent for task instance: " + ti, e);
		}
	}
	
	@Transactional(readOnly = true)
	public String getStartTaskName() {
		List<String> preferred = new ArrayList<String>(1);
		preferred.add(XFormsView.VIEW_TYPE);
		
		Long taskId = getProcessDefinition().getTaskMgmtDefinition().getStartTask().getId();
		
		View view = getBpmFactory().getViewByTask(taskId, false, preferred);
		
		return view.getDisplayName(new Locale("is", "IS"));
	}
	
	@Transactional(readOnly = true)
	public Collection<String> getTaskNodeTransitionsNames(String taskName) {
		ProcessDefinition pdef = getProcessDefinition();
		Task task = pdef.getTaskMgmtDefinition().getTask(taskName);
		
		TaskNode taskNode = task.getTaskNode();
		if (taskNode != null) {
			@SuppressWarnings("unchecked")
			Map<String, Transition> leavingTransitions = taskNode.getLeavingTransitionsMap();
			return leavingTransitions != null ? leavingTransitions.keySet() : null;
		} else
			// task node is null, when task in start node
			return null;
	}
	
	public Long getProcessDefinitionId() {
		return processDefinitionId;
	}
	
	public void setProcessDefinitionId(Long processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
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
	
	@Transactional(readOnly = false)
	public ProcessDefinition getProcessDefinition() {
		processDefinition = getBpmContext().execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				return context.getGraphSession().getProcessDefinition(getProcessDefinitionId());
			}
		});
		return processDefinition;
	}
	
	public String getProcessName(Locale locale) {
		return getProcessDefinition().getName();
	}
}