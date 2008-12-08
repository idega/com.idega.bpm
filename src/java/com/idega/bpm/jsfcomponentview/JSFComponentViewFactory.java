package com.idega.bpm.jsfcomponentview;

import java.util.Collection;

import org.apache.commons.lang.NotImplementedException;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Multimap;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.view.TaskView;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewFactory;
import com.idega.jbpm.view.ViewFactoryType;


@Scope("singleton")
@ViewFactoryType("jsf")
@Repository("process_jsfComponent_viewFactory")
public class JSFComponentViewFactory implements ViewFactory{

	
	private final String beanIdentifier = "process_jsfComponent_viewFactory";
	
	private BPMDAO BPMDAO;
	
	
	public Multimap<Long, TaskView> getAllViewsByProcessDefinitions(Collection<Long> processDefinitionsIds) {
		throw new NotImplementedException("Method getAllViewsByProcessDefinitions is not implemented yet");
		//return null;
	}

	public String getBeanIdentifier() {
		return beanIdentifier;
	}

	

	public View getView(String viewIdentifier, boolean submitable) {
		if(viewIdentifier == null || viewIdentifier.trim().equals("")){
			throw new NullPointerException("View identifier not provided");
		}
		View view = getJSFComponentView();
		view.setSubmitable(submitable);
		view.setViewId(viewIdentifier);
		return view;
	}
	
	public JSFComponentView getJSFComponentView(){
		JSFComponentView view = new JSFComponentView();
	
		return view;
	}

	public String getViewType() {
		
		return JSFComponentView.VIEW_TYPE;
	}

	public BPMDAO getBPMDAO() {
		return BPMDAO;
	}



	@Autowired
	public void setBPMDAO(BPMDAO bpmdao) {
		BPMDAO = bpmdao;
	}

	public TaskView getTaskView(Task task) {
		// TODO Auto-generated method stub
		return null;
	}

}
