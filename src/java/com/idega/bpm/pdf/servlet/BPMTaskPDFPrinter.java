package com.idega.bpm.pdf.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.variables.Variable;
import com.idega.io.MediaWritable;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.util.FileUtil;
import com.idega.util.expression.ELUtil;

public class BPMTaskPDFPrinter implements MediaWritable {

	public static final String PARAM_TASK_INSTANCE_ID = "taskID";
	public static final String PARAM_VARIABLE_NAME = "variableName";

	public String getMimeType() {
		return "application/pdf";
	}

	private static final Logger logger = Logger
			.getLogger(BPMTaskPDFPrinter.class.getName());

	private InputStream inputStream;

	@Autowired
	private VariablesHandler variablesHandler;

	@Autowired
	private BPMFactory bpmFactory;

	public void init(HttpServletRequest req, IWContext iwc) {

		String taskIdStr = iwc.getParameter(PARAM_TASK_INSTANCE_ID);
		String variableName = iwc.getParameter(PARAM_VARIABLE_NAME);

		if (taskIdStr != null && variableName != null) {

			Variable variable = Variable
					.parseDefaultStringRepresentation(variableName);

			Long taskInstanceId = Long.valueOf(taskIdStr);
			BinaryVariable binaryVariable = getBinVar(taskInstanceId, variable);

			VariablesHandler variablesHandler = getVariablesHandler();
			InputStream inputStream = variablesHandler
					.getBinaryVariablesHandler().getBinaryVariableContent(
							binaryVariable);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte buffer[] = new byte[1024];
			int noRead = 0;
			try {
				noRead = inputStream.read(buffer, 0, 1024);
				while (noRead != -1) {
					baos.write(buffer, 0, noRead);
					noRead = inputStream.read(buffer, 0, 1024);
				}
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Unable to read from input stream", e);
				inputStream = null;
				return;
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(baos
					.toByteArray());
			this.inputStream = bais;
			setOutpusAsPDF(iwc, bais.available());
		}
	}

	public void writeTo(OutputStream streamOut) throws IOException {
		if (inputStream == null) {
			logger.log(Level.SEVERE, "Unable to get input stream");
			return;
		}

		FileUtil.streamToOutputStream(inputStream, streamOut);

		streamOut.flush();
		streamOut.close();
	}

	public void setOutpusAsPDF(IWContext iwc, int fileLength) {
		iwc.getResponse().setContentType("application/pdf");
		if (fileLength > 0) {
			iwc.getResponse().setContentLength(fileLength);
		}
	}

	private BinaryVariable getBinVar(long taskInstanceId, Variable variable) {

		TaskInstanceW taskInstanceW = getBpmFactory()
				.getProcessManagerByTaskInstanceId(taskInstanceId)
				.getTaskInstance(taskInstanceId);
		return taskInstanceW.getAttachment(variable);

	}

	private VariablesHandler getVariablesHandler() {
		if (variablesHandler == null) {
			ELUtil.getInstance().autowire(this);
		}
		return variablesHandler;
	}

	private BPMFactory getBpmFactory() {
		if (bpmFactory == null) {
			ELUtil.getInstance().autowire(this);
		}

		return bpmFactory;
	}

}
