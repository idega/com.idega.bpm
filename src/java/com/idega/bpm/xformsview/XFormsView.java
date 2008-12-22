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
import com.idega.jbpm.view.ViewToTaskType;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.xformsmanager.business.Document;
import com.idega.xformsmanager.business.DocumentManager;
import com.idega.xformsmanager.business.DocumentManagerFactory;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $
 * 
 *          Last modified: $Date: 2008/12/22 08:58:42 $ by $Author: juozas $
 */
public class XFormsView implements View {

	public static final String VIEW_TYPE = "xforms";
	public static final String FORM_TYPE = "bpm";

	private String viewId;
	private Long taskInstanceId;
	private boolean submitable = true;
	private String displayName;
	private DocumentManagerFactory documentManagerFactory;
	private Document form;
	private Converter converter;
	private Map<String, String> parameters;
	private Map<String, Object> variables;

	private ViewToTask viewToTask;
	@Autowired
	private XFormsDAO xformsDAO;

	public ViewToTask getViewToTask() {
		if (viewToTask == null) {
			ELUtil.getInstance().autowire(this);
		}
		return viewToTask;
	}

	@Autowired
	public void setViewToTask(@ViewToTaskType("xforms") ViewToTask viewToTask) {
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
		formviewer.setXFormsDocument(getFormDocument().getXformsDocument());
		formviewer.setPdfViewer(pdfViewer);

		return formviewer;
	}

	public void setFormDocument(Document formDocument) {

		form = formDocument;

		if (!isSubmitable())
			form.setReadonly(true);

		populateParameters(getParameters());
		populateVariables(getVariables());

		viewId = form.getFormId().toString();
	}

	protected boolean isFormDocumentLoaded() {

		return form != null;
	}

	protected Document getFormDocument() {

		if (form == null) {
			
			if (getViewId() == null || getViewId().length() == 0)
				throw new IllegalStateException(
						"Tried to get form document, but no view id not set");

			final Long formId = new Long(getViewId());
			
			Logger.getLogger(getClass().getName()).finer("Opening form in xforms view by form id = "+formId);

			try {
				FacesContext fctx = FacesContext.getCurrentInstance();
				IWMainApplication iwma = fctx == null ? IWMainApplication
						.getDefaultIWMainApplication() : IWMainApplication
						.getIWMainApplication(fctx);

				DocumentManager documentManager = getDocumentManagerFactory()
						.newDocumentManager(iwma);
				form = documentManager.openForm(formId);

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

		Document formDocument = getFormDocument();

		FacesContext fctx = FacesContext.getCurrentInstance();
		IWMainApplication iwma = fctx == null ? IWMainApplication
				.getDefaultIWMainApplication() : IWMainApplication
				.getIWMainApplication(fctx);

		DocumentManager docMan = getDocumentManagerFactory()
				.newDocumentManager(iwma);
		formDocument = docMan.takeForm(formDocument.getFormId());

		setFormDocument(formDocument);
	}

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public String getDisplayName(Locale locale) {

		// TODO: get rid of displayName, which is not for any locale! and cache
		// here

		if (displayName == null) {

			try {
				Document document = getFormDocument();
				displayName = document.getFormTitle().getString(locale);

			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(
						Level.WARNING,
						"Exception while resolving form title by locale="
								+ locale, e);
				displayName = null;
			}
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