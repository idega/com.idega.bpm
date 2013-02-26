package com.idega.bpm.xformsview;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import com.idega.block.form.data.XForm;
import com.idega.block.form.data.dao.XFormsDAO;
import com.idega.block.form.presentation.FormViewer;
import com.idega.bpm.BPMConstants;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.variables.Converter;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewToTask;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.text.Heading2;
import com.idega.util.CoreUtil;
import com.idega.util.PresentationUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.xformsmanager.business.Document;
import com.idega.xformsmanager.business.DocumentManager;
import com.idega.xformsmanager.business.DocumentManagerFactory;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.12 $ Last modified: $Date: 2009/03/27 15:54:41 $ by $Author: civilis $
 */
public class XFormsView implements View {

	public static final String VIEW_TYPE = "xforms";
	public static final String FORM_TYPE = "bpm";

	private String viewId;
	private Long taskInstanceId;
	private boolean submitable = true, submitted;
	private DocumentManagerFactory documentManagerFactory;
	private Document form;
	private Converter converter;
	private Map<String, String> parameters;
	private Map<String, Object> variables;

	@Autowired
	private ViewToTask viewToTask;
	@Autowired
	private XFormsDAO xformsDAO;

	@Override
	public ViewToTask getViewToTask() {
		if (viewToTask == null)
			ELUtil.getInstance().autowire(this);

		return viewToTask;
	}

	public void setViewToTask(ViewToTask viewToTask) {
		this.viewToTask = viewToTask;
	}

	public Converter getConverter() {
		return converter;
	}

	public void setConverter(Converter converter) {
		this.converter = converter;
	}

	@Override
	public void setViewId(String viewId) {
		form = null;
		parameters = null;
		variables = null;
		this.viewId = viewId;
	}

	@Override
	public String getViewId() {
		return viewId;
	}

	@Override
	public String getViewType() {
		return VIEW_TYPE;
	}

	@Override
	public void setViewType(String viewType) {
		throw new UnsupportedOperationException("XFormsView view type cannot be changed");
	}

	public DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	public void setDocumentManagerFactory(DocumentManagerFactory documentManagerFactory) {
		this.documentManagerFactory = documentManagerFactory;
	}

	@Override
	public UIComponent getViewForDisplay() {
		return getViewForDisplay(false);
	}

	@Override
	public UIComponent getViewForDisplay(boolean pdfViewer) {
		FacesContext context = FacesContext.getCurrentInstance();
		Application application = context.getApplication();

		org.w3c.dom.Document xform = null;
		try {
			Document xformWithData = getFormDocumentWithData();
			xform = xformWithData == null ? null : xformWithData.getXformsDocument();
		} catch (Exception e) {
			e.printStackTrace();
			CoreUtil.sendExceptionNotification("Error rendering XForm: unable to load XML document or populate variables. View ID: " + viewId +
					", task instance ID: " + taskInstanceId, e);
		}
		if (xform == null) {
			IWContext iwc = IWContext.getIWContext(context);
			IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(BPMConstants.IW_BUNDLE_STARTER).getResourceBundle(iwc);
			Layer errorBlock = new Layer();
			Heading2 errorLabel = new Heading2(iwrb.getLocalizedString("error_rendering_xform",
					"Sorry, some error occurred. We are working on it. Please, try later..."));
			errorBlock.add(errorLabel);
			String action = "closeAllLoadingMessages();";
			if (!CoreUtil.isSingleComponentRenderingProcess(iwc))
				action = "jQuery(window).load(function() {" + action + "});";
			errorBlock.add(PresentationUtil.getJavaScriptAction(action));
			return errorBlock;
		} else {
			FormViewer formviewer = (FormViewer) application.createComponent(FormViewer.COMPONENT_TYPE);
			formviewer.setXFormsDocument(xform);
			formviewer.setPdfViewer(pdfViewer);
			formviewer.setSubmitted(isSubmitted());
			return formviewer;
		}
	}

	public void setFormDocument(Document formDocument) {
		form = formDocument;
		viewId = form.getFormId().toString();
	}

	protected boolean isFormDocumentLoaded() {
		return form != null;
	}

