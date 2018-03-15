package com.idega.bpm.exe;

import java.io.Serializable;
import java.security.AccessControlException;
import java.security.Permission;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.Resource;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.business.CaseManagersProvider;
import com.idega.block.process.business.CasesRetrievalManager;
import com.idega.block.process.business.ExternalEntityInterface;
import com.idega.bpm.BPMConstants;
import com.idega.bpm.security.TaskPermissionManager;
import com.idega.core.business.DefaultSpringBean;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.Variable;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMDocument;
import com.idega.jbpm.exe.BPMEmailDocument;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.exe.TaskMgmtInstanceW;
import com.idega.jbpm.exe.impl.BPMDocumentImpl;
import com.idega.jbpm.exe.impl.BPMEmailDocumentImpl;
import com.idega.jbpm.identity.BPMAccessControlException;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.BPMTypedPermission;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.rights.Right;
import com.idega.jbpm.utils.JBPMUtil;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.user.util.UserComparator;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.37 $ Last modified: $Date: 2009/07/03 08:56:48 $ by $Author: valdas $
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Service("defaultPIW")
public class DefaultBPMProcessInstanceW extends DefaultSpringBean implements ProcessInstanceW {

	private Serializable processInstanceId;
	private ProcessInstance processInstance;

	@Autowired
	private BPMContext bpmContext;

	private ProcessManager processManager;

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private PermissionsFactory permissionsFactory;

	@Autowired
	private BPMDAO bpmDAO;

	@Autowired
	private VariablesHandler variablesHandler;

	@Autowired
	@Qualifier("default")
	private TaskMgmtInstanceW taskMgmtInstance;

	@Autowired
	private VariableInstanceQuerier querier;

	public static final String email_fetch_process_name = "fetchEmails";

	public static final String add_attachement_process_name = "addAttachments";

