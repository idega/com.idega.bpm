package com.idega.bpm.bundle;

import java.io.IOException;

import com.idega.bpm.jsfcomponentview.JSFComponentView;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewResource;

public class JSFComponentViewResource implements ViewResource {
	
	private String taskName;
	private String componentName;
	private View view;
	
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getTaskName() {
		return taskName;
	}

	public View store(IWMainApplication iwma) throws IOException {
		if(getView() != null){
			return view;
		}else if (getComponentName() == null){
			throw new IllegalStateException("Error crating view: JSF component name was not set");
		}else{
			
			JSFComponentView view = new JSFComponentView();
			view.setViewId(getComponentName());
			this.view = view;
			
			
		}
		return view;
	}

	public String getComponentName() {
		return componentName;
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	public View getView() {
		return view;
	}

	public void setView(View view) {
		this.view = view;
	}

	
	
}