	protected Document getFormDocumentWithData() {
		Document doc = getFormDocument();

		if (!isSubmitable())
			form.setReadonly(true);

		populateParameters(getParameters());

		return populateVariables(getVariables()) ? doc : null;
	}

	protected Document getFormDocument() {
		if (form == null) {
			if (getViewId() == null || getViewId().length() == 0)
				throw new IllegalStateException("Tried to get form document, but no view id not set");

			final Long formId = new Long(getViewId());

			Logger.getLogger(getClass().getName()).finer("Opening form in xforms view by form id = " + formId);

			try {
				DocumentManager documentManager = getDocumentManagerFactory().newDocumentManager(IWMainApplication.getDefaultIWMainApplication());
				form = documentManager.openFormLazy(formId);

				setFormDocument(form);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return form;
	}

	@Override
	public boolean isSubmitable() {
		return submitable;
	}

	@Override
	public void setSubmitable(boolean submitable) {
		this.submitable = submitable;

		if (isFormDocumentLoaded()) {
			getFormDocument().setReadonly(!submitable);
		}
	}

	@Override
	public void populateParameters(Map<String, String> parameters) {
		this.parameters = parameters;

		if (parameters != null && isFormDocumentLoaded()) {
			getFormDocument().getParametersManager().cleanUpdate(parameters);
		}
	}

	@Override
	public boolean populateVariables(Map<String, Object> variables) {
		this.variables = variables;

		if (variables != null && isFormDocumentLoaded()) {
			Element element = null;
			Object o = null;
			try {
				element = getFormDocument().getSubmissionInstanceElement();
				o = getConverter().revert(variables, element);
			} catch (Exception e) {
				String message = "Unable to revert variables " + variables + " into the object from element " + element;
				CoreUtil.sendExceptionNotification(message, e);
			}
			if (o == null)
				return false;
		}

		return true;
	}

	@Override
	public Map<String, String> resolveParameters() {
		if (parameters != null)
			return parameters;

		throw new UnsupportedOperationException("Resolving parameters from form not supported yet.");
	}

	@Override
	public Map<String, Object> resolveVariables() {
		if (variables != null)
			return variables;

		throw new UnsupportedOperationException("Resolving variables from form not supported yet.");
	}

	@Override
	public String getDisplayName() {
		return getDisplayName(new Locale("is", "IS"));
	}

	@Override
	public Date getDateCreated() {
		// TODO: implement
		return new Date();
	}

	@Override
	public void takeView() {
		if (StringUtil.isEmpty(getViewId()))
			throw new IllegalStateException(
			        "Tried to take view, but no viewId not set");

		final Long formId = new Long(getViewId());

		FacesContext fctx = FacesContext.getCurrentInstance();
		IWMainApplication iwma = fctx == null ? IWMainApplication
		        .getDefaultIWMainApplication() : IWMainApplication
		        .getIWMainApplication(fctx);

		DocumentManager docMan = getDocumentManagerFactory()
		        .newDocumentManager(iwma);
		Document formDocument = docMan.takeForm(formId);

		setFormDocument(formDocument);
	}

	@Override
	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	@Override
	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	@Override
	public String getDisplayName(Locale locale) {
		// TODO: cache here by viewid

		String displayName;

		try {
			Document document = getFormDocument();
			displayName = document.getFormTitle().getString(locale);

		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING,
			    "Exception while resolving form title by locale=" + locale, e);
			displayName = null;
		}

		return displayName;
	}

	@Override
	public String getDefaultDisplayName() {

		if (StringUtil.isEmpty(getViewId()))
			throw new NullPointerException("View id not set");

		Long formId = new Long(getViewId());
		XForm xform = getXformsDAO().getXFormById(formId);

		return xform.getDisplayName();
	}

	XFormsDAO getXformsDAO() {

		if (xformsDAO == null)
			ELUtil.getInstance().autowire(this);

		return xformsDAO;
	}

	protected Map<String, String> getParameters() {
		return parameters;
	}

	protected Map<String, Object> getVariables() {
		return variables;
	}

	public boolean isSubmitted() {
		return submitted;
	}

	public void setSubmitted(boolean submitted) {
		this.submitted = submitted;
	}

	@Override
	public boolean hasViewForDisplay() {
		return true;
	}
}