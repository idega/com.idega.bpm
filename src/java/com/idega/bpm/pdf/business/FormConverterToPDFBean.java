package com.idega.bpm.pdf.business;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;

import org.apache.webdav.lib.WebdavResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.form.business.FormConverterToPDF;
import com.idega.block.form.presentation.FormViewer;
import com.idega.block.pdf.PDFRenderedComponent;
import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.bpm.BPMConstants;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.graphics.generator.business.PDFGenerator;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.artifacts.presentation.ProcessArtifacts;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.PresentationUtil;
import com.idega.util.StringUtil;

@Scope("singleton")
@Service(FormConverterToPDF.STRING_BEAN_IDENTIFIER)
public class FormConverterToPDFBean implements FormConverterToPDF {

	private static final Logger LOGGER = Logger.getLogger(FormConverterToPDFBean.class.getName());
	
	@Autowired
	private PDFGenerator generator;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@Autowired
	private ProcessArtifacts processArtifacts;
	
	public String getGeneratedPDFFromXForm(String taskInstanceId, String formId, String formSubmissionId, String uploadPath, String pdfName,
			boolean checkExistence) {
		if (StringUtil.isEmpty(taskInstanceId) && StringUtil.isEmpty(formId) && StringUtil.isEmpty(formSubmissionId)) {
			LOGGER.log(Level.SEVERE, "Do not know what to generate!");
			return null;
		}
		if (StringUtil.isEmpty(uploadPath)) {
			uploadPath = BPMConstants.PDF_OF_XFORMS_PATH_IN_SLIDE;
		}
		if (!uploadPath.startsWith(CoreConstants.SLASH)) {
			uploadPath = CoreConstants.SLASH + uploadPath;
		}
		if (!uploadPath.endsWith(CoreConstants.SLASH)) {
			uploadPath += CoreConstants.SLASH;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			LOGGER.log(Level.SEVERE, "IWContext is unavailable!");
			return null;
		}
		
		addStyleSheetsForPDF(iwc);
		
		String xformInPDF = getXFormInPDF(iwc, taskInstanceId, formId, formSubmissionId, uploadPath, pdfName, checkExistence);
		if (StringUtil.isEmpty(xformInPDF)) {
			LOGGER.log(Level.SEVERE, new StringBuilder("Unable to get 'XForm' with ").append(StringUtil.isEmpty(formId) ? "task instance id: " + taskInstanceId :
														"form id: " + formId).toString());
			return null;
		}
		return xformInPDF;
	}
	
	@SuppressWarnings("unchecked")
	private void addStyleSheetsForPDF(IWContext iwc) {
		IWBundle bundle = iwc.getIWMainApplication().getBundle(BPMConstants.IW_BUNDLE_STARTER);
		String pdfCss = bundle.getVirtualPathWithFileNameString("style/pdf.css");
		List<String> resources = null;
		Object o = iwc.getSessionAttribute(PresentationUtil.ATTRIBUTE_CSS_SOURCE_LINE_FOR_HEADER);
		if (o instanceof List) {
			resources = (List) o;
		}
		else {
			resources = new ArrayList<String>();
		}
		if (!resources.contains(pdfCss)) {
			resources.add(pdfCss);
			iwc.setSessionAttribute(PresentationUtil.ATTRIBUTE_CSS_SOURCE_LINE_FOR_HEADER, resources);
		}
		iwc.setSessionAttribute(PresentationUtil.ATTRIBUTE_ADD_CSS_DIRECTLY, Boolean.TRUE);
	}
	
