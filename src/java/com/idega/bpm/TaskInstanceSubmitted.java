package com.idega.bpm;

import org.springframework.context.ApplicationEvent;

import com.idega.jbpm.exe.TaskInstanceW;

public class TaskInstanceSubmitted extends ApplicationEvent {

	private static final long serialVersionUID = 2972600186154204917L;

	private Long piId, tiId;

	public TaskInstanceSubmitted(TaskInstanceW source, Long piId, Long tiId) {
		super(source);

		this.piId = piId;
		this.tiId = tiId;
	}

	public Long getProcessInstanceId() {
		return piId;
	}

	public void setProcessInstanceId(Long piId) {
		this.piId = piId;
	}

	public Long getTaskInstanceId() {
		return tiId;
	}

	public void setTaskInstanceId(Long tiId) {
		this.tiId = tiId;
	}

	@Override
	public String toString() {
		return "Task instance (ID: " + getTaskInstanceId() + ") submitted for process instance: " + getProcessInstanceId();
	}
}