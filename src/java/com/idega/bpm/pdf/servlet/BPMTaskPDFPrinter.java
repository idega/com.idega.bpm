package com.idega.bpm.pdf.servlet;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.variables.Variable;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.jbpm.artifacts.presentation.AttachmentWriter;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.util.ListUtil;
import com.idega.util.expression.ELUtil;

public class BPMTaskPDFPrinter extends AttachmentWriter {

	public static final String PARAM_TASK_INSTANCE_ID = "taskID";
	public static final String PARAM_VARIABLE_NAME = "variableName";

	private static final Logger logger = Logger
			.getLogger(BPMTaskPDFPrinter.class.getName());

	@Autowired
	private BPMFactory bpmFactory;

	@Override
	public String getMimeType() {
		if (binaryVariable != null && binaryVariable.getMimeType() != null)
			return binaryVariable.getMimeType();

		return MimeTypeUtil.MIME_TYPE_PDF_1;
	}

	@Override
	protected BinaryVariable resolveBinaryVariable(IWContext iwc) {
		String taskIdStr = iwc.getParameter(PARAM_TASK_INSTANCE_ID);
		String variableName = iwc.getParameter(PARAM_VARIABLE_NAME);

		if (taskIdStr != null && variableName != null) {
			Variable variable = Variable.parseDefaultStringRepresentation(variableName);

			Long taskInstanceId = Long.valueOf(taskIdStr);

			TaskInstanceW taskInstanceW = getBpmFactory()
					.getProcessManagerByTaskInstanceId(taskInstanceId)
					.getTaskInstance(taskInstanceId);
			List<BinaryVariable> attachmentsForVar = taskInstanceW.getAttachments(variable);
//			TODO: check if this is correct implementation (expecting only one attachment for variable)
			binaryVariable = !ListUtil.isEmpty(attachmentsForVar) ? attachmentsForVar.iterator().next() : null;
		} else {
			binaryVariable = null;
			logger.log(Level.WARNING, "No task instance id and variable name provided");
		}

		if (binaryVariable == null) {
			logger.warning("Variable not found by task instance id: " + taskIdStr + " and variable name: " + variableName);
		}

		return binaryVariable;
	}

	protected BPMFactory getBpmFactory() {
		if (bpmFactory == null) {
			ELUtil.getInstance().autowire(this);
		}

		return bpmFactory;
	}
}