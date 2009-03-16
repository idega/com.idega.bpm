package com.idega.bpm.exe;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMDocument;
import com.idega.jbpm.exe.BPMEmailDocument;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.TaskInstanceW;
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
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.user.util.UserComparator;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.24 $ Last modified: $Date: 2009/03/16 10:58:13 $ by $Author: juozas $
 */
@Scope("prototype")
@Service("defaultPIW")
public class DefaultBPMProcessInstanceW implements ProcessInstanceW {
	
	private Long processInstanceId;
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
	
	public static final String email_fetch_process_name = "fetchEmails";
	
	public static final String add_attachement_process_name = "addAttachments";
	
	// private static final String CASHED_TASK_NAMES =
	// "defaultBPM_taskinstance_names";
	
	@Transactional(readOnly = true)
	public List<TaskInstanceW> getAllTaskInstances() {
		
		// TODO: hide tasks of ended subprocesses
		
		return encapsulateInstances(getAllTaskInstancesPRVT());
	}
	
	Collection<TaskInstance> getAllTaskInstancesPRVT() {
		return getAllTaskInstancesPRVT(null, null);
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
	Collection<TaskInstance> getAllTaskInstancesPRVT(
	        final List<String> excludedSubProcessesNames,
	        final List<String> includedOnlySubProcessesNames) {
		
		return getBpmContext().execute(new JbpmCallback() {
			
			@SuppressWarnings("unchecked")
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				
				ProcessInstance processInstance = getProcessInstance();
				
				List<ProcessInstance> subProcessInstances = getBpmDAO()
				        .getSubprocessInstancesOneLevel(processInstance.getId());
				
				final Collection<TaskInstance> taskInstances;
				
				if (includedOnlySubProcessesNames != null) {
					
					// only inserting task instances from subprocesses
					taskInstances = new ArrayList<TaskInstance>();
				} else {
					
					taskInstances = processInstance.getTaskMgmtInstance()
					        .getTaskInstances();
					
				}
				
				if (!subProcessInstances.isEmpty()) {
					
					for (ProcessInstance subProcessInstance : subProcessInstances) {
						
						if ((includedOnlySubProcessesNames != null && !includedOnlySubProcessesNames
						        .contains(subProcessInstance
						                .getProcessDefinition().getName()))
						        || (includedOnlySubProcessesNames == null
						                && excludedSubProcessesNames != null && excludedSubProcessesNames
						                .contains(subProcessInstance
						                        .getProcessDefinition()
						                        .getName()))) {
							
							continue;
						}
						
						Collection<TaskInstance> subTaskInstances = subProcessInstance
						        .getTaskMgmtInstance().getTaskInstances();
						
						if(subTaskInstances != null)
							taskInstances.addAll(subTaskInstances);
					}
				}
				
				return taskInstances;
			}
		});
	}
	
	@Transactional(readOnly = true)
	public List<TaskInstanceW> getSubmittedTaskInstances(
	        List<String> excludedSubProcessesNames) {
		
		Collection<TaskInstance> taskInstances = getAllTaskInstancesPRVT(
		    excludedSubProcessesNames, null);
		
		for (Iterator<TaskInstance> iterator = taskInstances.iterator(); iterator
		        .hasNext();) {
			TaskInstance taskInstance = iterator.next();
			
			if (!taskInstance.hasEnded())
				iterator.remove();
		}
		
		return encapsulateInstances(taskInstances);
	}
	
	@Transactional(readOnly = true)
	public List<TaskInstanceW> getSubmittedTaskInstances() {
		return getSubmittedTaskInstances(null);
	}
	
	@Transactional(readOnly = true)
	public List<BPMDocument> getTaskDocumentsForUser(User user, Locale locale) {
		
		List<TaskInstanceW> unfinishedTaskInstances = getAllUnfinishedTaskInstances();
		
		PermissionsFactory permissionsFactory = getBpmFactory()
		        .getPermissionsFactory();
		RolesManager rolesManager = getBpmFactory().getRolesManager();
		
		for (Iterator<TaskInstanceW> iterator = unfinishedTaskInstances
		        .iterator(); iterator.hasNext();) {
			
			TaskInstanceW tiw = iterator.next();
			TaskInstance ti = tiw.getTaskInstance();
			
			try {
				// check if task instance is eligible for viewing for user
				// provided
				
				// TODO: add user into permission
				Permission permission = permissionsFactory
				        .getTaskInstanceSubmitPermission(false, ti);
				rolesManager.checkPermission(permission);
				
			} catch (BPMAccessControlException e) {
				iterator.remove();
			}
		}
		
		return getBPMDocuments(unfinishedTaskInstances, locale);
	}
	
