package com.idega.bpm.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpException;
import org.apache.webdav.lib.WebdavResource;

import com.idega.block.form.presentation.FormViewer;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.graphics.generator.business.PDFGenerator;
import com.idega.idegaweb.IWMainApplication;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.jbpm.artifacts.presentation.ProcessArtifacts;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;
import com.idega.util.FileUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * Downloads PDF for provided XForm
 * @author <a href="mailto:valdas@idega.com>Valdas Žemaitis</a>
 * Created: 2008.05.10
 * @version $Revision: 1.1 $
 * Last modified: $Date: 2008/09/17 13:09:02 $ by $Author: civilis $
 */
public class XFormToPDFWriter extends DownloadWriter implements MediaWritable { 
	
	public static final String XFORM_ID_PARAMETER = "XFormIdToDownload";
	public static final String TASK_INSTANCE_ID_PARAMETER = "taskInstanceId";
	public static final String PATH_IN_SLIDE_PARAMETER = "pathInSlideForXFormPDF";
	public static final String DO_NOT_CHECK_EXISTENCE_OF_XFORM_IN_PDF_PARAMETER = "doNotCheckExistence";
	
	private WebdavResource xformInPDF = null;
	
	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		String taskInstanceId = iwc.getParameter(TASK_INSTANCE_ID_PARAMETER);
		String formId = iwc.getParameter(XFORM_ID_PARAMETER);
		if (taskInstanceId == null && formId == null) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Do not know what to download: taskInstanceId and formId are nulls");
			return;
		}
		
		String pathInSlide = null;
		if (iwc.isParameterSet(PATH_IN_SLIDE_PARAMETER)) {
			pathInSlide = iwc.getParameter(PATH_IN_SLIDE_PARAMETER);
		
			if (pathInSlide == null || CoreConstants.EMPTY.equals(pathInSlide)) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unknown path for XForm in Slide");
				return;
			}
		}
		else {
			pathInSlide = CoreConstants.CONTENT_PATH + "/xforms/pdf/";
		}
		if (!pathInSlide.startsWith(CoreConstants.SLASH)) {
			pathInSlide = CoreConstants.SLASH + pathInSlide;
		}
		if (!pathInSlide.endsWith(CoreConstants.SLASH)) {
			pathInSlide += CoreConstants.SLASH;
		}
		
		boolean checkExistence = false;
		if (iwc.isParameterSet(DO_NOT_CHECK_EXISTENCE_OF_XFORM_IN_PDF_PARAMETER)) {
			checkExistence = Boolean.valueOf(iwc.getParameter(DO_NOT_CHECK_EXISTENCE_OF_XFORM_IN_PDF_PARAMETER));
		}
		
		if (!getXForm(iwc, taskInstanceId, formId, pathInSlide, checkExistence)) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unable to get 'XForm' with id: " + taskInstanceId == null ? formId : taskInstanceId);
			return;
		}
	}
	
	private boolean getXForm(IWContext iwc, String taskInstanceId, String formId, String pathInSlide, boolean checkExistence) {
		IWSlideService slide = null;
		try {
			slide = (IWSlideService) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), IWSlideService.class);
		} catch (IBOLookupException e) {
			e.printStackTrace();
		}
		if (slide == null) {
			return false;
		}
		
		String prefix = formId == null ? taskInstanceId : formId;
		prefix = prefix == null ? String.valueOf(System.currentTimeMillis()) : prefix;
		String pdfName = new StringBuilder("Form_").append(prefix).append(".pdf").toString();
		String pathToForm = pathInSlide + pdfName;
		
		if (!checkExistence) {
			//	Generating PDF for XForm despite PDF already exists or not. If exists - it will be overridden
			if (!generatePDF(iwc, slide, taskInstanceId, formId, pathInSlide, pdfName, pathToForm)) {
				return false;
			}
		}
		else if (setXForm(slide, pathToForm)) {
			if (xformInPDF == null || !xformInPDF.exists()) {
				//	XForm in PDF doesn't exist - trying to generate it
				if (!generatePDF(iwc, slide, taskInstanceId, formId, pathInSlide, pdfName, pathToForm)) {
					return false;
				}
			}
		}
		
		if (xformInPDF == null || !xformInPDF.exists()) {
			return false;
		}
		Long length = Long.valueOf(xformInPDF.getGetContentLength());
		setAsDownload(iwc, pdfName, length.intValue());
		
		return true;
	}
	
	private UIComponent getComponentToRender(IWContext iwc, String taskInstanceId, String formId) {
		UIComponent viewer = null;
		if (!StringUtil.isEmpty(taskInstanceId)) {
			ProcessArtifacts bean = ELUtil.getInstance().getBean(CoreConstants.SPRING_BEAN_NAME_PROCESS_ARTIFACTS);
			if (bean == null) {
				return null;
			}
			try {
				return bean.getViewInUIComponent(Long.valueOf(taskInstanceId));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (!StringUtil.isEmpty(formId)) {
			Application application = iwc.getApplication();
			viewer = application.createComponent(FormViewer.COMPONENT_TYPE);
			((FormViewer) viewer).setFormId(formId);
		}
		
		return viewer;
	}
	
	private boolean generatePDF(IWContext iwc, IWSlideService slide, String taskInstanceId, String formId, String pathInSlide, String pdfName,
			String pathToForm) {
		UIComponent viewer = getComponentToRender(iwc, taskInstanceId, formId);
		if (viewer == null) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unable to get viewer for " + taskInstanceId == null ? "xform: " + formId : "taskInstance: " 
				+ taskInstanceId);
			return false;
		}
		boolean isFormViewer = viewer instanceof FormViewer;
		if (isFormViewer) {
			((FormViewer) viewer).setPdfViewer(true);
		}
		
		PDFGenerator generator = ELUtil.getInstance().getBean(CoreConstants.SPRING_BEAN_NAME_PDF_GENERATOR);
		if (generator == null) {
			return false;
		}
		if (generator.generatePDF(iwc, viewer, pdfName, pathInSlide, true, isFormViewer)) {
			return setXForm(slide, pathToForm);
		}
		
		Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unable to generate PDF for " + taskInstanceId == null ? "xform: " + formId: "taskInstance: " +
				taskInstanceId);
		return false;
	}

	private boolean setXForm(IWSlideService slide, String pathToForm) {
		try {
			xformInPDF = slide.getWebdavResourceAuthenticatedAsRoot(pathToForm);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return xformInPDF == null ? false : true;
	}
	
	@Override
	public void writeTo(OutputStream streamOut) throws IOException {
		if (xformInPDF == null) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unable to get XForm");
			return;
		}
		
		InputStream streamIn = xformInPDF.getMethodData();
		FileUtil.streamToOutputStream(streamIn, streamOut);
		
		streamOut.flush();
		streamOut.close();
		streamIn.close();
	}
	
	@Override
	public String getMimeType() {
		return MimeTypeUtil.MIME_TYPE_PDF_1;
	}
	
}