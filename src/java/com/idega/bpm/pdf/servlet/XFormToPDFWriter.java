package com.idega.bpm.pdf.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.form.business.FormConverterToPDF;
import com.idega.block.form.data.dao.XFormsDAO;
import com.idega.bpm.BPMConstants;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.idegaweb.IWMainApplication;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.presentation.IWContext;
import com.idega.repository.bean.RepositoryItem;
import com.idega.util.CoreConstants;
import com.idega.util.FileUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.xformsmanager.business.DocumentManagerFactory;
import com.idega.xformsmanager.business.Form;
import com.idega.xformsmanager.business.PersistenceManager;
import com.idega.xformsmanager.business.Submission;
import com.idega.xformsmanager.business.XFormPersistenceType;
import com.idega.xformsmanager.component.beans.LocalizedStringBean;

/**
 * Downloads PDF for provided XForm
 * @author <a href="mailto:valdas@idega.com>Valdas Žemaitis</a>
 * Created: 2008.05.10
 * @version $Revision: 1.10 $
 * Last modified: $Date: 2009/05/15 07:23:58 $ by $Author: valdas $
 */
public class XFormToPDFWriter extends DownloadWriter implements MediaWritable {

	public static final String XFORM_ID_PARAMETER = "XFormIdToDownload";
	public static final String PATH_IN_SLIDE_PARAMETER = "pathInSlideForXFormPDF";
	public static final String XFORM_SUBMISSION_ID_PARAMETER = "XFormSubmitionId";
	public static final String XFORM_SUBMISSION_UNIQUE_ID_PARAMETER = "XFormSubmissionUniqueId";
	public static final String DO_NOT_CHECK_EXISTENCE_OF_XFORM_IN_PDF_PARAMETER = "doNotCheckExistence";

	private static final Logger LOGGER = Logger.getLogger(XFormToPDFWriter.class.getName());

	private RepositoryItem resourceInPDF = null;
	private boolean showPDF;

	@Autowired(required = false)
	@XFormPersistenceType("slide")
	private transient PersistenceManager persistenceManager;

	@Autowired(required = false)
	private BPMFactory bpmFactory;

	@Autowired
	private FormConverterToPDF formConverter;

	@Autowired
	private XFormsDAO xformsDAO;

	@Autowired
	private DocumentManagerFactory documentManager;

	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		String paramShowPDF = iwc.getParameter("showPDF");
		showPDF = !StringUtil.isEmpty(paramShowPDF) && Boolean.TRUE.toString().equals(paramShowPDF);

		String taskInstanceId = iwc.getParameter(ProcessConstants.TASK_INSTANCE_ID);
		String formId = iwc.getParameter(XFORM_ID_PARAMETER);
		String formSubmissionId = iwc.getParameter(XFORM_SUBMISSION_ID_PARAMETER);
		String formSubmissionUniqueId = iwc.getParameter(XFORM_SUBMISSION_UNIQUE_ID_PARAMETER);

		String pdfName = null;
		String pathInSlide = null;

		if (taskInstanceId == null && formId == null && formSubmissionId == null && formSubmissionUniqueId == null) {
			LOGGER.log(Level.SEVERE, "Do not know what to download: taskInstanceId, formId, formSubmitionId and formSubmissionUniqueId are nulls");
			return;
		}
		if (!StringUtil.isEmpty(formSubmissionId) || !StringUtil.isEmpty(formSubmissionUniqueId)) {
			Submission submission = null;
			try {
				submission = getFormSubmission(formSubmissionId, formSubmissionUniqueId);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Unable to get instance of XForm submition by id: " + formSubmissionId);
			}
			if (submission == null) {
				return;
			}

			pdfName = getLocalizedFormName(submission, iwc.getCurrentLocale());
			pathInSlide = submission.getSubmissionStorageIdentifier();
			formSubmissionId = submission.getSubmissionUUID();	//	Using unique ID
		}

		if (!StringUtil.isEmpty(taskInstanceId)) {
			pdfName = getPDFName(taskInstanceId, iwc.getCurrentLocale());
		}