	@Transactional(readOnly = true)
	public List<BPMDocument> getSubmittedDocumentsForUser(User user,
	        Locale locale) {
		
		List<TaskInstanceW> submittedTaskInstances = getSubmittedTaskInstances(Arrays
		        .asList(email_fetch_process_name));
		
		PermissionsFactory permissionsFactory = getBpmFactory()
		        .getPermissionsFactory();
		RolesManager rolesManager = getBpmFactory().getRolesManager();
		
		for (Iterator<TaskInstanceW> iterator = submittedTaskInstances
		        .iterator(); iterator.hasNext();) {
			TaskInstanceW tiw = iterator.next();
			TaskInstance ti = tiw.getTaskInstance();
			
			try {
				// check if task instance is eligible for viewing for user
				// provided
				
				// TODO: add user into permission
				Permission permission = permissionsFactory
				        .getTaskInstanceViewPermission(true, ti);
				rolesManager.checkPermission(permission);
				
			} catch (BPMAccessControlException e) {
				iterator.remove();
			}
		}
		
		return getBPMDocuments(submittedTaskInstances, locale);
	}
	
	private List<BPMDocument> getBPMDocuments(List<TaskInstanceW> tiws,
	        Locale locale) {
		
		ArrayList<BPMDocument> documents = new ArrayList<BPMDocument>(tiws
		        .size());
		
		UserBusiness userBusiness = getUserBusiness();
		
		for (TaskInstanceW tiw : tiws) {
			TaskInstance ti = tiw.getTaskInstance();
			
			// creating document representation
			BPMDocumentImpl bpmDoc = new BPMDocumentImpl();
			
			// get submitted by
			String actorId = ti.getActorId();
			String actorName;
			
			if (actorId != null) {
				
				try {
					User usr = userBusiness.getUser(Integer.parseInt(actorId));
					actorName = usr.getName();
					
				} catch (Exception e) {
					Logger.getLogger(getClass().getName()).log(
					    Level.SEVERE,
					    "Exception while resolving actor name for actorId: "
					            + actorId, e);
					actorName = CoreConstants.EMPTY;
				}
				
			} else
				actorName = CoreConstants.EMPTY;
			
			String submittedBy;
			String assignedTo;
			
			if (ti.getEnd() == null) {
				// task
				submittedBy = CoreConstants.EMPTY;
				assignedTo = actorName;
				
			} else {
				// document
				submittedBy = actorName;
				assignedTo = CoreConstants.EMPTY;
			}
			
			// string representation of end date, if any
			
			bpmDoc.setTaskInstanceId(ti.getId());
			bpmDoc.setAssignedToName(assignedTo);
			bpmDoc.setSubmittedByName(submittedBy);
			bpmDoc.setDocumentName(tiw.getName(locale));
			bpmDoc.setCreateDate(ti.getCreate());
			bpmDoc.setEndDate(ti.getEnd());
			bpmDoc.setSignable(tiw.isSignable());
			
			documents.add(bpmDoc);
		}
		
		return documents;
	}
	
	@Transactional(readOnly = true)
	public List<TaskInstanceW> getUnfinishedTaskInstances(Token rootToken) {
		
		ProcessInstance processInstance = rootToken.getProcessInstance();
		
		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = processInstance
		        .getTaskMgmtInstance().getUnfinishedTasks(rootToken);
		
		return encapsulateInstances(taskInstances);
	}
	
