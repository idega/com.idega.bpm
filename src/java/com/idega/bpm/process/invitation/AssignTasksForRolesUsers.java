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
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 *          Last modified: $Date: 2008/11/30 08:19:04 $ by $Author: civilis $
 */
@Service("assignTasksForRolesUsers")
@Scope("prototype")
public class AssignTasksForRolesUsers implements ActionHandler {

	private static final long serialVersionUID = -1873068611362644702L;
	private List<AssignTasksForRolesUsersBean> assignableTasks;
	@Autowired
	private BPMFactory bpmFactory;

	public void execute(ExecutionContext ectx) throws Exception {

		final List<AssignTasksForRolesUsersBean> tasksBeans = getAssignableTasks();

		if (tasksBeans != null) {

			RolesManager rolesManager = getBpmFactory().getRolesManager();

			for (AssignTasksForRolesUsersBean tb : tasksBeans) {

				if (tb.getRoles() != null && tb.getRoles().length != 0) {

					Task task = tb.getTask();

					List<String> rolesNames = Arrays.asList(tb.getRoles());
					ProcessInstance pi = tb.getToken().getProcessInstance();

					Collection<User> users = rolesManager.getAllUsersForRoles(
							rolesNames, pi.getId());

					if (users != null) {

						for (User user : users) {

							ExecutionContext newEctx = new ExecutionContext(tb
									.getToken());
							TaskInstance ti = newEctx.getTaskMgmtInstance()
									.createTaskInstance(task, newEctx);
							// creating task instance for each user, and
							// assigning
							ti.setStart(new Date());
							ti.setActorId(user.getPrimaryKey().toString());
						}
					}

				} else {
					Logger.getLogger(getClass().getName()).log(
							Level.WARNING,
							"AssignTasksForRolesUsersBean got, but roles were not set. Task="
									+ tb.getTask());
				}
			}
		}
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public List<AssignTasksForRolesUsersBean> getAssignableTasks() {
		return assignableTasks;
	}

	public void setAssignableTasks(
			List<AssignTasksForRolesUsersBean> assignableTasks) {
		this.assignableTasks = assignableTasks;
	}
}