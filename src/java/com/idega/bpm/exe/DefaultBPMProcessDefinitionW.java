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
import org.jbpm.context.def.VariableAccess;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.variables.Variable;
import com.idega.bpm.xformsview.XFormsView;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.11 $
 * 
 *          Last modified: $Date: 2008/12/16 20:00:41 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("defaultPDW")
public class DefaultBPMProcessDefinitionW implements ProcessDefinitionW {

	private Long processDefinitionId;
	private ProcessDefinition processDefinition;

	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMContext bpmContext;
	@Autowired
	private VariablesHandler variablesHandler;

	private static final Logger logger = Logger
			.getLogger(DefaultBPMProcessDefinitionW.class.getName());

	public List<Variable> getTaskVariableList(String taskName) {

		ProcessDefinition pdef = getProcessDefinition();
		Task task = pdef.getTaskMgmtDefinition().getTask(taskName);
		TaskController tiController = task.getTaskController();

		if (tiController == null)
			return null;

		@SuppressWarnings("unchecked")
		List<VariableAccess> variableAccesses = tiController
				.getVariableAccesses();
		ArrayList<Variable> variables = new ArrayList<Variable>(
				variableAccesses.size());

		for (VariableAccess variableAccess : variableAccesses) {

			Variable variable = Variable
					.parseDefaultStringRepresentation(variableAccess
							.getVariableName());
			variables.add(variable);
		}

		return variables;
	}

	public void startProcess(ViewSubmission viewSubmission) {

		Long processDefinitionId = viewSubmission.getProcessDefinitionId();

		logger.finer("Starting process for process definition id = "
				+ processDefinitionId);

		Map<String, String> parameters = viewSubmission.resolveParameters();

		logger.finer("Params " + parameters);

		JbpmContext ctx = getBpmContext().createJbpmContext();

		try {

			ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(
					processDefinitionId);
			ProcessInstance pi = new ProcessInstance(pd);
			TaskInstance ti = pi.getTaskMgmtInstance()
					.createStartTaskInstance();

			View view = getBpmFactory().getView(viewSubmission.getViewId(),
					viewSubmission.getViewType(), false);

			// binding view to task instance
			view.getViewToTask().bind(view, ti);

			logger.log(Level.INFO,
					"New process instance created for the process "
							+ pd.getName());

			pi.setStart(new Date());

			submitVariablesAndProceedProcess(ti, viewSubmission
					.resolveVariables(), true);

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			getBpmContext().closeAndCommit(ctx);
		}
	}

	public View loadInitView(Integer initiatorId) {

		JbpmContext ctx = getBpmContext().createJbpmContext();

		try {
			Long processDefinitionId = getProcessDefinitionId();
			ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(
					processDefinitionId);

			Long startTaskId = pd.getTaskMgmtDefinition().getStartTask()
					.getId();

			List<String> preferred = new ArrayList<String>(1);
			preferred.add(XFormsView.VIEW_TYPE);
			View view = getBpmFactory().getViewByTask(startTaskId, true,
					preferred);
			view.takeView();

			Map<String, String> parameters = new HashMap<String, String>(2);

			parameters.put(ProcessConstants.START_PROCESS,
					ProcessConstants.START_PROCESS);
			parameters.put(ProcessConstants.PROCESS_DEFINITION_ID, String
					.valueOf(processDefinitionId));
			parameters.put(ProcessConstants.VIEW_ID, view.getViewId());
			parameters.put(ProcessConstants.VIEW_TYPE, view.getViewType());

			view.populateParameters(parameters);

			return view;

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			getBpmContext().closeAndCommit(ctx);
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
	 *            - some context depending implementation, e.g., roles can start
	 *            process using applications - then context will be application
	 *            id
	 */
	public void setRolesCanStartProcess(List<String> roles, Object context) {
	}

	protected void submitVariablesAndProceedProcess(TaskInstance ti,
			Map<String, Object> variables, boolean proceed) {

		getVariablesHandler().submitVariables(variables, ti.getId(), true);

		if (proceed) {

			String actionTaken = (String) ti
					.getVariable(ProcessConstants.actionTakenVariableName);

			if (actionTaken != null && !CoreConstants.EMPTY.equals(actionTaken)
					&& false)
				ti.end(actionTaken);
			else
				ti.end();
		} else {
			ti.setEnd(new Date());
		}

		Integer usrId = getBpmFactory().getBpmUserFactory().getCurrentBPMUser()
				.getIdToUse();

		if (usrId != null)
			ti.setActorId(usrId.toString());
	}

	public String getStartTaskName() {

		List<String> preferred = new ArrayList<String>(1);
		preferred.add(XFormsView.VIEW_TYPE);

		Long taskId = getProcessDefinition().getTaskMgmtDefinition()
				.getStartTask().getId();

		View view = getBpmFactory().getViewByTask(taskId, false, preferred);

		return view.getDisplayName(new Locale("is", "IS"));
	}

	public Collection<String> getTaskNodeTransitionsNames(String taskName) {

		ProcessDefinition pdef = getProcessDefinition();
		Task task = pdef.getTaskMgmtDefinition().getTask(taskName);

		TaskNode taskNode = task.getTaskNode();

		if (taskNode != null) {

			@SuppressWarnings("unchecked")
			Map<String, Transition> leavingTransitions = taskNode
					.getLeavingTransitionsMap();
			return leavingTransitions != null ? leavingTransitions.keySet()
					: null;
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

	public ProcessDefinition getProcessDefinition() {

		if (processDefinition == null && getProcessDefinitionId() != null) {

			JbpmContext ctx = getBpmContext().createJbpmContext();

			try {
				processDefinition = ctx.getGraphSession().getProcessDefinition(
						getProcessDefinitionId());

			} finally {
				getBpmContext().closeAndCommit(ctx);
			}
		}

		return processDefinition;
	}

	public String getProcessName(Locale locale) {

		return getProcessDefinition().getName();
	}
}