	@Transactional(readOnly = true)
	public List<TaskInstanceW> getAllUnfinishedTaskInstances() {
		
		Collection<TaskInstance> taskInstances = getAllTaskInstancesPRVT();
		
		// removing hidden, ended task instances, and task insances of ended
		// processes (i.e. subprocesses)
		for (Iterator<TaskInstance> iterator = taskInstances.iterator(); iterator
		        .hasNext();) {
			TaskInstance ti = iterator.next();
			
			if (ti.hasEnded()
			        || ti.getPriority() == DefaultBPMTaskInstanceW.PRIORITY_HIDDEN
			        || ti.getProcessInstance().hasEnded())
				iterator.remove();
		}
		
		return encapsulateInstances(taskInstances);
	}
	
	private ArrayList<TaskInstanceW> encapsulateInstances(
	        Collection<TaskInstance> taskInstances) {
		ArrayList<TaskInstanceW> instances = new ArrayList<TaskInstanceW>(
		        taskInstances.size());
		
		for (TaskInstance instance : taskInstances) {
			TaskInstanceW tiw = getProcessManager().getTaskInstance(
			    instance.getId());
			instances.add(tiw);
		}
		
		return instances;
	}
	
	public Long getProcessInstanceId() {
		return processInstanceId;
	}
	
	public void setProcessInstanceId(Long processInstanceId) {
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
	
	@Transactional(readOnly = true)
	public ProcessInstance getProcessInstance() {
		
		if (true || (processInstance == null && getProcessInstanceId() != null)) {
			
			processInstance = getBpmContext().execute(new JbpmCallback() {
				
				public Object doInJbpm(JbpmContext context)
				        throws JbpmException {
					return context.getProcessInstance(getProcessInstanceId());
				}
			});
			
		} else if (processInstance != null) {
			processInstance = getBpmContext().execute(new JbpmCallback() {
				
				public Object doInJbpm(JbpmContext context)
				        throws JbpmException {
					
					return getBpmContext().mergeProcessEntity(processInstance);
				}
			});
		}
		return processInstance;
	}
	
	public void assignHandler(Integer handlerUserId) {
	}
	
	public String getProcessDescription() {
		
		return null;
	}
	
	public String getProcessIdentifier() {
		
		return null;
	}
	
	@Transactional(readOnly = true)
	public ProcessDefinitionW getProcessDefinitionW() {
		
		Long pdId = getProcessInstance().getProcessDefinition().getId();
		return getProcessManager().getProcessDefinition(pdId);
	}
	
	public Integer getHandlerId() {
		
		return null;
	}
	
	@Transactional(readOnly = true)
	public List<User> getUsersConnectedToProcess() {
		
		final Collection<User> users;
		
		try {
			Long processInstanceId = getProcessInstanceId();
			BPMTypedPermission perm = (BPMTypedPermission) getBpmFactory()
			        .getPermissionsFactory().getRoleAccessPermission(
			            processInstanceId, null, false);
			users = getBpmFactory().getRolesManager().getAllUsersForRoles(null,
			    processInstanceId, perm);
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
			    "Exception while resolving all process instance users", e);
			return null;
		}
		
		if (users != null && !users.isEmpty()) {
			
			// using separate list, as the resolved one could be cashed (shared)
			// and so
			ArrayList<User> connectedPeople = new ArrayList<User>(users);
			
			for (Iterator<User> iterator = connectedPeople.iterator(); iterator
			        .hasNext();) {
				
				User user = iterator.next();
				String hideInContacts = user
				        .getMetaData(BPMUser.HIDE_IN_CONTACTS);
				
				if (hideInContacts != null)
					// excluding ones, that should be hidden in contacts list
					iterator.remove();
			}
			
			try {
				Collections.sort(connectedPeople, new UserComparator(CoreUtil
				        .getIWContext().getCurrentLocale()));
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(
				    Level.SEVERE,
				    "Exception while sorting contacts list (" + connectedPeople
				            + ")", e);
			}
			
			return connectedPeople;
		}
		
		return null;
	}
	
	public boolean hasHandlerAssignmentSupport() {
		
		return false;
	}
	
	public void setContactsPermission(Role role, Integer userId) {
		
		Long processInstanceId = getProcessInstanceId();
		
		getBpmFactory().getRolesManager().setContactsPermission(role,
		    processInstanceId, userId);
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}
	
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	public ProcessWatch getProcessWatcher() {
		return null;
	}
	
