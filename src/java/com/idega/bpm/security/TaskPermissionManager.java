package com.idega.bpm.security;

import com.idega.jbpm.exe.TaskInstanceW;

public interface TaskPermissionManager {

	public boolean isTaskVisible(TaskInstanceW task, String procDefName);

	public boolean canSubmitTask(Long tiId, String procDefName);

}