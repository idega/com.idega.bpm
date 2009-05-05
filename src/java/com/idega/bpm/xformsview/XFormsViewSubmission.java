package com.idega.bpm.xformsview;

import java.util.Map;

import org.chiba.xml.xforms.core.Submission;
import org.w3c.dom.Node;

import com.idega.block.form.submission.XFormSubmissionInstance;
import com.idega.jbpm.variables.Converter;
import com.idega.jbpm.view.ViewSubmissionImpl;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $ Last modified: $Date: 2009/05/05 14:07:25 $ by $Author: civilis $
 */
public class XFormsViewSubmission extends ViewSubmissionImpl {
	
	private Converter converter;
	
	public void setSubmission(Submission submission, Node submissionInstance) {
		
		Map<String, String> parameters = new XFormSubmissionInstance(
		        submissionInstance).getParameters();
		
		populateParameters(parameters);
		populateVariables(getConverter().convert(submissionInstance));
	}
	
	public Converter getConverter() {
		return converter;
	}
	
	public void setConverter(Converter converter) {
		this.converter = converter;
	}
	
	@Override
	public Long getTaskInstanceId() {
		Long taskInstanceId = super.getTaskInstanceId();
		
		if (taskInstanceId == null) {
			
			Map<String, String> params = resolveParameters();
			taskInstanceId = new XFormParameters(params).getTaskInstance();
		}
		
		return taskInstanceId;
	}
}