	/*
	 * public String getName(Locale locale) { final IWMainApplication iwma =
	 * getIWma();
	 * 
	 * @SuppressWarnings("unchecked") Map<Long, Map<Locale, String>>
	 * cashTaskNames = IWCacheManager2
	 * .getInstance(iwma).getCache(CASHED_TASK_NAMES); final Map<Locale, String>
	 * names; final Long taskInstanceId = getProcessDefinitionW()
	 * .getProcessDefinition().getTaskMgmtDefinition().getStartTask() .getId();
	 * 
	 * if (cashTaskNames.containsKey(taskInstanceId)) { names =
	 * cashTaskNames.get(taskInstanceId); } else { names = new HashMap<Locale,
	 * String>(5); cashTaskNames.put(taskInstanceId, names); } final String
	 * name;
	 * 
	 * if (names.containsKey(locale)) name = names.get(locale); else { View
	 * taskInstanceView = loadView(); name =
	 * taskInstanceView.getDisplayName(locale); names.put(locale, name); }
	 * 
	 * return name; }
	 */

	/*
	 * private IWMainApplication getIWma() { final IWContext iwc =
	 * CoreUtil.getIWContext(); final IWMainApplication iwma;
	 * 
	 * if (iwc != null) { iwma = iwc.getIWMainApplication(); } else { iwma =
	 * IWMainApplication.getDefaultIWMainApplication(); }
	 * 
	 * return iwma; }
	 */

	/*
	 * public View loadView() { Long taskId =
	 * getProcessDefinitionW().getProcessDefinition()
	 * .getTaskMgmtDefinition().getStartTask().getId(); JbpmContext ctx =
	 * getIdegaJbpmContext().createJbpmContext();
	 * 
	 * try { List<String> preferred = new ArrayList<String>(1);
	 * preferred.add(XFormsView.VIEW_TYPE);
	 * 
	 * View view = getBpmFactory().getViewByTask(taskId, false, preferred);
	 * 
	 * return view;
	 * 
	 * } catch (RuntimeException e) { throw e; } catch (Exception e) { throw new
	 * RuntimeException(e); } finally {
	 * getIdegaJbpmContext().closeAndCommit(ctx); } }
	 */

	/**
	 * checks right for process instance and current logged in user
	 * 
	 * @param right
	 * @return
	 */
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
	@Transactional(readOnly = true)
	public boolean hasRight(Right right, User user) {
		
		switch (right) {
			case processHandler:

				try {
					Permission perm = getBpmFactory().getPermissionsFactory()
					        .getAccessPermission(getProcessInstanceId(),
					            Access.caseHandler, user);
					getBpmFactory().getRolesManager().checkPermission(perm);
					
					return true;
					
				} catch (AccessControlException e) {
					return false;
				}
				
			default:
				throw new IllegalArgumentException("Right type " + right
				        + " not supported for cases process instance");
		}
	}
	
	BPMDAO getBpmDAO() {
		return bpmDAO;
	}
	
	@Transactional(readOnly = true)
	public List<BPMEmailDocument> getAttachedEmails() {
		
		ArrayList<String> included = new ArrayList<String>(1);
		included.add(email_fetch_process_name);
		Collection<TaskInstance> taskInstances = getAllTaskInstancesPRVT(null,
		    included);
		
		List<BPMEmailDocument> bpmEmailDocs = new ArrayList<BPMEmailDocument>();
		
		for (Iterator<TaskInstance> iterator = taskInstances.iterator(); iterator
		        .hasNext();) {
			TaskInstance taskInstance = iterator.next();
			
			if (taskInstance.hasEnded()) {
				
				try {
					Permission permission = getPermissionsFactory()
					        .getTaskInstanceViewPermission(true, taskInstance);
					getBpmFactory().getRolesManager().checkPermission(
					    permission);
					
				} catch (BPMAccessControlException e) {
					continue;
				}
				
				Map<String, Object> vars = getVariablesHandler()
				        .populateVariables(taskInstance.getId());
				
				String subject = (String) vars.get("string_subject");
				String fromPersonal = (String) vars.get("string_fromPersonal");
				String fromAddress = (String) vars.get("string_fromAddress");
				
				BPMEmailDocument bpmEmailDocument = new BPMEmailDocumentImpl();
				bpmEmailDocument.setTaskInstanceId(taskInstance.getId());
				bpmEmailDocument.setSubject(subject);
				bpmEmailDocument.setFromAddress(fromAddress);
				bpmEmailDocument.setFromPersonal(fromPersonal);
				bpmEmailDocument.setEndDate(taskInstance.getEnd());
				bpmEmailDocument.setDocumentName(taskInstance.getName());
				bpmEmailDocument.setCreateDate(taskInstance.getCreate());
				bpmEmailDocs.add(bpmEmailDocument);
				
			}
		}
		
		return bpmEmailDocs;
	}
	
