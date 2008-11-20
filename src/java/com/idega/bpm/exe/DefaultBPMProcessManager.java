package com.idega.bpm.exe;

import java.util.ArrayList;
import java.util.List;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.business.CaseManager;
import com.idega.block.process.business.CaseManagersProvider;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.util.ListUtil;

/**
 * abstract implementation of ProcessManager. Default behavior is that bean
 * container (e.g. spring) instantiates this and proxies the abstract methods,
 * which create wanted bpm wrapper instances
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 *          Last modified: $Date: 2008/11/20 07:30:42 $ by $Author: valdas $
 */
public abstract class DefaultBPMProcessManager implements ProcessManager {

	@Autowired
	private BPMContext bpmContext;
	
	@Autowired
	private CaseManagersProvider caseManagersProvider;

	public ProcessDefinitionW getProcessDefinition(long pdId) {

		return createProcessDefinition(pdId);
	}

	public ProcessDefinitionW getProcessDefinition(String processName) {

		JbpmContext jctx = getBpmContext().createJbpmContext();

		try {
			ProcessDefinition pd = jctx.getGraphSession()
					.findLatestProcessDefinition(processName);
			return getProcessDefinition(pd.getId());

		} finally {
			getBpmContext().closeAndCommit(jctx);
		}
	}

	public ProcessInstanceW getProcessInstance(long piId) {

		return createProcessInstance(piId);
	}

	public TaskInstanceW getTaskInstance(long tiId) {

		return createTaskInstance(tiId);
	}

	/**
	 * method injected by bean container
	 * 
	 * @return
	 */
	protected abstract ProcessDefinitionW createPDW();

	// synchronized because spring doesn't do it when autowiring beans
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

	// synchronized because spring doesn't do it when autowiring beans
	public synchronized ProcessInstanceW createProcessInstance(long piId) {

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

	// synchronized because spring doesn't do it when autowiring beans
	public synchronized TaskInstanceW createTaskInstance(long tiId) {

		TaskInstanceW tiw = createTIW();
		tiw.setTaskInstanceId(tiId);

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

	public void setCaseManagersProvider(CaseManagersProvider caseManagersProvider) {
		this.caseManagersProvider = caseManagersProvider;
	}

	public List<ProcessDefinitionW> getAllProcesses() {
		List<CaseManager> caseManagers = getCaseManagersProvider().getCaseManagers();
		if (ListUtil.isEmpty(caseManagers)) {
			return null;
		}
		
		List<ProcessDefinitionW> allProcesses = new ArrayList<ProcessDefinitionW>();
		for (CaseManager caseManager: caseManagers) {
			List<Long> caseProcesses = caseManager.getAllCaseProcessDefinitions();
			
			if (!ListUtil.isEmpty(caseProcesses)) {
				for (Long id: caseProcesses) {
					allProcesses.add(getProcessDefinition(id));
				}
			}
		}
		
		return allProcesses;
	}
}