		if (StringUtil.isEmpty(pathInSlide)) {
			if (iwc.isParameterSet(PATH_IN_SLIDE_PARAMETER)) {
				pathInSlide = iwc.getParameter(PATH_IN_SLIDE_PARAMETER);

				if (pathInSlide == null || CoreConstants.EMPTY.equals(pathInSlide)) {
					LOGGER.log(Level.SEVERE, "Unknown path for resource in Slide");
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

		FormConverterToPDF formConverter = getFormConverter();
		if (formConverter == null) {
			return;
		}

		String pathToPdf = formConverter.getGeneratedPDFFromXForm(taskInstanceId, formId, formSubmissionId, pathInSlide, pdfName, checkExistence);
		if (StringUtil.isEmpty(pathToPdf)) {
			LOGGER.log(Level.SEVERE, "PDF from XForm was not generated!");
			return;
		}

		if (!setResource(pathToPdf)) {
			LOGGER.log(Level.SEVERE, "Error reading PDF document: " + pathToPdf);
			return;
		}
		if (resourceInPDF == null || !resourceInPDF.exists()) {
			return;
		}

		if (showPDF) {
			//	Setting inline attribute - we don't want to force download of PDF file
			iwc.getResponse().setHeader("Content-Disposition", "inline;filename=\"" + resourceInPDF.getName() + "\"");
		} else {
			Long length = Long.valueOf(resourceInPDF.getLength());
			setAsDownload(iwc, resourceInPDF.getName(), length.intValue());
		}
	}

	private boolean setResource(String pathToPDF) {
		try {
			resourceInPDF = getRepositoryService().getRepositoryItemAsRootUser(pathToPDF);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting resource: " + pathToPDF, e);
		}

		return resourceInPDF == null ? false : true;
	}

	@Override
	public void writeTo(OutputStream streamOut) throws IOException {
		if (resourceInPDF == null) {
			LOGGER.log(Level.SEVERE, "Unable to get resource: " + resourceInPDF.getName());
			return;
		}

		InputStream streamIn = resourceInPDF.getInputStream();
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
				LOGGER.log(Level.SEVERE, "Error getting Spring bean: " + PersistenceManager.class, e);
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
				LOGGER.log(Level.SEVERE, "Error getting Spring bean: " + BPMFactory.class, e);
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
			LOGGER.log(Level.WARNING, "Error converting task instance ID from String to Long: " + taskInstanceId, e);
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
			LOGGER.log(Level.WARNING, "Error getting name for task instance by ID: " + taskInstance + " and locale: " + locale, e);
			return null;
		}

		String caseIdentifier = null;
		try {
			caseIdentifier = tiw.getProcessInstanceW().getProcessIdentifier();
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting case identifier for task instance: " + taskInstanceId, e);
		}
		if (StringUtil.isEmpty(caseIdentifier)) {
			return taskName;
		}

		return new StringBuilder(taskName).append(CoreConstants.MINUS).append(caseIdentifier).toString();
	}

	private String getLocalizedFormName(Submission submission, Locale locale) {
		Form form = null;
		try {
			form = submission.getXform();
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting XForm!", e);
		}
		if (form == null) {
			return "Unknown";
		}

		LocalizedStringBean title = null;
		try {
			title = getDocumentManager().newDocumentManager(IWMainApplication.getDefaultIWMainApplication()).openFormLazy(form.getFormId()).getFormTitle();
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting localized title for form: " + form.getFormId(), e);
		}

		return title == null ? form.getDisplayName() : title.getString(locale);
	}

	private Submission getFormSubmission(String formSubmissionId, String formSubmissionUniqueId) throws Exception {
		if (!StringUtil.isEmpty(formSubmissionId)) {
			if (getPersistenceManager() == null) {
				return null;
			}
			return getPersistenceManager().getSubmission(Long.valueOf(formSubmissionId));
		}

		return getXformsDAO().getSubmissionBySubmissionUUID(formSubmissionUniqueId);
	}

	public FormConverterToPDF getFormConverter() {
		if (formConverter == null) {
			try {
				ELUtil.getInstance().autowire(this);
			} catch(Exception e) {
				LOGGER.log(Level.SEVERE, "Error getting Spring bean: " + FormConverterToPDF.class, e);
			}
		}
		return formConverter;
	}

	public void setFormConverter(FormConverterToPDF formConverter) {
		this.formConverter = formConverter;
	}

	public XFormsDAO getXformsDAO() {
		if (xformsDAO == null) {
			try {
				ELUtil.getInstance().autowire(this);
			} catch(Exception e) {
				LOGGER.log(Level.SEVERE, "Error getting Spring bean: " + XFormsDAO.class, e);
			}
		}
		return xformsDAO;
	}

	public void setXformsDAO(XFormsDAO xformsDAO) {
		this.xformsDAO = xformsDAO;
	}

	public DocumentManagerFactory getDocumentManager() {
		if (documentManager == null) {
			try {
				ELUtil.getInstance().autowire(this);
			} catch(Exception e) {
				LOGGER.log(Level.SEVERE, "Error getting Spring bean: " + DocumentManagerFactory.class, e);
			}
		}
		return documentManager;
	}

	public void setDocumentManager(DocumentManagerFactory documentManager) {
		this.documentManager = documentManager;
	}

}