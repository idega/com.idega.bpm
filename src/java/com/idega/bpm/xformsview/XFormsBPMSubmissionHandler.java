package com.idega.bpm.xformsview;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chiba.xml.xforms.connector.AbstractConnector;
import org.chiba.xml.xforms.connector.SubmissionHandler;
import org.chiba.xml.xforms.core.Submission;
import org.chiba.xml.xforms.exception.XFormsException;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.documentmanager.util.FormManagerUtil;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.util.URIUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/09/18 17:11:31 $ by $Author: civilis $
 */
public class XFormsBPMSubmissionHandler extends AbstractConnector implements SubmissionHandler {
	
	private BPMFactory bpmFactory;
	private IXFormViewFactory xfvFact;
	private BPMContext bpmContext;
	private TmpFilesManager fileUploadManager;
	private TmpFileResolver uploadedResourceResolver;
    
    public Map<String, Object> submit(Submission submission, Node submissionInstance) throws XFormsException {
		
    	//method - post, replace - none
    	if (!submission.getReplace().equalsIgnoreCase("none"))
            throw new XFormsException("Submission mode '" + submission.getReplace() + "' not supported");
    	
    	if(!submission.getMethod().equalsIgnoreCase("put") && !submission.getMethod().equalsIgnoreCase("post"))
    		throw new XFormsException("Submission method '" + submission.getMethod() + "' not supported");
    	
    	if(submission.getMethod().equalsIgnoreCase("put")) {
    		//update (put)
    		//currently unsupported
    		throw new XFormsException("Submission method '" + submission.getMethod() + "' not yet supported");
    		
    	} else {
    		//insert (post)
    	}
    	
    	ELUtil.getInstance().autowire(this);
    	
    	BPMFactory bpmFactory = getBpmFactory();
    	IXFormViewFactory xfvFact = getXfvFact();
    	XFormsView xFormsView = xfvFact.getXFormsView();
    	xFormsView.setSubmission(submission, submissionInstance);
    	
    	JbpmContext ctx = getBpmContext().createJbpmContext();
    	
    	try {
//    	TODO: unify. see parameters manager and xformview setsubmission
    		Element paramsEl = FormManagerUtil.getFormParamsElement(submissionInstance);
    		
    		//String action = submission.getElement().getAttribute(FormManagerUtil.action_att);
    		String params = paramsEl != null ? paramsEl.getTextContent() : null;
        	Map<String, String> parameters = new URIUtil(params).getParameters();
        	
        	ProcessDefinition processDefinition;
        	
        	if(parameters.containsKey(ProcessConstants.START_PROCESS)) {
        		
        		long tskInstId = Long.parseLong(parameters.get(ProcessConstants.TASK_INSTANCE_ID));
        		processDefinition = ctx.getTaskInstance(tskInstId).getProcessInstance().getProcessDefinition();
        		xFormsView.setTaskInstanceId(tskInstId);
        		bpmFactory.getProcessManager(processDefinition.getId()).getProcessDefinition(processDefinition.getId()).startProcess(xFormsView);
        		
        	} else if(parameters.containsKey(ProcessConstants.TASK_INSTANCE_ID)) {
        		
        		long tskInstId = Long.parseLong(parameters.get(ProcessConstants.TASK_INSTANCE_ID));
        		processDefinition = ctx.getTaskInstance(tskInstId).getProcessInstance().getProcessDefinition();
        		xFormsView.setTaskInstanceId(tskInstId);
        		bpmFactory.getProcessManager(processDefinition.getId()).getTaskInstance(tskInstId).submit(xFormsView);

        	} else {
            	
        		Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Couldn't handle submission. No action associated with the submission action: "+params);
        	}
			
		} finally {
			getBpmContext().closeAndCommit(ctx);
		}
		
		getFileUploadManager().cleanup(null, submissionInstance, getUploadedResourceResolver());
		
		return null;
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
	public void setUploadedResourceResolver(@TmpFileResolverType("xformVariables") TmpFileResolver uploadedResourceResolver) {
		this.uploadedResourceResolver = uploadedResourceResolver;
	}
}