package com.idega.bpm.process.invitation;

import java.io.Serializable;

import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.Task;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/10/22 14:51:54 $ by $Author: civilis $
 */
public class AssignTasksForRolesUsersBean implements Serializable {

	private static final long serialVersionUID = -9051548646937182970L;
	private Task task;
	private Token token;
	private String[] roles;
	
	public Task getTask() {
		return task;
	}
	public void setTask(Task task) {
		this.task = task;
	}
	public String[] getRoles() {
		return roles;
	}
	public void setRoles(String[] roles) {
		this.roles = roles;
	}
	public Token getToken() {
		return token;
	}
	public void setToken(Token token) {
		this.token = token;
	}
}