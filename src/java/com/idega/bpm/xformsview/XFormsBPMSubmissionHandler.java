package com.idega.bpm.xformsview;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chiba.xml.xforms.connector.AbstractConnector;
import org.chiba.xml.xforms.connector.SubmissionHandler;
import org.chiba.xml.xforms.core.Submission;
import org.chiba.xml.xforms.exception.XFormsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Node;

import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.event.ProcessInstanceCreatedEvent;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.util.CoreUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $ Last modified: $Date: 2009/05/05 14:07:04 $ by $Author: civilis $
 */
public class XFormsBPMSubmissionHandler extends AbstractConnector implements SubmissionHandler {

	private BPMFactory bpmFactory;
	private IXFormViewFactory xfvFact;
	private BPMContext bpmContext;
	private TmpFilesManager fileUploadManager;
	private TmpFileResolver uploadedResourceResolver;

	@Override
	@Transactional(readOnly = false)
	public Map<String, Object> submit(Submission submission, Node submissionInstance) throws XFormsException {
		// method - post, replace - none
		if (!submission.getReplace().equalsIgnoreCase("none")) {
			throw new XFormsException("Submission mode '" + submission.getReplace() + "' not supported");
		}

		if (!submission.getMethod().equalsIgnoreCase("put") && !submission.getMethod().equalsIgnoreCase("post")) {
			throw new XFormsException("Submission method '" + submission.getMethod() + "' not supported");
		}

		if (submission.getMethod().equalsIgnoreCase("put")) {
			// update (put) currently unsupported
			throw new XFormsException("Submission method '" + submission.getMethod() + "' not yet supported");
		} else {
			// insert (post)
		}

		ELUtil.getInstance().autowire(this);

		IXFormViewFactory xfvFact = getXfvFact();
		XFormsViewSubmission xformsViewSubmission = xfvFact.getViewSubmission();
		xformsViewSubmission.setSubmission(submission, submissionInstance);

		Long taskInstanceId = xformsViewSubmission.getTaskInstanceId();

		Long piId = null;
		String procDefName = null;
		boolean error = false;
		Map<String, Object> variables = null;
		TaskInstanceW tiW = null;
		ProcessInstanceW piW = null;
		try {
			if (taskInstanceId != null) {
				BPMFactory bpmFactory = getBpmFactory();
				xformsViewSubmission.setTaskInstanceId(taskInstanceId);
				tiW = bpmFactory.getProcessManagerByTaskInstanceId(taskInstanceId).getTaskInstance(taskInstanceId);

				piW = tiW.getProcessInstanceW();
				piId = piW.getProcessInstanceId();
				ProcessDefinitionW pdW = piW.getProcessDefinitionW();
				procDefName = pdW == null ? null : pdW.getProcessDefinitionName();

				tiW.submit(xformsViewSubmission);
				variables = xformsViewSubmission.resolveVariables();
			} else {
				BPMFactory bpmFactory = getBpmFactory();
				Map<String, String> parameters = xformsViewSubmission.resolveParameters();

				if (parameters.containsKey(ProcessConstants.START_PROCESS)) {
					long processDefinitionId = Long.parseLong(parameters.get(ProcessConstants.PROCESS_DEFINITION_ID));
					String viewId = parameters.get(ProcessConstants.VIEW_ID);
					String viewType = parameters.get(ProcessConstants.VIEW_TYPE);

					xformsViewSubmission.setProcessDefinitionId(processDefinitionId);
					xformsViewSubmission.setViewId(viewId);
					xformsViewSubmission.setViewType(viewType);

					ProcessDefinitionW pdW = bpmFactory.getProcessManager(processDefinitionId).getProcessDefinition(processDefinitionId);
					procDefName = getBpmFactory().getBPMDAO().getProcessDefinitionNameByProcessDefinitionId(processDefinitionId);

					piId = pdW.startProcess(xformsViewSubmission);
					variables = xformsViewSubmission.resolveVariables();
				} else {
					Logger.getLogger(getClass().getName()).severe("Couldn't handle submission. No action associated with the submission " +
							"action. Parameters=" + parameters);
				}
			}

			getFileUploadManager().cleanup(null, submissionInstance, getUploadedResourceResolver());
			return variables;
		} catch (Exception e) {
			error = true;
			String message = "Error submitting view for task instance: " + tiW + ", proc. inst. " + piW + ", proc. def. name: " + procDefName;
			Logger.getLogger(getClass().getName()).log(Level.WARNING, message, e);
			CoreUtil.sendExceptionNotification(message, e);
		} finally {
			if (!error && procDefName != null && piId != null) {
				ELUtil.getInstance().publishEvent(new ProcessInstanceCreatedEvent(procDefName, piId, variables));
			}
		}

		return variables;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public IXFormViewFactory getXfvFact() {
		return xfvFact;
	}

	@Autowired
	public void setXfvFact(IXFormViewFactory xfvFact) {
		this.xfvFact = xfvFact;
	}

	public BPMContext getBpmContext() {
		return bpmContext;
	}

	@Autowired
	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}

	public TmpFilesManager getFileUploadManager() {
		return fileUploadManager;
	}

	@Autowired
	public void setFileUploadManager(TmpFilesManager fileUploadManager) {
		this.fileUploadManager = fileUploadManager;
	}

	public TmpFileResolver getUploadedResourceResolver() {
		return uploadedResourceResolver;
	}

	@Autowired
	public void setUploadedResourceResolver(
	        @TmpFileResolverType("xformVariables") TmpFileResolver uploadedResourceResolver) {
		this.uploadedResourceResolver = uploadedResourceResolver;
	}
}