	@Transactional(readOnly = true)
	public List<BinaryVariable> getAttachements() {
		
		ArrayList<String> included = new ArrayList<String>(1);
		included.add(add_attachement_process_name);
		Collection<TaskInstance> taskInstances = getAllTaskInstancesPRVT(null,
		    included);
		
		List<BinaryVariable> attachements = new ArrayList<BinaryVariable>();
		
		for (Iterator<TaskInstance> iterator = taskInstances.iterator(); iterator
		        .hasNext();) {
			TaskInstance taskInstance = iterator.next();
			
			if (taskInstance.hasEnded()) {
				
				try {
					Permission permission = getPermissionsFactory()
					        .getTaskInstanceViewPermission(true, taskInstance);
					getBpmFactory().getRolesManager().checkPermission(
					    permission);
					
				} catch (BPMAccessControlException e) {
					continue;
				}
				
				attachements
				        .addAll(getBpmFactory()
				                .getProcessManagerByTaskInstanceId(
				                    taskInstance.getId()).getTaskInstance(
				                    taskInstance).getAttachments());
				
			}
		}
		
		return attachements;
	}
		
	@Transactional(readOnly = false)
	public TaskInstanceW createTask(final String taskName, final long tokenId) {
		
		return getBpmContext().execute(new JbpmCallback() {
			
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				ProcessInstance processInstance = getProcessInstance();
				
				@SuppressWarnings("unchecked")
				List<Token> tkns = processInstance.findAllTokens();
				
				for (Token token : tkns) {
					
					if (token.getId() == tokenId) {
						TaskInstance ti = processInstance.getTaskMgmtInstance()
						        .createTaskInstance(
						            ((TaskNode) token.getNode())
						                    .getTask(taskName), token);
						/*
						 * getBpmFactory().takeView(ti.getId(), true,
						 * preferred);
						 */

						TaskInstanceW taskInstanceW = getBpmFactory()
						        .getProcessManagerByTaskInstanceId(ti.getId())
						        .getTaskInstance(ti.getId());
						
						taskInstanceW.loadView();
						return taskInstanceW;
						
					}
				}
				
				return null;
			}
		});
		
	}
	
	@Transactional(readOnly = true)
	public TaskInstanceW getStartTaskInstance() {
		
		long id = getProcessDefinitionW().getProcessDefinition()
		        .getTaskMgmtDefinition().getStartTask().getId();
		for (TaskInstanceW taskInstanceW : getAllTaskInstances()) {
			if (taskInstanceW.getTaskInstance().getTask().getId() == id) {
				
				return taskInstanceW;
			}
		}
		return null;
	}
	
	@Transactional(readOnly = true)
	public boolean hasEnded() {
		return getProcessInstance().hasEnded();
	}
	
	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}
	
	private UserBusiness getUserBusiness() {
		try {
			IWApplicationContext iwac = IWMainApplication
			        .getDefaultIWApplicationContext();
			return (UserBusiness) IBOLookup.getServiceInstance(iwac,
			    UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}
	
	public Collection<Role> getRolesContactsPermissions(Integer userId) {
		
		Collection<Role> roles = getBpmFactory().getRolesManager()
		        .getUserPermissionsForRolesContacts(getProcessInstanceId(),
		            userId);
		
		return roles;
	}
	
	public Object getVariableLocally(final String variableName, Token token) {
		
		if (token == null)
			token = getProcessInstance().getRootToken();
		
		ContextInstance contextInstnace = token.getProcessInstance()
		        .getContextInstance();
		
		Object val = contextInstnace.getVariableLocally(variableName, token);
		return val;
	}
}