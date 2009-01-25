package com.idega.bpm.jsfcomponentview;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewToTask;
import com.idega.util.expression.ELUtil;

public class JSFComponentView implements View, Serializable {

	private static final long serialVersionUID = -5876615718277520488L;

	public static final String VIEW_TYPE = "jsf";

	private String viewId;
	private Long taskInstanceId;
	private boolean submitable = true;
	private Map<String, Object> variables;

	@Autowired
	private transient ViewToTask viewToTask;

	public Date getDateCreated() {
		return new Date();
	}

	public String getDefaultDisplayName() {
		FacesContext context = FacesContext.getCurrentInstance();
		UIComponent component = context.getApplication().createComponent(
				getViewId());
		BPMCapableJSFComponent jsfComponent = (BPMCapableJSFComponent) component;

		return jsfComponent.getDefaultDisplayName();
	}

	public String getDisplayName() {
		return getDefaultDisplayName();
	}

	public String getDisplayName(Locale locale) {
		FacesContext context = FacesContext.getCurrentInstance();
		UIComponent component = context.getApplication().createComponent(
				getViewId());
		BPMCapableJSFComponent jsfComponent = (BPMCapableJSFComponent) component;

		return jsfComponent.getDisplayName(locale);
	}

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public UIComponent getViewForDisplay() {
		return getViewForDisplay(false);
	}

	public UIComponent getViewForDisplay(boolean pdfViewer) {
		// TODO: finish him
		FacesContext context = FacesContext.getCurrentInstance();
		UIComponent component = context.getApplication().createComponent(
				getViewId());

		BPMCapableJSFComponent jsfComponent = (BPMCapableJSFComponent) component;
		jsfComponent.setView(this);

		return component;
	}

	public String getViewId() {
		return viewId;
	}

	public ViewToTask getViewToTask() {
		if (viewToTask == null) {
			ELUtil.getInstance().autowire(this);
		}
		return viewToTask;
	}

	public String getViewType() {
		return VIEW_TYPE;
	}

	public boolean isSubmitable() {
		return submitable;
	}

	public void populateParameters(Map<String, String> parameters) {
	}

	public void populateVariables(Map<String, Object> variables) {
		this.variables = variables;
	}

	public Map<String, String> resolveParameters() {
		throw new UnsupportedOperationException(
				"Resolving parameters not supported by: "
						+ this.getClass().getName());
	}

	public Map<String, Object> resolveVariables() {
		if (variables != null)
			return variables;

		throw new UnsupportedOperationException(
				"Resolving variables from form not supported yet.");
	}

	public void setSubmitable(boolean submitable) {
		this.submitable = submitable;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public void setViewId(String viewId) {
		this.viewId = viewId;
	}

	public void setViewType(String viewType) {
		throw new UnsupportedOperationException(
				"JSFComponentView view type cannot be changed");
	}

	public void takeView() {
		// Not needed atm.
	}
}