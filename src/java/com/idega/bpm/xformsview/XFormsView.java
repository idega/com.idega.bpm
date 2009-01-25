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

import com.idega.block.form.data.XForm;
import com.idega.block.form.data.dao.XFormsDAO;
import com.idega.block.form.presentation.FormViewer;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.variables.Converter;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewToTask;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.xformsmanager.business.Document;
import com.idega.xformsmanager.business.DocumentManager;
import com.idega.xformsmanager.business.DocumentManagerFactory;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.11 $
 * 
 *          Last modified: $Date: 2009/01/25 15:44:13 $ by $Author: civilis $
 */
public class XFormsView implements View {

	public static final String VIEW_TYPE = "xforms";
	public static final String FORM_TYPE = "bpm";

	private String viewId;
	private Long taskInstanceId;
	private boolean submitable = true;
	private DocumentManagerFactory documentManagerFactory;
	private Document form;
	private Converter converter;
	private Map<String, String> parameters;
	private Map<String, Object> variables;

	@Autowired
	private ViewToTask viewToTask;
	@Autowired
	private XFormsDAO xformsDAO;

	public ViewToTask getViewToTask() {
		if (viewToTask == null) {
			ELUtil.getInstance().autowire(this);
		}
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

	public void setViewId(String viewId) {
		form = null;
		parameters = null;
		variables = null;
		this.viewId = viewId;
	}

	public String getViewId() {
		return viewId;
	}

	public String getViewType() {
		return VIEW_TYPE;
	}

	public void setViewType(String viewType) {
		throw new UnsupportedOperationException(
				"XFormsView view type cannot be changed");
	}

	public DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	public void setDocumentManagerFactory(
			DocumentManagerFactory documentManagerFactory) {
		this.documentManagerFactory = documentManagerFactory;
	}

	public UIComponent getViewForDisplay() {
		return getViewForDisplay(false);
	}

	public UIComponent getViewForDisplay(boolean pdfViewer) {
		FacesContext context = FacesContext.getCurrentInstance();
		Application application = context.getApplication();

		FormViewer formviewer = (FormViewer) application
				.createComponent(FormViewer.COMPONENT_TYPE);
		formviewer.setXFormsDocument(getFormDocumentWithData()
				.getXformsDocument());
		formviewer.setPdfViewer(pdfViewer);

		return formviewer;
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
		populateVariables(getVariables());

		return doc;
	}

	protected Document getFormDocument() {

		if (form == null) {

			if (getViewId() == null || getViewId().length() == 0)
				throw new IllegalStateException(
						"Tried to get form document, but no view id not set");

			final Long formId = new Long(getViewId());

			Logger.getLogger(getClass().getName()).finer(
					"Opening form in xforms view by form id = " + formId);

			try {
				DocumentManager documentManager = getDocumentManagerFactory()
						.newDocumentManager(
								IWMainApplication.getDefaultIWMainApplication());
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

	public boolean isSubmitable() {
		return submitable;
	}

	public void setSubmitable(boolean submitable) {

		this.submitable = submitable;

		if (isFormDocumentLoaded()) {

			getFormDocument().setReadonly(!submitable);
		}
	}

	public void populateParameters(Map<String, String> parameters) {

		this.parameters = parameters;

		if (parameters != null && isFormDocumentLoaded()) {
			getFormDocument().getParametersManager().cleanUpdate(parameters);
		}
	}

	public void populateVariables(Map<String, Object> variables) {

		this.variables = variables;

		if (variables != null && isFormDocumentLoaded()) {
			getConverter().revert(variables,
					getFormDocument().getSubmissionInstanceElement());
		}
	}

	public Map<String, String> resolveParameters() {

		if (parameters != null)
			return parameters;

		throw new UnsupportedOperationException(
				"Resolving parameters from form not supported yet.");
	}

	public Map<String, Object> resolveVariables() {

		if (variables != null)
			return variables;

		throw new UnsupportedOperationException(
				"Resolving variables from form not supported yet.");
	}

	public String getDisplayName() {
		return getDisplayName(new Locale("is", "IS"));
	}

	public Date getDateCreated() {

		// TODO: implement
		return new Date();
	}

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

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public String getDisplayName(Locale locale) {

		// TODO: cache here by viewid

		String displayName;

		try {
			Document document = getFormDocument();
			displayName = document.getFormTitle().getString(locale);

		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING,
					"Exception while resolving form title by locale=" + locale,
					e);
			displayName = null;
		}

		return displayName;
	}

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

}