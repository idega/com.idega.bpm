package com.idega.bpm.pdf.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpException;
import org.apache.webdav.lib.WebdavResource;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.bpm.BPMConstants;
import com.idega.bpm.pdf.business.ProcessTaskInstanceConverterToPDF;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;
import com.idega.util.FileUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.xformsmanager.business.PersistenceManager;
import com.idega.xformsmanager.business.Submission;
import com.idega.xformsmanager.business.XFormPersistenceType;

/**
 * Downloads PDF for provided XForm
 * @author <a href="mailto:valdas@idega.com>Valdas Žemaitis</a>
 * Created: 2008.05.10
 * @version $Revision: 1.7 $
 * Last modified: $Date: 2009/01/26 11:08:40 $ by $Author: valdas $
 */
public class XFormToPDFWriter extends DownloadWriter implements MediaWritable { 
	
	public static final String XFORM_ID_PARAMETER = "XFormIdToDownload";
	public static final String TASK_INSTANCE_ID_PARAMETER = "taskInstanceId";
	public static final String PATH_IN_SLIDE_PARAMETER = "pathInSlideForXFormPDF";
	public static final String XFORM_SUBMISSION_ID_PARAMETER = "XFormSubmitionId";
	public static final String DO_NOT_CHECK_EXISTENCE_OF_XFORM_IN_PDF_PARAMETER = "doNotCheckExistence";
	
	private static final Logger logger = Logger.getLogger(XFormToPDFWriter.class.getName());
	
	private WebdavResource resourceInPDF = null;
	
	@Autowired(required = false)
	@XFormPersistenceType("slide")
	private transient PersistenceManager persistenceManager;
	
	@Autowired(required = false)
	private BPMFactory bpmFactory;
	
	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		String taskInstanceId = iwc.getParameter(TASK_INSTANCE_ID_PARAMETER);
		String formId = iwc.getParameter(XFORM_ID_PARAMETER);
		String formSubmitionId = iwc.getParameter(XFORM_SUBMISSION_ID_PARAMETER);
		
		String pdfName = null;
		String pathInSlide = null;
		
		if (taskInstanceId == null && formId == null && formSubmitionId == null) {
			logger.log(Level.SEVERE, "Do not know what to download: taskInstanceId, formId and formSubmitionId are nulls");
			return;
		}
		if (!StringUtil.isEmpty(formSubmitionId)) {
			if (getPersistenceManager() == null) {
				logger.log(Level.SEVERE, "Unable to get instance of: " + PersistenceManager.class.getName());
				return;
			}
			Submission xformSubmition = null;
			xformSubmition = getPersistenceManager().getSubmission(Long.valueOf(formSubmitionId));
			if (xformSubmition == null) {
				logger.log(Level.SEVERE, "Unable to get instance of XForm submition by id: " + formSubmitionId);
				return;
			}
			
			pdfName = xformSubmition.getXform().getDisplayName();
			pathInSlide = xformSubmition.getSubmissionStorageIdentifier();
			formSubmitionId = xformSubmition.getSubmissionUUID();	//	Using unique ID
		}
		
		if (!StringUtil.isEmpty(taskInstanceId)) {
			pdfName = getPDFName(taskInstanceId, iwc.getCurrentLocale());
		}
		
		if (StringUtil.isEmpty(pathInSlide)) {
			if (iwc.isParameterSet(PATH_IN_SLIDE_PARAMETER)) {
				pathInSlide = iwc.getParameter(PATH_IN_SLIDE_PARAMETER);
			
				if (pathInSlide == null || CoreConstants.EMPTY.equals(pathInSlide)) {
					logger.log(Level.SEVERE, "Unknown path for resource in Slide");
					return;
				}
			}
			else {
				pathInSlide = BPMConstants.PDF_OF_XFORMS_PATH_IN_SLIDE;
			}
		}
		
		boolean checkExistence = false;
		if (iwc.isParameterSet(DO_NOT_CHECK_EXISTENCE_OF_XFORM_IN_PDF_PARAMETER)) {
			checkExistence = Boolean.valueOf(iwc.getParameter(DO_NOT_CHECK_EXISTENCE_OF_XFORM_IN_PDF_PARAMETER));
		}
		
