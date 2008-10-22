package com.idega.bpm.process.invitation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.user.data.User;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/10/22 14:51:54 $ by $Author: civilis $
 */
public class AssignTasksForRolesUsers implements ActionHandler {

	private static final long serialVersionUID = -1873068611362644702L;
	private String tasksExp;
	private BPMFactory bpmFactory;

	public void execute(ExecutionContext ectx) throws Exception {

		@SuppressWarnings("unchecked")
		final List<AssignTasksForRolesUsersBean> tasksBeans = (List<AssignTasksForRolesUsersBean>)JbpmExpressionEvaluator.evaluate(getTasksExp(), ectx);
		
		if(tasksBeans != null) {

			ELUtil.getInstance().autowire(this);
			
			RolesManager rolesManager = getBpmFactory().getRolesManager();
			
			for (AssignTasksForRolesUsersBean tb : tasksBeans) {
				
				if(tb.getRoles() != null && tb.getRoles().length != 0) {

					Task task = tb.getTask();
					
					List<String> rolesNames = Arrays.asList(tb.getRoles());
					ProcessInstance pi = tb.getToken().getProcessInstance();
					
					Collection<User> users = rolesManager.getAllUsersForRoles(rolesNames, pi.getId());
					
					if(users != null) {
					
						for (User user : users) {
							
							ExecutionContext newEctx = new ExecutionContext(tb.getToken());
							TaskInstance ti = newEctx.getTaskMgmtInstance().createTaskInstance(task, newEctx);
//							creating task instance for each user, and assigning
							ti.setStart(new Date());
							ti.setActorId(user.getPrimaryKey().toString());
						}
					}
					
				} else {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "AssignTasksForRolesUsersBean got, but roles were not set. Task="+tb.getTask());
				}
			}
		}
	}

	public String getTasksExp() {
		return tasksExp;
	}

	public void setTasksExp(String tasksExp) {
		this.tasksExp = tasksExp;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}