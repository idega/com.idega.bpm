package com.idega.bpm.exe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.business.CaseManagersProvider;
import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.util.ListUtil;

/**
 * abstract implementation of ProcessManager. Default behavior is that bean container (e.g. spring)
 * instantiates this and proxies the abstract methods, which create wanted bpm wrapper instances
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $ Last modified: $Date: 2009/03/17 20:56:09 $ by $Author: civilis $
 */
public abstract class ProcessManagerAbstractImpl implements ProcessManager {

	private String managerType;
	@Autowired
	private BPMContext bpmContext;

	@Autowired
	private CaseManagersProvider caseManagersProvider;

	@Override
	public ProcessDefinitionW getProcessDefinition(Serializable pdId) {
		if (pdId instanceof Number) {
			return createProcessDefinition(((Number) pdId).longValue());
		}
		return null;
	}

	@Override
	public ProcessDefinitionW getProcessDefinition(final String processName) {
		return getBpmContext().execute(new JbpmCallback<ProcessDefinitionW>() {

			@Override
			public ProcessDefinitionW doInJbpm(JbpmContext context) throws JbpmException {
				ProcessDefinition pd = context.getGraphSession().findLatestProcessDefinition(processName);

				if (pd == null)
					throw new IllegalStateException("Process definition not deployed by the process name = " + processName);

				return getProcessDefinition(pd.getId());
			}
		});
	}

	@Override
	public <T extends Serializable> ProcessInstanceW getProcessInstance(T piId) {
		return createProcessInstance(piId);
	}

	@Override
	public <T extends Serializable> TaskInstanceW getTaskInstance(T tiId) {
		return createTaskInstance(tiId);
	}

	@Override
	public TaskInstanceW getTaskInstance(TaskInstance ti) {

		TaskInstanceW tiw = createTaskInstance(ti.getId());
		tiw.setTaskInstance(ti);
		return tiw;
	}

	/**
	 * method injected by bean container
	 *
	 * @return
	 */
	protected abstract ProcessDefinitionW createPDW();

	public synchronized ProcessDefinitionW createProcessDefinition(long pdId) {
		ProcessDefinitionW pdw = createPDW();
		pdw.setProcessDefinitionId(pdId);
		return pdw;
	}

	/**
	 * method injected by bean container
	 *
	 * @return
	 */
	protected abstract ProcessInstanceW createPIW();

	public synchronized <T extends Serializable> ProcessInstanceW createProcessInstance(T piId) {
		ProcessInstanceW piw = createPIW();
		piw.setProcessInstanceId(piId);
		return piw;
	}

	/**
	 * method injected by bean container
	 *
	 * @return
	 */
	protected abstract TaskInstanceW createTIW();

	public synchronized <T extends Serializable> TaskInstanceW createTaskInstance(T tiId) {
		TaskInstanceW tiw = createTIW();
		tiw.setTaskInstanceId(tiId);
		tiw.setProcessManager(this);
		return tiw;
	}

	public BPMContext getBpmContext() {
		return bpmContext;
	}

	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}

	public CaseManagersProvider getCaseManagersProvider() {
		return caseManagersProvider;
	}

	public void setCaseManagersProvider(
	        CaseManagersProvider caseManagersProvider) {
		this.caseManagersProvider = caseManagersProvider;
	}

	// FIXME: wrong place for this, refactor
	@Override
	public List<ProcessDefinitionW> getAllProcesses() {
		List<CasesRetrievalManager> caseManagers = getCaseManagersProvider()
		        .getCaseManagers();
		if (ListUtil.isEmpty(caseManagers)) {
			return null;
		}

		List<ProcessDefinitionW> allProcesses = new ArrayList<ProcessDefinitionW>();
		for (CasesRetrievalManager caseManager : caseManagers) {
			List<Long> caseProcesses = caseManager
			        .getAllCaseProcessDefinitions();

			if (!ListUtil.isEmpty(caseProcesses)) {
				for (Long id : caseProcesses) {
					allProcesses.add(getProcessDefinition(id));
				}
			}
		}

		return allProcesses;
	}

	@Override
	public String getManagerType() {
		return managerType != null ? managerType : "default";
	}

	public void setManagerType(String managerType) {
		this.managerType = managerType;
	}
}