	public VariableInstanceQuerier getVariableInstanceQuerier() {
		if (querier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return querier;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaskInstanceW> getAllTaskInstances() {
		// TODO: hide tasks of ended subprocesses
		return wrapTaskInstances(getUnfilteredProcessTaskInstances());
	}

	Collection<TaskInstance> getUnfilteredProcessTaskInstances() {
		return getProcessTaskInstances(null, null);
	}

	/**
	 * gets task instances
	 *
	 * @param excludedSubProcessesNames
	 *            - task instances of subprocesses listed here are excluded
	 * @param includedOnlySubProcessesNames
	 *            - only task instances of subprocesses listed here are included
	 * @return
	 */
	@Transactional(readOnly = true)
	List<TaskInstance> getProcessTaskInstances(final List<String> excludedSubProcessesNames, final List<String> includedOnlySubProcessesNames) {
		return getBpmContext().execute(new JbpmCallback<List<TaskInstance>>() {

			@Override
			public List<TaskInstance> doInJbpm(JbpmContext context) throws JbpmException {
				ProcessInstance processInstance = getProcessInstance(context);

				final List<TaskInstance> taskInstances;
				if (includedOnlySubProcessesNames != null) {
					// only inserting task instances from subprocesses
					taskInstances = new ArrayList<TaskInstance>();
				} else {
					@SuppressWarnings("unchecked")
					Collection<TaskInstance> tasks = processInstance.getTaskMgmtInstance().getTaskInstances();
					if (ListUtil.isEmpty(tasks)) {
						taskInstances = new ArrayList<TaskInstance>();
						getLogger().warning("Where are no tasks for process instance " + processInstance.getId());
					} else {
						taskInstances = new ArrayList<TaskInstance>(tasks);
					}
				}

				taskInstances.addAll(getSubprocessesTaskInstances(context, processInstance, excludedSubProcessesNames, includedOnlySubProcessesNames));
				return taskInstances;
			}
		});
	}

	private boolean isFilterOutProcessInstance(String processName, final List<String> excludedSubProcessesNames, List<String> includedOnlySubProcessesNames) {
		return (includedOnlySubProcessesNames != null && !includedOnlySubProcessesNames.contains(processName)) ||
				(includedOnlySubProcessesNames == null && excludedSubProcessesNames != null && excludedSubProcessesNames.contains(processName));
	}

	@Override
	public List<Long> getIdsOfSubProcesses(final Long procInstId) {
		if (procInstId == null) {
			return null;
		}

		List<ProcessInstance> subProcesses = getAllSubprocesses(procInstId);
		if (ListUtil.isEmpty(subProcesses)) {
			return null;
		}

		List<Long> ids = new ArrayList<Long>();
		for (ProcessInstance subProcess: subProcesses) {
			ids.add(subProcess.getId());
		}
		return ids;
	}

	private List<ProcessInstance> getAllSubprocesses(ProcessInstance processInstance) {
		if (processInstance == null) {
			return null;
		}

		return getAllSubprocesses(processInstance.getId());
	}

	private List<ProcessInstance> getAllSubprocesses(Long procInstId) {
		List<ProcessInstance> subProcessInstances = getBpmDAO().getSubprocessInstancesOneLevel(procInstId);
		if(ListUtil.isEmpty(subProcessInstances)) {
			return Collections.emptyList();
		}
		List<ProcessInstance> childSubProcessInstances = new ArrayList<ProcessInstance>(subProcessInstances);
		for (ProcessInstance subProcess: subProcessInstances) {
			childSubProcessInstances.addAll(getAllSubprocesses(subProcess));
		}
		return childSubProcessInstances;
	}

	private Collection<TaskInstance> getSubprocessesTaskInstances(
			JbpmContext context,
			ProcessInstance processInstance,
			final List<String> excludedSubProcessesNames,
			final List<String> includedOnlySubProcessesNames
	) {
		List<ProcessInstance> subProcessInstances = getAllSubprocesses(processInstance);
		List<TaskInstance> taskInstances;
		if (!ListUtil.isEmpty(subProcessInstances)) {
			taskInstances = new ArrayList<TaskInstance>();
			for (ProcessInstance subProcessInstance: subProcessInstances) {
				subProcessInstance = context.getProcessInstance(subProcessInstance.getId());

				if (!isFilterOutProcessInstance(subProcessInstance.getProcessDefinition().getName(),
				    excludedSubProcessesNames, includedOnlySubProcessesNames)) {

					@SuppressWarnings("unchecked")
					Collection<TaskInstance> subTaskInstances = subProcessInstance.getTaskMgmtInstance().getTaskInstances();

					if (subTaskInstances != null) {
						taskInstances.addAll(subTaskInstances);
					}
				}
			}
		} else {
			taskInstances = Collections.emptyList();
		}

		return taskInstances;
	}

	@Transactional(readOnly = true)
	public List<TaskInstanceW> getSubmittedTaskInstances(List<String> excludedSubProcessesNames) {
		return getSubmittedTaskInstances(excludedSubProcessesNames, DefaultBPMTaskInstanceW.PRIORITY_HIDDEN, DefaultBPMTaskInstanceW.PRIORITY_VALID_HIDDEN);
	}

	List<TaskInstanceW> getSubmittedTaskInstances(List<String> excludedSubProcessesNames, Integer... prioritiesToFilter) {
		boolean measure = JBPMUtil.isPerformanceMeasurementOn();
		long start = measure ? System.currentTimeMillis() : 0;
		try {
			Collection<TaskInstance> taskInstances = getProcessTaskInstances(excludedSubProcessesNames, null);
			if (ListUtil.isEmpty(taskInstances)) {
				return wrapTaskInstances(taskInstances);
			}

			List<Integer> prioritiesToFilterList = Arrays.asList(prioritiesToFilter);

			for (Iterator<TaskInstance> iterator = taskInstances.iterator(); iterator.hasNext();) {
				TaskInstance ti = iterator.next();

				try {
					if (ti == null) {
						getLogger().warning("Task instance is null in a collection of task instances: " + taskInstances);
						iterator.remove();
					} else if (!ti.hasEnded() || prioritiesToFilterList.contains(ti.getPriority())) {
						// simply filtering out the not ended task instances
						iterator.remove();
					}
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error while getting submitted tasks for processes: " + excludedSubProcessesNames, e);
					iterator.remove();
				}
			}

			return wrapTaskInstances(taskInstances);
		} finally {
			if (measure) {
				getLogger().info("Got all submitted task instances in: " + (System.currentTimeMillis() - start + " ms"));
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaskInstanceW> getSubmittedTaskInstances() {
		return getSubmittedTaskInstances(new ArrayList<String>(0));
	}

	@Override
	public List<TaskInstanceW> getSubmittedTaskInstances(String taskInstanceName) {
		if (StringUtil.isEmpty(taskInstanceName)) {
			return null;
		}

		List<TaskInstanceW> allSubmittedTaskInstances = getSubmittedTaskInstances();
		if (ListUtil.isEmpty(allSubmittedTaskInstances)) {
			return null;
		}

		Map<Date, TaskInstanceW> tasksByName = new HashMap<Date, TaskInstanceW>();
		for (TaskInstanceW tiW: allSubmittedTaskInstances) {
			TaskInstance ti = tiW.getTaskInstance();
			if (taskInstanceName.equals(ti.getName())) {
				tasksByName.put(ti.getEnd(), tiW);
			}
		}

		if (MapUtil.isEmpty(tasksByName)) {
			return null;
		}

		List<TaskInstanceW> tasks = new ArrayList<TaskInstanceW>();
		List<Date> dates = new ArrayList<Date>(tasksByName.keySet());
		Collections.sort(dates);
		for (Date date: dates) {
			tasks.add(tasksByName.get(date));
		}
		return tasks;
	}

	@Override
	public TaskInstanceW getLastSubmittedTaskInstance(String taskInstanceName) {
		List<TaskInstanceW> submittedTaskInstances = getSubmittedTaskInstances(taskInstanceName);
		if (ListUtil.isEmpty(submittedTaskInstances)) {
			return null;
		}

		return submittedTaskInstances.get(submittedTaskInstances.size() - 1);
	}

	@Override
	public List<BPMDocument> getTaskDocumentsForUser(User user, Locale locale, boolean doShowExternalEntity) {
		return getTaskDocumentsForUser(user, locale, doShowExternalEntity, null);
	}
	@Override
	@Transactional(readOnly = true)
	public List<BPMDocument> getTaskDocumentsForUser(User user, Locale locale, boolean doShowExternalEntity, List<String> tasksNamesToReturn) {
		List<TaskInstanceW> unfinishedTaskInstances = getAllUnfinishedTaskInstances();

		unfinishedTaskInstances = filterTasksByUserPermission(user, unfinishedTaskInstances);

		return getBPMDocuments(unfinishedTaskInstances, locale, doShowExternalEntity, false, tasksNamesToReturn, BPMDocument.class);
	}

	//	TODO: improve performance!
	@Transactional(readOnly = true)
	private List<TaskInstanceW> filterTasksByUserPermission(User user, final List<TaskInstanceW> unfinishedTaskInstances) {
		boolean measure = JBPMUtil.isPerformanceMeasurementOn();
		long start = measure ? System.currentTimeMillis() : 0;
		try {
			final PermissionsFactory permissionsFactory = getBpmFactory().getPermissionsFactory();
			final RolesManager rolesManager = getBpmFactory().getRolesManager();

			final Map<String, TaskPermissionManager> tasksManagers = getBeansOfType(TaskPermissionManager.class);

			for (Iterator<TaskInstanceW> iterator = unfinishedTaskInstances.iterator(); iterator.hasNext();) {
				TaskInstanceW tiw = iterator.next();
				try {
					// Checks if task instance is eligible for viewing for user provided

					// TODO: add user into permission
					Permission permission = permissionsFactory.getTaskInstanceSubmitPermission(false, tiw.getTaskInstanceId());
					rolesManager.checkPermission(permission);

					//	Checking custom business logic
					if (!MapUtil.isEmpty(tasksManagers)) {
						Boolean visible = true;
						String procDefName = tiw.getProcessInstanceW().getProcessDefinitionW().getProcessDefinitionName();
						for (Iterator<TaskPermissionManager> tasksManagersIter = tasksManagers.values().iterator(); (visible && tasksManagersIter.hasNext());) {
							visible = tasksManagersIter.next().isTaskVisible(tiw, procDefName);
						}
						if (!visible) {
							iterator.remove();
						}
					}
				} catch (BPMAccessControlException e) {
					iterator.remove();
				}
			}

			return unfinishedTaskInstances;
		} finally {
			if (measure) {
				getLogger().info("Filtered out tasks (from unfinished task instances: " + unfinishedTaskInstances + ") by user's (" + user + ") permission in " + (System.currentTimeMillis() - start ) + " ms");
			}
		}
	}

	@Transactional(readOnly = true)
	private List<TaskInstanceW> filterDocumentsByUserPermission(User user, final List<TaskInstanceW> submittedTaskInstances) {
		boolean measure = JBPMUtil.isPerformanceMeasurementOn();
		long start = measure ? System.currentTimeMillis() : 0;
		try {
			final PermissionsFactory permissionsFactory = getBpmFactory().getPermissionsFactory();
			final RolesManager rolesManager = getBpmFactory().getRolesManager();

			for (Iterator<TaskInstanceW> iterator = submittedTaskInstances.iterator(); iterator.hasNext();) {
				TaskInstanceW tiw = iterator.next();
				long permissionCheckedIn = 0;
				try {
					// Check if task instance is eligible for viewing for user provided

					// TODO: add user into permission
					long permissionCreated = measure ? System.currentTimeMillis() : 0;
					Permission permission = permissionsFactory.getTaskInstanceViewPermission(true, tiw.getTaskInstanceId());
					if (measure) {
						getLogger().info("Permission for " + tiw + " created in " + (System.currentTimeMillis() - permissionCreated) + " ms");

						permissionCheckedIn = System.currentTimeMillis();
					}

					rolesManager.checkPermission(permission);
					if (measure) {
						getLogger().info("Permission for " + tiw + " checked in " + (System.currentTimeMillis() - permissionCheckedIn) + " ms");
					}
				} catch (BPMAccessControlException e) {
					iterator.remove();

					if (measure) {
						getLogger().info("Permission for " + tiw + " checked in " + (System.currentTimeMillis() - permissionCheckedIn) + " ms");
					}
				}
			}
			return submittedTaskInstances;
		} finally {
			if (measure) {
				getLogger().info("Filtered out submitted task instances (" + submittedTaskInstances + ") by user's permission in: " + (System.currentTimeMillis() - start + " ms"));
			}
		}
	}

	@Override
	public List<BPMDocument> getSubmittedDocumentsForUser(User user, Locale locale, boolean doShowExternalEntity, boolean checkIfSignable) {
		return getSubmittedDocumentsForUser(user, locale, doShowExternalEntity, checkIfSignable, null);
	}
	@Override
	public List<BPMDocument> getSubmittedDocumentsForUser(User user, Locale locale, boolean doShowExternalEntity, boolean checkIfSignable, List<String> tasksNamesToReturn) {
		return getSubmittedTasksForUser(user, locale, doShowExternalEntity, checkIfSignable, tasksNamesToReturn, BPMDocument.class);
	}

	@Override
	public List<TaskInstanceW> getSubmittedTasksForUser(User user, Locale locale, boolean doShowExternalEntity, boolean checkIfSignable, List<String> tasksNamesToReturn) {
		return getSubmittedTasksForUser(user, locale, doShowExternalEntity, checkIfSignable, tasksNamesToReturn, TaskInstanceW.class);
	}

	@Transactional(readOnly = true)
	private <T> List<T> getSubmittedTasksForUser(User user, Locale locale, boolean doShowExternalEntity, boolean checkIfSignable, List<String> tasksNamesToReturn, Class<T> resultType) {
		List<TaskInstanceW> submittedTaskInstances = getSubmittedTaskInstances(Arrays.asList(email_fetch_process_name));

		submittedTaskInstances = filterDocumentsByUserPermission(user, submittedTaskInstances);
		return getBPMDocuments(submittedTaskInstances, locale, doShowExternalEntity, checkIfSignable, tasksNamesToReturn, resultType);
	}

	@Autowired(required=false)
	private ExternalEntityInterface externalEntityInterface;

	protected ExternalEntityInterface getExternalEntityInterface() {
		if (this.externalEntityInterface == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.externalEntityInterface;
	}

	@Override
	public List<BPMDocument> getBPMDocuments(List<TaskInstanceW> tiWs, Locale locale) {
		return getBPMDocuments(tiWs, locale, false, false, null, BPMDocument.class);
	}

	private <T> List<T> getBPMDocuments(List<TaskInstanceW> tiws, Locale locale, boolean doShowExternalEntity, boolean checkIfSignable, List<String> tasksNamesToReturn, Class<T> resultType) {
		boolean measure = JBPMUtil.isPerformanceMeasurementOn();
		long start = measure ? System.currentTimeMillis() : 0;
		try {
			List<T> results = new ArrayList<>();

			boolean bpmDocument = resultType.getName().equals(BPMDocument.class.getName());
			boolean taskInstance = resultType.getName().equals(TaskInstanceW.class.getName());

			UserBusiness userBusiness = getServiceInstance(UserBusiness.class);

			getBpmContext().execute(new JbpmCallback<Void>() {

				@Override
				public Void doInJbpm(JbpmContext context) throws JbpmException {
					tiws.stream().forEach((tiw) -> {
						TaskInstance ti = tiw.getTaskInstance(context);
						String taskName = ti.getName();

						boolean canAdd = true;
						if (!ListUtil.isEmpty(tasksNamesToReturn)) {
							canAdd = tasksNamesToReturn.contains(taskName);
						}

						String name = tiw.getName(locale);
						if (StringUtil.isEmpty(name)) {
							canAdd = false;
						}

						if (canAdd) {
							if (bpmDocument) {
								// creating document representation
								BPMDocumentImpl bpmDoc = new BPMDocumentImpl();
								bpmDoc.setTaskName(taskName);

								// get submitted by
								String actorId = ti.getActorId();
								String actorName = null;

								if (actorId != null) {
									try {
										User usr = userBusiness.getUser(Integer.parseInt(actorId));

										if (!doShowExternalEntity) {
											actorName = usr.getName();
										} else {
											ExternalEntityInterface eei = getExternalEntityInterface();
											if (eei != null) {
												actorName = eei.getName(usr);
											}

											if (StringUtil.isEmpty(actorName)) {
												actorName = usr.getName();
											} else {
												actorName = actorName + " (" + usr.getName() + ")";
											}
										}

									} catch (Exception e) {
										getLogger().log(Level.SEVERE, "Exception while resolving actor name for actorId: " + actorId, e);
										actorName = CoreConstants.EMPTY;
									}
								} else {
									actorName = CoreConstants.EMPTY;
								}

								String submittedBy = null, assignedTo = null;

								if (ti.getEnd() == null) {
									// task
									submittedBy = CoreConstants.EMPTY;
									assignedTo = actorName;
								} else {
									// document
									submittedBy = actorName;
									assignedTo = CoreConstants.EMPTY;
								}

								bpmDoc.setTaskInstanceId(ti.getId());
								bpmDoc.setAssignedToName(assignedTo);
								bpmDoc.setSubmittedByName(submittedBy);
								bpmDoc.setDocumentName(name);

								bpmDoc.setCreateDate(ti.getCreate());
								bpmDoc.setEndDate(ti.getEnd());
								if (checkIfSignable) {
									bpmDoc.setSignable(tiw.isSignable());
								}

								bpmDoc.setOrder(tiw.getOrder());

								boolean hasView = tiw.hasViewForDisplay();
								bpmDoc.setHasViewUI(hasView);

								@SuppressWarnings("unchecked")
								T result = (T) bpmDoc;
								results.add(result);
							} else if (taskInstance) {
								@SuppressWarnings("unchecked")
								T result = (T) tiw;
								results.add(result);
							}
						}
					});
					return null;
				}

			});

			return results;
		} finally {
			if (measure) {
				getLogger().info("Got BPM docs for task instances: " + tiws + ", locale: " + locale + ", doShowExternalEntity: " + doShowExternalEntity +
						", checkIfSignable: " + checkIfSignable + " in " + (System.currentTimeMillis() - start) + " ms");
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaskInstanceW> getUnfinishedTaskInstances(Token rootToken) {
		ProcessInstance processInstance = rootToken.getProcessInstance();

		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getUnfinishedTasks(rootToken);

		return wrapTaskInstances(taskInstances);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaskInstanceW> getAllUnfinishedTaskInstances() {
		boolean measure = JBPMUtil.isPerformanceMeasurementOn();
		long start = measure ? System.currentTimeMillis() : 0;
		try {
			return getBpmContext().execute(new JbpmCallback<List<TaskInstanceW>>() {

				@Override
				public List<TaskInstanceW> doInJbpm(JbpmContext context) throws JbpmException {
					return getUnfinishedTaskInstancesForTask(
							context,
							null,
							DefaultBPMTaskInstanceW.PRIORITY_HIDDEN,
							DefaultBPMTaskInstanceW.PRIORITY_VALID_HIDDEN
					);
				}
			});
		} finally {
			if (measure) {
				getLogger().info("Got all unfinished task instances in: " + (System.currentTimeMillis() - start + " ms"));
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaskInstanceW> getUnfinishedTaskInstancesForTask(final String taskName) {
		return getBpmContext().execute(new JbpmCallback<List<TaskInstanceW>>() {

			@Override
			public List<TaskInstanceW> doInJbpm(JbpmContext context) throws JbpmException {
				return getUnfinishedTaskInstancesForTask(context, taskName, DefaultBPMTaskInstanceW.PRIORITY_HIDDEN);
			}
		});
	}

	List<TaskInstanceW> getUnfinishedTaskInstancesForTask(JbpmContext context, String taskName, Integer... prioritiesToFilter) {
		/*String query = "select ti.name, ti.id from " + TaskInstance.class.getName() + " ti where ti.processInstance.id = :piId and ti.end is null and ti.processInstance.end is null";
		List<Param> params = new ArrayList<Param>();
		params.add(new Param("piId", getProcessInstanceId()));

		if (!StringUtil.isEmpty(taskName)) {
			query += " and ti.task.name = :taskName";
			params.add(new Param("taskName", taskName));
		}
		if (!ArrayUtil.isEmpty(prioritiesToFilter)) {
			query += " and ti.priority not in (:priorities)";
			params.add(new Param("priorities", Arrays.asList(prioritiesToFilter)));
		}
		query += " order by ti.start desc";

		List<Object[]> data = getBpmDAO().getResultListByInlineQuery(
				query,
				Object[].class,
				ArrayUtil.convertListToArray(params)
		);
		if (ListUtil.isEmpty(data)) {
			return Collections.emptyList();
		}

		Map<String, Long> taskInstancesIds = new HashMap<String, Long>();
		for (Object[] entry: data) {
			String name = (String) entry[0];
			if (taskInstancesIds.containsKey(name)) {
				continue;
			}

			Number id = (Number) entry[1];
			taskInstancesIds.put(name, id.longValue());
		}

		return getTaskInstances(taskInstancesIds.values());*/

		Collection<TaskInstance> taskInstances = getUnfilteredProcessTaskInstances();
		if (ListUtil.isEmpty(taskInstances)) {
			return wrapTaskInstances(taskInstances);
		}

		Map<String, Long> taskInstancesIds = new HashMap<String, Long>();

		List<Integer> prioritiesToFilterList = Arrays.asList(prioritiesToFilter);
		boolean filterByTaskName = !StringUtil.isEmpty(taskName);
		for (Iterator<TaskInstance> iterator = taskInstances.iterator(); iterator.hasNext();) {
			TaskInstance ti = iterator.next();

			// removing hidden, ended task instances, and task instances of ended
			// processes (i.e. subprocesses), also leaving on task for taskName, if taskName
			// provided
			Long id = null;
			try {
				if (ti == null) {
					getLogger().warning("Task instance is null in a collection of task instances: " + taskInstances);
					iterator.remove();
				} else {
					id = ti.getId();
					ProcessInstance pi = ti.getProcessInstance();
					if (ti.hasEnded()
							|| prioritiesToFilterList.contains(ti.getPriority())
					        || (pi == null || pi.hasEnded())
					        || (filterByTaskName && !taskName.equals(context.getTaskInstance(id).getTask().getName()))) {
						iterator.remove();
					}
				}
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error while getting unfinished tasks for the task (name=" + taskName + "). Unable to resolve if a task (id=" + id +
						") is unfinished - removing it!", e);
				iterator.remove();
			}
		}

		for (TaskInstance ti: taskInstances) {
			if (ti == null) {
				continue;
			}
			String name = ti.getName();
			if (StringUtil.isEmpty(name)) {
				continue;
			}
			if (taskInstancesIds.containsKey(name)) {
				continue;
			}

			taskInstancesIds.put(name, ti.getId());
		}

		return getTaskInstances(taskInstancesIds.values());
	}

	@Override
	public TaskInstanceW getSingleUnfinishedTaskInstanceForTask(String taskName) {
		List<TaskInstanceW> tiws = getUnfinishedTaskInstancesForTask(taskName);

		if (ListUtil.isEmpty(tiws)) {
			return null;
		}

		TaskInstanceW tiw = null;
		if (tiws.size() > 1) {
			getLogger().info("More than one unfinished task instance resolved for task=" + taskName + " in the process=" + getProcessInstanceId());
		}
		tiw = tiws.iterator().next();

		return tiw;
	}

	private List<TaskInstanceW> wrapTaskInstances(Collection<TaskInstance> taskInstances) {
		if (ListUtil.isEmpty(taskInstances)) {
			return Collections.emptyList();
		}

		List<Long> ids = new ArrayList<Long>(taskInstances.size());
		for (TaskInstance instance : taskInstances) {
			ids.add(instance.getId());
		}

		return getTaskInstances(ids);
	}

	private List<TaskInstanceW> getTaskInstances(Collection<Long> taskInstancesIds) {
		if (ListUtil.isEmpty(taskInstancesIds)) {
			return Collections.emptyList();
		}

		List<TaskInstanceW> instances = new ArrayList<TaskInstanceW>(taskInstancesIds.size());
		for (Long id: taskInstancesIds) {
			TaskInstanceW tiw = getProcessManager().getTaskInstance(id);
			instances.add(tiw);
		}
		return instances;
	}

	@Override
	public <T extends Serializable> T getProcessInstanceId() {
		@SuppressWarnings("unchecked")
		T id = (T) processInstanceId;
		return id;
	}

	@Override
	public <T extends Serializable> void setProcessInstanceId(T processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public BPMContext getBpmContext() {
		return bpmContext;
	}

	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}

	public ProcessManager getProcessManager() {
		return processManager;
	}

	@Required
	@Resource(name = "defaultBpmProcessManager")
	public void setProcessManager(ProcessManager processManager) {
		this.processManager = processManager;
	}

	@Override
	@Transactional(readOnly = true)
	public <T> T getProcessInstance() {
		processInstance = getBpmContext().execute(new JbpmCallback<ProcessInstance>() {
			@Override
			public ProcessInstance doInJbpm(JbpmContext context) throws JbpmException {
				return getProcessInstance(context);
			}
		});
		@SuppressWarnings("unchecked")
		T instance = (T) processInstance;
		return instance;
	}

	@Override
	@Transactional(readOnly = true)
	public <T> T getProcessInstance(JbpmContext context) {
		Serializable id = getProcessInstanceId();
		processInstance = id instanceof Number ? context.getProcessInstance(((Number) id).longValue()) : null;
		@SuppressWarnings("unchecked")
		T instance = (T) processInstance;
		return instance;
	}

	@Override
	public void assignHandler(Integer handlerUserId) {
	}

	@Override
	public String getProcessDescription() {
		return null;
	}

	@Override
	public String getProcessIdentifier() {
		return null;
	}

	@Override
	@Transactional(readOnly = true)
	public ProcessDefinitionW getProcessDefinitionW() {
		return getBpmContext().execute(new JbpmCallback<ProcessDefinitionW>() {

			@Override
			public ProcessDefinitionW doInJbpm(JbpmContext context) throws JbpmException {
				return getProcessDefinitionW(context);
			}
		});
	}

	@Override
	@Transactional(readOnly = true)
	public ProcessDefinitionW getProcessDefinitionW(JbpmContext context) {
		ProcessInstance pi = getProcessInstance(context);
		ProcessDefinition pd = pi == null ? null : pi.getProcessDefinition();
		if (pd == null) {
			return null;
		}
		Long pdId = pd.getId();
		return getProcessManager().getProcessDefinition(pdId);
	}

	@Override
	public Integer getHandlerId() {
		return null;
	}

	private List<User> usersConnectedToProcess = null;

	@Override
	@Transactional(readOnly = true)
	public List<User> getUsersConnectedToProcess() {
		if (usersConnectedToProcess != null) {
			return usersConnectedToProcess;
		}

		try {
			String procDefName = getProcessDefinitionW().getProcessDefinitionName();
			usersConnectedToProcess = getBpmContext().execute(new JbpmCallback<List<User>>() {

				@Override
				public List<User> doInJbpm(JbpmContext context) throws JbpmException {
					Long piId = getProcessInstanceId();
					ProcessInstance pi = context.getProcessInstance(piId);
					@SuppressWarnings("unchecked")
					Map<String, Object> variables = pi.getContextInstance().getVariables();
					return getBpmFactory().getBPMDAO().getUsersConnectedToProcess(piId, procDefName, variables);
				}

			});
		} catch (Exception e) {}
		if (usersConnectedToProcess != null) {
			return usersConnectedToProcess;
		}

		final Collection<User> users;
		try {
			Long processInstanceId = getProcessInstanceId();
			BPMTypedPermission perm = (BPMTypedPermission) getBpmFactory().getPermissionsFactory().getRoleAccessPermission(processInstanceId, null, false);
			users = getBpmFactory().getRolesManager().getAllUsersForRoles(null, processInstanceId, perm);
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Exception while resolving all process instance users", e);
			return null;
		}

		if (ListUtil.isEmpty(users)) {
			usersConnectedToProcess = new ArrayList<User>();
			return usersConnectedToProcess;
		}

		// using separate list, as the resolved one could be cashed (shared) and so
		usersConnectedToProcess = new ArrayList<User>(users);
		for (Iterator<User> iterator = usersConnectedToProcess.iterator(); iterator.hasNext();) {
			User user = iterator.next();
			String hideInContacts = user.getMetaData(BPMUser.HIDE_IN_CONTACTS);

			if (hideInContacts != null) {
				// excluding ones, that should be hidden in contacts list
				iterator.remove();
			}
		}

		try {
			Collections.sort(usersConnectedToProcess, new UserComparator(getCurrentLocale()));
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Exception while sorting contacts list (" + usersConnectedToProcess + ")", e);
		}

		return usersConnectedToProcess;
	}

	@Override
	public boolean hasHandlerAssignmentSupport() {
		return false;
	}

	@Override
	public void setContactsPermission(Role role, Integer userId) {
		Long processInstanceId = getProcessInstanceId();
		getBpmFactory().getRolesManager().setContactsPermission(role, processInstanceId, userId);
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	@Override
	public ProcessWatch getProcessWatcher() {
		return null;
	}

	/**
	 * checks right for process instance and current logged in user
	 *
	 * @param right
	 * @return
	 */
	@Override
	public boolean hasRight(Right right) {
		return hasRight(right, null);
	}

	/**
	 * checks right for process instance and user provided
	 *
	 * @param right
	 * @param user
	 *            to check right against
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public boolean hasRight(Right right, User user) {
		switch (right) {
			case processHandler:
				return hasPermission(Access.caseHandler, user);

			case commentsViewer:
				return hasPermission(Access.seeComments, user) || hasPermission(Access.writeComments, user);

			default:
				throw new IllegalArgumentException("Right type " + right + " not supported for cases process instance");
		}
	}

	private boolean hasPermission(Access access, User user) {
		try {
			Permission perm = getBpmFactory().getPermissionsFactory().getAccessPermission(getProcessInstanceId(), access, user);
			getBpmFactory().getRolesManager().checkPermission(perm);
			return true;
		} catch (AccessControlException e) {
			return false;
		}
	}

	protected BPMDAO getBpmDAO() {
		return bpmDAO;
	}

	@Override
	public List<BPMEmailDocument> getAttachedEmails(User user) {
		return getAttachedEmails(user, false);
	}

	@Override
	@Transactional(readOnly = true)
	public List<BPMEmailDocument> getAttachedEmails(User user, boolean fetchMessage) {
		List<String> included = new ArrayList<String>(1);
		included.add(email_fetch_process_name);
		List<TaskInstance> emailsTaskInstances = getProcessTaskInstances(null, included);

		emailsTaskInstances = filterEmailsTaskInstances(emailsTaskInstances);

		List<BPMEmailDocument> bpmEmailDocs = new ArrayList<BPMEmailDocument>(emailsTaskInstances.size());
		for (TaskInstance emailTaskInstance: emailsTaskInstances) {
			Map<String, Object> vars = getVariablesHandler().populateVariables(emailTaskInstance.getId());

			String subject = (String) vars.get(BPMConstants.VAR_SUBJECT);
			String text = null;
			if (fetchMessage) {
				text = (String) vars.get(BPMConstants.VAR_TEXT);
			}
			String fromPersonal = (String) vars.get(BPMConstants.VAR_FROM);
			String fromAddress = (String) vars.get(BPMConstants.VAR_FROM_ADDRESS);

			BPMEmailDocument bpmEmailDocument = new BPMEmailDocumentImpl();
			bpmEmailDocument.setTaskInstanceId(emailTaskInstance.getId());
			bpmEmailDocument.setSubject(subject);
			bpmEmailDocument.setMessage(text);
			bpmEmailDocument.setFromAddress(fromAddress);
			bpmEmailDocument.setFromPersonal(fromPersonal);
			bpmEmailDocument.setEndDate(emailTaskInstance.getEnd());
			bpmEmailDocument.setDocumentName(emailTaskInstance.getName());
			bpmEmailDocument.setCreateDate(emailTaskInstance.getCreate());
			bpmEmailDocs.add(bpmEmailDocument);
		}

		return bpmEmailDocs;
	}

	List<TaskInstance> filterEmailsTaskInstances(List<TaskInstance> emailsTaskInstances) {
		for (Iterator<TaskInstance> iterator = emailsTaskInstances.iterator(); iterator.hasNext();) {
			TaskInstance taskInstance = iterator.next();

			try {
				if (taskInstance == null) {
					iterator.remove();
				} else if (!taskInstance.hasEnded()) {
					iterator.remove();
				} else {
					try {
						Permission permission = getPermissionsFactory().getTaskInstanceViewPermission(true, taskInstance.getId());
						getBpmFactory().getRolesManager().checkPermission(permission);
					} catch (BPMAccessControlException e) {
						iterator.remove();
					}
				}
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting emails from the list of task instances: (" +
						(emailsTaskInstances.size() > 10 ? emailsTaskInstances.subList(0, 9) : emailsTaskInstances) + "). Total number of tasks: " + emailsTaskInstances.size(), e);
				iterator.remove();
			}
		}

		return emailsTaskInstances;
	}

	/**
	 * @return all attachments that where attached to process(including subprocesses), and that are
	 *         not hidden (binVar.getHidden() == false)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<BinaryVariable> getAttachments() {
		List<TaskInstanceW> taskInstances = getAllTaskInstances();
		List<BinaryVariable> attachments = new ArrayList<BinaryVariable>();

		for (Iterator<TaskInstanceW> iterator = taskInstances.iterator(); iterator.hasNext();) {
			attachments.addAll(iterator.next().getAttachments());
		}

		return attachments;
	}

	@Override
	public <T extends Serializable> T getIdOfStartTaskInstance() {
		return getBpmFactory().getIdOfStartTaskInstance(getProcessInstanceId());
	}

	@Override
	@Transactional(readOnly = true)
	public TaskInstanceW getStartTaskInstance() {
		Long id = getIdOfStartTaskInstance();
		if (id == null) {
			return null;
		}

		return getBpmFactory().getTaskInstanceW(id);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasEnded() {
		ProcessInstance pi = getProcessInstance();
		return pi == null ? false : pi.hasEnded();
	}

	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	@Override
	public Collection<Role> getRolesContactsPermissions(Integer userId) {
		Collection<Role> roles = getBpmFactory().getRolesManager().getUserPermissionsForRolesContacts(getProcessInstanceId(), userId);
		return roles;
	}

	private Object getVariableValue(String variableName, Long piId) {
		try {
			List<Variable> variables = getBpmDAO().getVariablesByNameAndProcessInstance(variableName, piId);
			Object value = null;
			for (Iterator<Variable> variablesIter = variables.iterator(); (variablesIter.hasNext() && value == null);) {
				Variable variable = variablesIter.next();
				if (variable == null) {
					continue;
				}

				value = variable.getValue();
			}
			return value;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting variable " + variableName + " for proc. instance " + piId, e);
		}
		return null;
	}

	@Override
	public Object getVariableLocally(final String variableName, Token token) {
		if (token == null) {
			ProcessInstance pi = getProcessInstance();
			token = pi == null ? null : pi.getRootToken();
		}

		ProcessInstance pi = token.getProcessInstance();
		ContextInstance contextInstnace = pi.getContextInstance();

		Object val = contextInstnace.getVariableLocally(variableName, token);

		if (val == null || StringUtil.isEmpty(val.toString())) {
			getLogger().warning("Failed to find variable '" + variableName + "' from context instance " + contextInstnace + ", will load from proc. instance " + pi);
			val = getVariableValue(variableName, pi.getId());
			if (val == null) {
				getLogger().warning("Failed to find variable '" + variableName + "' for proc. inst.: " + pi.getId());
			} else {
				getLogger().info("Found value " + val + " for variable " + variableName + " and proc. inst.: " + pi.getId());
			}
		}

		if (val instanceof Date && getSettings().getBoolean("bpm.format_local_data_var", false)) {
			IWTimestamp temp = new IWTimestamp((Date) val);
			val = temp.getLocaleDateAndTime(getCurrentLocale(), DateFormat.SHORT, DateFormat.SHORT);
		}

		return val;
	}

	@Override
	public TaskMgmtInstanceW getTaskMgmtInstance() {
		return taskMgmtInstance.init(this);
	}

	@Override
	public User getOwner() {
		//	TODO: add implementation for none case based processes
		CaseManagersProvider managersProvider = ELUtil.getInstance().getBean(CaseManagersProvider.beanIdentifier);
		if (managersProvider == null) {
			return null;
		}

		List<CasesRetrievalManager> retrievalManagers = managersProvider.getCaseManagers();
		if (ListUtil.isEmpty(retrievalManagers)) {
			return null;
		}

		Long id = getProcessInstanceId();

		User owner = null;
		for (Iterator<CasesRetrievalManager> retrievalManagersIter = retrievalManagers.iterator(); (retrievalManagersIter.hasNext() && owner == null);) {
			owner = retrievalManagersIter.next().getCaseOwner(id);
		}

		return owner;
	}

	@Override
	public boolean doSubmitTask(String taskName, Map<String, Object> variables) {
		TaskInstanceW tiW = getSubmittedTaskInstance(taskName, variables);
		return tiW == null ? Boolean.FALSE : Boolean.TRUE;
	}

	@Override
	public TaskInstanceW getSubmittedTaskInstance(String taskName, Map<String, Object> variables) {
		if (StringUtil.isEmpty(taskName)) {
			getLogger().warning("Task name is not provided");
			return null;
		}

		TaskInstanceW taskInstanceW = getSingleUnfinishedTaskInstanceForTask(taskName);
		if (taskInstanceW == null) {
			getLogger().warning("Unable to find unfinished task " + taskName + " for process instance ID: " + getProcessInstanceId());
			return null;
		}

		return getSubmitedTask(taskInstanceW, null, variables);
	}

	@Override
	@Transactional(readOnly = false)
	public TaskInstanceW getSubmitedTask(TaskInstanceW task, ViewSubmission viewSubmission, Map<String, Object> variables) {
		if (task == null) {
			getLogger().warning("Task instance is not provided");
			return null;
		}

		try {
			View view = task.loadView();
			viewSubmission = viewSubmission == null ? getBpmFactory().getViewSubmission() : viewSubmission;

			Map<String, Object> currentVariables = view.resolveVariables();
			if (currentVariables == null) {
				currentVariables = variables;
			} else if (variables != null) {
				for (Map.Entry<String, Object> entry: variables.entrySet()) {
					currentVariables.put(entry.getKey(), entry.getValue());
				}
			}

			viewSubmission.populateParameters(view.resolveParameters());
			viewSubmission.populateVariables(currentVariables);

			Long viewTaskInstanceId = view.getTaskInstanceId();
			TaskInstanceW viewTIW = getBpmFactory().getProcessManagerByTaskInstanceId(viewTaskInstanceId).getTaskInstance(viewTaskInstanceId);
			viewTIW.submit(viewSubmission);

			return viewTIW;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error submiting task '" + task.getTaskInstanceName() + "' for process instance: " + getProcessInstanceId(), e);
		}

		return null;
	}

	@Override
	public List<BPMDocument> getSubmittedDocumentsForUser(User user, Locale locale) {
		return getSubmittedDocumentsForUser(user, locale, false, false);
	}

	@Override
	public List<BPMDocument> getTaskDocumentsForUser(User user, Locale locale) {
		return getTaskDocumentsForUser(user, locale, false);
	}

	@Override
	public TaskInstanceW getTaskInstance(String taskName) {
		if (StringUtil.isEmpty(taskName)) {
			getLogger().warning("Task name is not provided");
			return null;
		}

		List<TaskInstanceW> tasks = getAllTaskInstances();
		if (ListUtil.isEmpty(tasks)) {
			getLogger().warning("Proc. ins. " + getProcessInstanceId() + " does not have any tasks");
			return null;
		}

		for (TaskInstanceW task: tasks) {
			if (taskName.equals(task.getTaskInstanceName())) {
				return task;
			}
		}

		getLogger().warning("Task " + taskName + " can not be found for proc. inst. " + getProcessInstanceId() + " and it's tasks: " + tasks);
		return null;
	}

	@Override
	public String toString() {
		return "Proc. inst. ID: " + getProcessInstanceId();
	}

	@Override
	public Object getValueForTaskInstance(String taskInstanceName, String variable) {
		List<TaskInstanceW> submittedTiWs = getSubmittedTaskInstances(taskInstanceName);
		if (ListUtil.isEmpty(submittedTiWs)) {
			getLogger().info("Task with name " + taskInstanceName + " was not submitted yet. Can not get value for " + variable);
			return null;
		}

		Map<String, Object> values = getLatestValues(submittedTiWs, Arrays.asList(variable), submittedTiWs.size() - 1, null);
		return MapUtil.isEmpty(values) ? null : values.get(values);
	}

	@Override
	public Object getValueForTaskInstance(List<TaskInstanceW> submittedTiWs, String variable) {
		if (ListUtil.isEmpty(submittedTiWs)) {
			return null;
		}

		Map<String, Object> values = getLatestValues(submittedTiWs, Arrays.asList(variable), submittedTiWs.size() - 1, null);
		return MapUtil.isEmpty(values) ? null : values.get(values);
	}

	@Override
	public <T> Map<String, T> getValuesForTaskInstance(String taskInstanceName, List<String> variables) {
		List<TaskInstanceW> submittedTiWs = getSubmittedTaskInstances(taskInstanceName);
		if (ListUtil.isEmpty(submittedTiWs)) {
			getLogger().info("Task with name " + taskInstanceName + " was not submitted yet. Can not get values for " + variables);
			return null;
		}

		return getLatestValues(submittedTiWs, variables, submittedTiWs.size() - 1, null);
	}

	private <T> Map<String, T> getLatestValues(List<TaskInstanceW> submittedTiWs, List<String> variables, int index, Map<String, T> results) {
		if (ListUtil.isEmpty(variables) || ListUtil.isEmpty(submittedTiWs) || index < 0 || index >= submittedTiWs.size()) {
			return results;
		}

		if (results == null) {
			results = new HashMap<String, T>();
		}

		List<String> loaded = new ArrayList<String>();
		TaskInstanceW tiW = submittedTiWs.get(index);
		for (String variable: variables) {
			Object value = tiW.getVariable(variable);
			if (value != null) {
				@SuppressWarnings("unchecked")
				T latestValue = (T) value;
				results.put(variable, latestValue);
				loaded.add(variable);
			}
		}
		if (!ListUtil.isEmpty(loaded)) {
			variables = new ArrayList<String>(variables);
			variables.removeAll(loaded);
		}

		index--;
		return getLatestValues(submittedTiWs, variables, index, results);
	}

	@Override
	public String getProcessOwner() {
		return null;
	}

	@Override
	public List<ProcessInstance> getSubProcesses() {
		Long procInstId = getProcessInstanceId();
		return getAllSubprocesses(procInstId);
	}

}