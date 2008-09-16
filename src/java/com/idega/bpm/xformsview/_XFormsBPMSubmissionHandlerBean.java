package com.idega.bpm.xformsview;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chiba.xml.xforms.core.Submission;
import org.chiba.xml.xforms.exception.XFormsException;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;

import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.documentmanager.util.FormManagerUtil;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.util.URIUtil;

/**
 * TODO: move all this logic to spring bean
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/16 17:48:15 $ by $Author: civilis $
 */
//@Scope("singleton")
//@Service(_XFormsBPMSubmissionHandlerBean.beanIdentifier)
public class _XFormsBPMSubmissionHandlerBean {
	
	public static final String beanIdentifier = "XFormsBPMSubmissionHandlerBean";
	
	private BPMFactory bpmFactory;
	private IXFormViewFactory xfvFact;
	private BPMContext idegaJbpmContext;
	private TmpFilesManager fileUploadManager;
	private TmpFileResolver uploadedResourceResolver;
    
	public Map handle(Submission submission, Node submissionInstance) throws XFormsException {
		
    	BPMFactory bpmFactory = getBpmFactory();
    	IXFormViewFactory xfvFact = getXfvFact();
    	XFormsView casesXFormsView = xfvFact.getXFormsView();
    	casesXFormsView.setSubmission(submission, submissionInstance);
    	
    	JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
    	
    	try {
//    		TODO: move to params node
    		String action = submission.getElement().getAttribute(FormManagerUtil.action_att);
        	Map<String, String> parameters = new URIUtil(action).getParameters();
        	
        	ProcessDefinition processDefinition;
        	
        	if(parameters.containsKey(ProcessConstants.START_PROCESS)) {
        		
        		long tskInstId = Long.parseLong(parameters.get(ProcessConstants.TASK_INSTANCE_ID));
        		processDefinition = ctx.getTaskInstance(tskInstId).getProcessInstance().getProcessDefinition();
        		casesXFormsView.setTaskInstanceId(tskInstId);
        		bpmFactory.getProcessManager(processDefinition.getId()).getProcessDefinition(processDefinition.getId()).startProcess(casesXFormsView);
        		
        	} else if(parameters.containsKey(ProcessConstants.TASK_INSTANCE_ID)) {
        		
        		long tskInstId = Long.parseLong(parameters.get(ProcessConstants.TASK_INSTANCE_ID));
        		processDefinition = ctx.getTaskInstance(tskInstId).getProcessInstance().getProcessDefinition();
        		casesXFormsView.setTaskInstanceId(tskInstId);
        		bpmFactory.getProcessManager(processDefinition.getId()).getTaskInstance(tskInstId).submit(casesXFormsView);

        	} else {
            	
        		Logger.getLogger(_XFormsBPMSubmissionHandlerBean.class.getName()).log(Level.SEVERE, "Couldn't handle submission. No action associated with the submission action: "+action);
        	}
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
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
	
	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

    @Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
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
	public void setUploadedResourceResolver(@TmpFileResolverType("xformVariables")
			TmpFileResolver uploadedResourceResolver) {
		this.uploadedResourceResolver = uploadedResourceResolver;
	}
}