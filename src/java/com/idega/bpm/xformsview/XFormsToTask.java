package com.idega.bpm.xformsview;

import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewToTask;
import com.idega.jbpm.view.ViewToTaskType;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/16 17:48:15 $ by $Author: civilis $
 */
@ViewToTaskType("xforms")
@Scope("singleton")
@Service("process_xforms_viewToTask")
public class XFormsToTask implements ViewToTask {
	
	private BPMDAO BPMDAO; 
	
	public Long getTask(String viewId) {
		
		ViewTaskBind vtb = getBPMDAO().getViewTaskBindByView(viewId, XFormsView.VIEW_TYPE);
		return vtb == null ? null : vtb.getTaskId();
	}
	
	public void unbind(String viewId) {
		
		ViewTaskBind vtb = getBPMDAO().getViewTaskBindByView(viewId, XFormsView.VIEW_TYPE);
		
		if(vtb != null)
			getBPMDAO().remove(vtb);
	}

	public void bind(View view, Task task) {

//		TODO: view type and task id should be a alternate key. that means unique too.
//		also catch when duplicate view type and task id pair is tried to be entered, and override
//		views could be versioned
		
		ViewTaskBind vtb = getBPMDAO().getViewTaskBind(task.getId(), XFormsView.VIEW_TYPE);
		
		boolean newVtb = false;
		
		if(vtb == null) {
			vtb = new ViewTaskBind();
			newVtb = true;
		}
		
		vtb.setTaskId(task.getId());
		vtb.setTaskInstanceId(null);
		vtb.setViewIdentifier(view.getViewId());
		vtb.setViewType(view.getViewType());

		if(newVtb)
			getBPMDAO().persist(vtb);
	}
	
	public BPMDAO getBPMDAO() {
		return BPMDAO;
	}

	@Autowired
	public void setBPMDAO(BPMDAO bpmdao) {
		BPMDAO = bpmdao;
	}

	public void bind(View view, TaskInstance taskInstance) {
		
		ViewTaskBind vtb = getBPMDAO().getViewTaskBindByTaskInstance(taskInstance.getId(), XFormsView.VIEW_TYPE);
		
		boolean newVtb = false;
		
		if(vtb == null) {
			vtb = new ViewTaskBind();
			newVtb = true;
		}
		
		vtb.setTaskInstanceId(taskInstance.getId());
		vtb.setTaskId(null);
		vtb.setViewIdentifier(view.getViewId());
		vtb.setViewType(view.getViewType());

		if(newVtb)
			getBPMDAO().persist(vtb);
	}
}