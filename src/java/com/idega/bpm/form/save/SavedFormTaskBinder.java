package com.idega.bpm.form.save;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.form.save.FormSavePhasePlugin;
import com.idega.block.form.save.FormSavePhasePluginParams;
import com.idega.block.form.submission.XFormSubmissionInstance;
import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.bpm.xformsview.XFormParameters;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.transaction.TransactionContext;
import com.idega.transaction.TransactionalCallback;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/05/05 14:07:41 $ by $Author: civilis $
 */
@Service(SavedFormTaskBinder.beanIdentifier)
@Scope("prototype")
public class SavedFormTaskBinder implements FormSavePhasePlugin {
	
	public static final String beanIdentifier = "SavedFormTaskBinder";
	
	private static final String savedSubmissionIdVarName = "savedSubmissionId";
	
	private FormSavePhasePluginParams params;
	private Long taskInstanceId;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@Autowired
	private TransactionContext transactionContext;
	
	public String getSubmissionScheme() {
		
		return "xformsBPM";
	}
	
	public String getBeanIdentifier() {
		
		return beanIdentifier;
	}
	
	public void afterSave(FormSavePhasePluginParams params) {
		
		this.params = params;
		
		XFormSubmissionInstance submissionInstance = params.submissionInstance;
		
		Map<String, String> formParams = submissionInstance.getParameters();
		Long taskInstanceId = new XFormParameters(formParams).getTaskInstance();
		
		if (taskInstanceId != null) {
			
			// we don't process saving the start task form, only ordinary tasks
			
			this.taskInstanceId = taskInstanceId;
			bindSavedFormWithTaskInstance();
		}
	}
	
	private void bindSavedFormWithTaskInstance() {
		
		final String submissionUUID = params.submissionUUID;
		final Long taskInstanceId = this.taskInstanceId;
		final Variable variable = new Variable(savedSubmissionIdVarName,
		        VariableDataType.SYSTEM);
		
		getTransactionContext().executeInTransaction(
		    new TransactionalCallback() {
			    
			    public <T> T execute() {
				    TaskInstanceW tiw = getBpmFactory().getTaskInstanceW(
				        taskInstanceId);
				    tiw.addVariable(variable, submissionUUID);
				    return null;
			    }
		    });
	}
	
	BPMFactory getBpmFactory() {
		return bpmFactory;
	}
	
	TransactionContext getTransactionContext() {
		return transactionContext;
	}
}