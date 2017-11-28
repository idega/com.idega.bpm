package com.idega.bpm.jsfcomponentview;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewToTask;
import com.idega.presentation.IWContext;
import com.idega.util.expression.ELUtil;

public class JSFComponentView implements View, Serializable {

	private static final long serialVersionUID = -5876615718277520488L;

	public static final String VIEW_TYPE = "jsf";

	private String viewId;
	private Serializable taskInstanceId;
	private boolean submitable = true, submitted;
	private Map<String, Object> variables;
	private Map<String, String> parameters;

	@Autowired
	private transient ViewToTask viewToTask;

	@Override
	public Date getDateCreated() {
		return new Date();
	}

	@Override
	public String getDefaultDisplayName() {
		FacesContext context = FacesContext.getCurrentInstance();
		UIComponent component = context.getApplication().createComponent(getViewId());
		BPMCapableJSFComponent jsfComponent = (BPMCapableJSFComponent) component;

		return jsfComponent.getDefaultDisplayName();
	}

	@Override
	public String getDisplayName() {
		return getDefaultDisplayName();
	}

	@Override
	public String getDisplayName(Locale locale) {
		FacesContext context = FacesContext.getCurrentInstance();
		UIComponent component = context.getApplication().createComponent(getViewId());
		BPMCapableJSFComponent jsfComponent = (BPMCapableJSFComponent) component;

		IWContext iwc = IWContext.getIWContext(context);
		iwc.getRequest().setAttribute(ProcessConstants.TASK_INSTANCE_ID, getTaskInstanceId());

		return jsfComponent.getDisplayName(locale);
	}

	@Override
	public <T> T getTaskInstanceId() {
		@SuppressWarnings("unchecked")
		T id = (T) taskInstanceId;
		return id;
	}

	@Override
	public UIComponent getViewForDisplay() {
		return getViewForDisplay(false);
	}

	@Override
	public UIComponent getViewForDisplay(boolean pdfViewer) {
		// TODO: finish him
		FacesContext context = FacesContext.getCurrentInstance();
		UIComponent component = context.getApplication().createComponent(getViewId());

		BPMCapableJSFComponent jsfComponent = (BPMCapableJSFComponent) component;
		jsfComponent.setView(this);

		return component;
	}

	@Override
	public String getViewId() {
		return viewId;
	}

	@Override
	public ViewToTask getViewToTask() {
		if (viewToTask == null)
			ELUtil.getInstance().autowire(this);

		return viewToTask;
	}

	@Override
	public String getViewType() {
		return VIEW_TYPE;
	}

	@Override
	public boolean isSubmitable() {
		return submitable;
	}

	@Override
	public void populateParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	@Override
	public boolean populateVariables(Map<String, Object> variables) {
		this.variables = variables;
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
	public void setSubmitable(boolean submitable) {
		this.submitable = submitable;
	}

	@Override
	public <T> void setTaskInstanceId(T taskInstanceId) {
		this.taskInstanceId = (Serializable) taskInstanceId;
	}

	@Override
	public void setViewId(String viewId) {
		this.viewId = viewId;
	}

	@Override
	public void setViewType(String viewType) {
		throw new UnsupportedOperationException("JSFComponentView view type cannot be changed");
	}

	@Override
	public void takeView() {
		// Not needed atm.
	}

	@Override
	public boolean hasViewForDisplay() {
		return true;
	}

	@Override
	public void setSubmitted(boolean submitted) {
		this.submitted = submitted;
	}

	@Override
	public boolean isSubmitted() {
		return submitted;
	}
}