		ProcessTaskInstanceConverterToPDF pdfGenerator = null;
		try {
			pdfGenerator = ELUtil.getInstance().getBean(ProcessTaskInstanceConverterToPDF.STRING_BEAN_IDENTIFIER);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error getting Spring bean!", e);
		}
		if (pdfGenerator == null) {
			return;
		}
		
		String pathToPdf = pdfGenerator.getGeneratedPDFFromXForm(taskInstanceId, formId, formSubmitionId, pathInSlide, pdfName, checkExistence);
		if (StringUtil.isEmpty(pathToPdf)) {
			logger.log(Level.SEVERE, "PDF from XForm was not generated!");
			return;
		}
		
		IWSlideService slide = null;
		try {
			slide = (IWSlideService) IBOLookup.getServiceInstance(iwc, IWSlideService.class);
		} catch (IBOLookupException e) {
			logger.log(Level.SEVERE, "Error getting IWSlideService!", e);
		}
		if (slide == null) {
			return;
		}
		
		if (!setResource(slide, pathToPdf)) {
			logger.log(Level.SEVERE, "Error reading PDF document: " + pathToPdf);
			return;
		}
		if (resourceInPDF == null || !resourceInPDF.exists()) {
			return;
		}
		Long length = Long.valueOf(resourceInPDF.getGetContentLength());
		setAsDownload(iwc, resourceInPDF.getDisplayName(), length.intValue());
	}

	private boolean setResource(IWSlideService slide, String pathToPDF) {
		try {
			resourceInPDF = slide.getWebdavResourceAuthenticatedAsRoot(pathToPDF);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return resourceInPDF == null ? false : true;
	}
	
	@Override
	public void writeTo(OutputStream streamOut) throws IOException {
		if (resourceInPDF == null) {
			logger.log(Level.SEVERE, "Unable to get resource: " + resourceInPDF.getName());
			return;
		}
		
		InputStream streamIn = resourceInPDF.getMethodData();
		FileUtil.streamToOutputStream(streamIn, streamOut);
		
		streamOut.flush();
		streamOut.close();
		streamIn.close();
	}
	
	@Override
	public String getMimeType() {
		return MimeTypeUtil.MIME_TYPE_PDF_1;
	}

	public PersistenceManager getPersistenceManager() {
		if (persistenceManager == null) {
			try {
				ELUtil.getInstance().autowire(this);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return persistenceManager;
	}

	public void setPersistenceManager(PersistenceManager persistenceManager) {
		this.persistenceManager = persistenceManager;
	}
	
	public BPMFactory getBpmFactory() {
		if (bpmFactory == null) {
			try {
				ELUtil.getInstance().autowire(this);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	private String getPDFName(String taskInstanceId, Locale locale) {
		if (StringUtil.isEmpty(taskInstanceId)) {
			return null;
		}
		
		BPMFactory bpmFactory = getBpmFactory();
		if (bpmFactory == null) {
			return null;
		}
		Long taskInstance = null;
		try {
			taskInstance = Long.valueOf(taskInstanceId);
		} catch(NumberFormatException e) {
			logger.log(Level.WARNING, "Error converting task instance ID from String to Long: " + taskInstanceId, e);
		}
		if (taskInstance == null) {
			return null;
		}
		
		ProcessManager processManager = bpmFactory.getProcessManagerByTaskInstanceId(taskInstance);
		
		TaskInstanceW tiw = processManager.getTaskInstance(taskInstance);
		String taskName = null;
		
		try {
			taskName = tiw.getName(locale);
			
			if(StringUtil.isEmpty(taskName))
				return null;
			
		} catch(Exception e) {
			logger.log(Level.WARNING, "Error getting name for task instance by ID: " + taskInstance + " and locale: " + locale, e);
			return null;
		}
		
		String caseIdentifier = null;
		try {
			caseIdentifier = tiw.getProcessInstanceW().getProcessIdentifier();
		} catch(Exception e) {
			logger.log(Level.WARNING, "Error getting case identifier for task instance: " + taskInstanceId, e);
		}
		if (StringUtil.isEmpty(caseIdentifier)) {
			return taskName;
		}
		
		return new StringBuilder(taskName).append(CoreConstants.MINUS).append(caseIdentifier).toString();
	}

}