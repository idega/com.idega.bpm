package com.idega.bpm.process.attachments;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.idega.block.process.variables.Variable;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;

@Service("addAttachmentsToTaskInstanceHanlder")
public class AddAttachmentsToTaskInstanceHanlder implements ActionHandler {
	
	/**
     * 
     */
    private static final long serialVersionUID = 7679101385240129699L;

	private String selectedAttachementVariableName;
	
	private String addedAttachmentsVariableName;
	
	private long taskInstanceId;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@Autowired
	private VariablesHandler variablesHandler;
	
	public void execute(ExecutionContext executionContext) throws Exception {
		TaskInstanceW taskInstanceW = getBpmFactory()
		        .getProcessManagerByTaskInstanceId(getTaskInstanceId())
		        .getTaskInstance(getTaskInstanceId());
		
		Object obj = taskInstanceW.getVariable(getSelectedAttachementVariableName());
		
		Variable variable = Variable
		        .parseDefaultStringRepresentation(getAddedAttachmentsVariableName());
		
		if (obj instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> variablesList = (List<String>) obj;
			for (String str : variablesList) {
				String taskInstnaceId = str.substring(0, str.indexOf(";"));
				String variableHash = str.substring(str.indexOf(";") + 1, str
				        .length());
				BinaryVariable binaryVariable = getBinaryVariable(Long
				        .valueOf(taskInstnaceId), Integer
				        .valueOf(variableHash));
				
				BinaryVariable addedAttachment = taskInstanceW.addAttachment(variable, binaryVariable
				        .getFileName(), binaryVariable.getDescription(),
				    getVariablesHandler().getBinaryVariablesHandler()
				            .getBinaryVariableContent(binaryVariable));
				
				
				//TODO: what really should be copied and what shoud not?
				addedAttachment.setMetadata(binaryVariable.getMetadata());
				addedAttachment.setSigned(binaryVariable.getSigned());
				addedAttachment.update();
			}
		}
		
	}
	
	private BinaryVariable getBinaryVariable(long taskInstanceId,
	        Integer binaryVariableHash) {
		
		List<BinaryVariable> variables = getVariablesHandler()
		        .resolveBinaryVariables(taskInstanceId);
		
		for (BinaryVariable binaryVariable : variables) {
			
			if (binaryVariable.getHash().equals(binaryVariableHash)) {
				
				return binaryVariable;
			}
		}
		
		return null;
	}
	
	public long getTaskInstanceId() {
		return taskInstanceId;
	}
	
	public void setTaskInstanceId(long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}
	
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}
	
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}

	public String getSelectedAttachementVariableName() {
    	return selectedAttachementVariableName;
    }

	public void setSelectedAttachementVariableName(
            String selectedAttachementVariableName) {
    	this.selectedAttachementVariableName = selectedAttachementVariableName;
    }

	public String getAddedAttachmentsVariableName() {
    	return addedAttachmentsVariableName;
    }

	public void setAddedAttachmentsVariableName(String addedAttachmentsVariableName) {
    	this.addedAttachmentsVariableName = addedAttachmentsVariableName;
    }
	
}
