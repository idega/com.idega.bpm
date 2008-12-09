package com.idega.bpm.xformsview;

import java.util.Map;

import org.chiba.xml.xforms.core.Submission;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.idega.jbpm.variables.Converter;
import com.idega.jbpm.view.ViewSubmissionImpl;
import com.idega.util.URIUtil;
import com.idega.xformsmanager.util.FormManagerUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2008/12/09 02:49:00 $ by $Author: civilis $
 */
public class XFormsViewSubmission extends ViewSubmissionImpl {

	private Converter converter;

	public void setSubmission(Submission submission, Node submissionInstance) {

		Element paramsEl = FormManagerUtil
				.getFormParamsElement(submissionInstance);

		Map<String, String> parameters = paramsEl == null ? new URIUtil(null)
				.getParameters() : new URIUtil(paramsEl.getTextContent())
				.getParameters();

		populateParameters(parameters);
		populateVariables(getConverter().convert(submissionInstance));
	}

	public Converter getConverter() {
		return converter;
	}

	public void setConverter(Converter converter) {
		this.converter = converter;
	}
}