	public String getHashValueForGeneratedPDFFromXForm(String taskInstanceId, boolean checkExistence) {
		if (StringUtil.isEmpty(taskInstanceId)) {
			LOGGER.log(Level.INFO, "Only tasks instances can have binary variables!");
			return null;
		}
		Long taskId = null;
		try {
			taskId = Long.valueOf(taskInstanceId);
		} catch(NumberFormatException e) {
			LOGGER.log(Level.SEVERE, "Invalid task instance id: " + taskInstanceId, e);
		}
		if (taskId == null) {
			return null;
		}
		
		TaskInstanceW taskInstance = null;
		try {
			taskInstance = getBpmFactory().getProcessManagerByTaskInstanceId(taskId).getTaskInstance(taskId);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting task instance by id: " + taskInstanceId, e);
		}
		if (taskInstance == null) {
			return null;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		addStyleSheetsForPDF(iwc);
		
		UIComponent viewer = getComponentToRender(iwc, taskInstanceId, null, null);
		if (viewer == null) {
			LOGGER.log(Level.SEVERE, "Unable to get viewer for taskInstance: " + taskInstanceId);
			return null;
		}
		boolean isFormViewer = viewer instanceof FormViewer;
		if (isFormViewer) {
			((FormViewer) viewer).setPdfViewer(true);
		}
		InputStream streamToPDF = getGenerator().getStreamToGeneratedPDF(iwc, viewer, true, isFormViewer);
		if (streamToPDF == null) {
			return null;
		}
		
		String fileName = getProcessArtifacts().getFileNameForGeneratedPDFFromTaskInstance(taskInstanceId);
		String description = iwc.getIWMainApplication().getBundle(BPMConstants.IW_BUNDLE_STARTER).getResourceBundle(iwc)
		.getLocalizedString("auto_generated_pdf", "Generated PDF from document");
		
		BinaryVariable newAttachment = null;
		Variable variable = new Variable("generated_pdf_from_document_task", VariableDataType.FILE);
		try {
			newAttachment = taskInstance.addAttachment(variable, fileName, description, streamToPDF);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Unable to set binary variable for task instance: " + taskInstanceId, e);
			return null;
		} finally {
			closeInputStream(streamToPDF);
		}
		
		return newAttachment == null ? null : String.valueOf(newAttachment.getHash());
	}
	
	private void closeInputStream(InputStream stream) {
		if (stream == null) {
			return;
		}
		
		try {
			stream.close();
		} catch (IOException e) {}
	}
	
	private IWSlideService getSlideService() {
		try {
			return (IWSlideService) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), IWSlideService.class);
		} catch (IBOLookupException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	private String getXFormInPDF(IWContext iwc, String taskInstanceId, String formId, String formSubmitionId, String pathInSlide, String pdfName,
			boolean checkExistence) {
		IWSlideService slide = getSlideService();
		if (slide == null) {
			return null;
		}
		
		String prefix = formId == null ? taskInstanceId : formId;
		prefix = prefix == null ? String.valueOf(System.currentTimeMillis()) : prefix;
		pdfName = StringUtil.isEmpty(pdfName) ? new StringBuilder("Form_").append(prefix).toString() : pdfName;
		if (!pdfName.endsWith(".pdf")) {
			pdfName = new StringBuilder(pdfName).append(".pdf").toString();
		}
		String pathToForm = new StringBuilder(pathInSlide).append(pdfName).toString();
		
		boolean needToGenerate = true;
		if (checkExistence) {
			WebdavResource xformInPDF = getXFormInPDFResource(slide, pathToForm);
			needToGenerate = xformInPDF == null || !xformInPDF.exists();
		}
		if (needToGenerate) {
			return generatePDF(iwc, slide, taskInstanceId, formId, formSubmitionId, pathInSlide, pdfName, pathToForm);
		}
		
		return pathToForm;
	}
	
	private WebdavResource getXFormInPDFResource(IWSlideService slide, String pathToForm) {
		if (slide == null || StringUtil.isEmpty(pathToForm)) {
			return null;
		}
		
		WebdavResource xformInPDF = null;
		try {
			xformInPDF = slide.getWebdavResourceAuthenticatedAsRoot(pathToForm);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting resource from: " + pathToForm);
		}
		
		return xformInPDF;
	}
	
	private UIComponent getComponentToRender(IWContext iwc, String taskInstanceId, String formId, String formSubmitionId) {
		UIComponent component = null;
		if (!StringUtil.isEmpty(taskInstanceId)) {
			try {
				component = getProcessArtifacts().getViewInUIComponent(Long.valueOf(taskInstanceId), true);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error getting UIComponent for task instance: " + taskInstanceId, e);
			}
			if (component == null) {
				return null;
			}
			
			if (component instanceof PDFRenderedComponent) {
				((PDFRenderedComponent) component).setPdfViewer(true);
			}
		}
		else if (!StringUtil.isEmpty(formId) || !StringUtil.isEmpty(formSubmitionId)) {
			Application application = iwc.getApplication();
			FormViewer viewer = (FormViewer) application.createComponent(FormViewer.COMPONENT_TYPE);
		
			if (!StringUtil.isEmpty(formId)) {
				viewer.setFormId(formId);
			}
			else {
				viewer.setSubmissionId(formSubmitionId);
			}
			viewer.setPdfViewer(true);
			
			component = viewer;
		}
		
		return component;
	}
	
	private String generatePDF(IWContext iwc, IWSlideService slide, String taskInstanceId, String formId, String formSubmitionId, String pathInSlide,
			String pdfName, String pathToForm) {
		UIComponent viewer = getComponentToRender(iwc, taskInstanceId, formId, formSubmitionId);
		if (viewer == null) {
			LOGGER.log(Level.SEVERE, "Unable to get viewer for " + taskInstanceId == null ? "xform: " + formId : "taskInstance: " + taskInstanceId);
			return null;
		}
		boolean isFormViewer = viewer instanceof FormViewer;
		if (isFormViewer) {
			((FormViewer) viewer).setPdfViewer(true);
		}
		
		if (getGenerator().generatePDF(iwc, viewer, pdfName, pathInSlide, true, isFormViewer)) {
			return pathToForm;
		}
		
		LOGGER.log(Level.SEVERE, "Unable to generate PDF for " + taskInstanceId == null ? "xform: " + formId: "taskInstance: " + taskInstanceId);
		return null;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public PDFGenerator getGenerator() {
		return generator;
	}

	public void setGenerator(PDFGenerator generator) {
		this.generator = generator;
	}

	public ProcessArtifacts getProcessArtifacts() {
		return processArtifacts;
	}

	public void setProcessArtifacts(ProcessArtifacts processArtifacts) {
		this.processArtifacts = processArtifacts;
	}

}