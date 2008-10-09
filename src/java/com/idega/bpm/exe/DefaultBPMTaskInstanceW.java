package com.idega.bpm.exe;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.variables.Variable;
import com.idega.bpm.xformsview.XFormsView;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.cache.IWCacheManager2;
import com.idega.core.file.data.ExtendedFile;
import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessException;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMAccessControlException;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.view.View;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.11 $
 *
 * Last modified: $Date: 2008/10/07 13:31:11 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("defaultTIW")
public class DefaultBPMTaskInstanceW implements TaskInstanceW {
	
	@Autowired private TmpFilesManager fileUploadManager;
	@Autowired @TmpFileResolverType("defaultResolver") private TmpFileResolver uploadedResourceResolver;
	
	private Long taskInstanceId;
	private TaskInstance taskInstance;
	
	private BPMFactory bpmFactory;
	private BPMContext idegaJbpmContext;
	private VariablesHandler variablesHandler;
	
	private static final String CASHED_TASK_NAMES = "defaultBPM_taskinstance_names";
	
	public TaskInstance getTaskInstance() {
		
		if(taskInstance == null && getTaskInstanceId() != null) {
			
			JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
			
			try {
				taskInstance = ctx.getTaskInstance(getTaskInstanceId());
				
			} finally {
				getIdegaJbpmContext().closeAndCommit(ctx);
			}
		}
		return taskInstance;
	}

	public void assign(User usr) {
		
		Object pk = usr.getPrimaryKey();
		Integer userId;
		
		if(pk instanceof Integer)
			userId = (Integer)pk;
		else
			userId = new Integer(pk.toString());
		
		assign(userId);
	}
	
	public void assign(int userId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			Long taskInstanceId = getTaskInstanceId();
			RolesManager rolesManager = getBpmFactory().getRolesManager();
			rolesManager.hasRightsToAssignTask(taskInstanceId, userId);
			
			getTaskInstance().setActorId(String.valueOf(userId));
			ctx.save(getTaskInstance());
		
		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public User getAssignedTo() {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			Long taskInstanceId = getTaskInstanceId();
			
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			String actorId = taskInstance.getActorId();

			User usr;
			
			if(actorId != null) {
				
				try {
					int assignedTo = Integer.parseInt(actorId);
					usr = getUserBusiness().getUser(assignedTo);
					
				} catch (Exception e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving assigned user name for actor id: "+actorId, e);
					usr = null;
				}
			} else
				usr = null;
			
			return usr;
		
		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	public void start(int userId) {
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			Long taskInstanceId = getTaskInstanceId();
			RolesManager rolesManager = getBpmFactory().getRolesManager();
			rolesManager.hasRightsToStartTask(taskInstanceId, userId);
			
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			taskInstance.start();
			
			ctx.save(taskInstance);
		
		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	public void submit(View view) {
		submit(view, true);
	}

	public void submit(View view, boolean proceedProcess) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			Long taskInstanceId = getTaskInstanceId();
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			if(taskInstance.hasEnded())
				throw new ProcessException("Task instance ("+taskInstanceId+") is already submitted", "Task instance is already submitted");
			
	    	submitVariablesAndProceedProcess(taskInstance, view.resolveVariables(), proceedProcess);
	    	ctx.save(taskInstance);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	protected void submitVariablesAndProceedProcess(TaskInstance ti, Map<String, Object> variables, boolean proceed) {
		
		getVariablesHandler().submitVariables(variables, ti.getId(), true);
		
		if(proceed) {
		
			String actionTaken = (String)ti.getVariable(ProcessConstants.actionTakenVariableName);
	    	
	    	if(actionTaken != null && !CoreConstants.EMPTY.equals(actionTaken) && false)
	    		ti.end(actionTaken);
	    	else
	    		ti.end();
		} else {
			ti.setEnd(new Date());
		}
    	
		BPMUser bpmUser = getBpmFactory().getBpmUserFactory().getCurrentBPMUser();
		
		if(bpmUser != null) {
		
			Integer usrId = bpmUser.getIdToUse();
			
			if(usrId != null)
				ti.setActorId(usrId.toString());
		}
	}
	
	public View loadView() {
		
		Long taskInstanceId = getTaskInstanceId();
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			List<String> preferred = new ArrayList<String>(1);
			preferred.add(XFormsView.VIEW_TYPE);
			
			View view;
			
			if(taskInstance.hasEnded()) {
				
				view = getBpmFactory().getViewByTaskInstance(taskInstanceId, false, preferred);
				
			} else {
				
				view = getBpmFactory().takeView(taskInstanceId, true, preferred);
			}
			
			Map<String, String> parameters = new HashMap<String, String>(1);
			parameters.put(ProcessConstants.TASK_INSTANCE_ID, String.valueOf(taskInstance.getId()));
			view.populateParameters(parameters);
			view.populateVariables(getVariablesHandler().populateVariables(taskInstance.getId()));
			
			return view;
		
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Required
	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	protected UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil.getIWContext(), UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public String getName(Locale locale) {
		
		final IWMainApplication iwma = getIWMA();
		@SuppressWarnings("unchecked")
		Map<Long, Map<Locale, String>> cashTaskNames = IWCacheManager2.getInstance(iwma).getCache(CASHED_TASK_NAMES);
		final Map<Locale, String> names;
		final Long taskInstanceId = getTaskInstanceId();
		
//		synchronized (cashTaskNames) {
//			synchronizing on CASHED_TASK_NAMES map, as it's accessed from multiple threads
			
			if(cashTaskNames.containsKey(taskInstanceId)) {
			
				names = cashTaskNames.get(getTaskInstanceId());
			} else {
				
				names = new HashMap<Locale, String>(5);
				cashTaskNames.put(taskInstanceId, names);
			}
//		}
		
		final String name;
		
		if(names.containsKey(locale))
			name = names.get(locale);
		else {
			
			View taskInstanceView = loadView();
			name = taskInstanceView.getDisplayName(locale);
			names.put(locale, name);
		}
		
		return name;
	}
	
	public void setTaskRolePermissions(Role role, boolean setSameForAttachments, String variableIdentifier) {
		
		Long processInstanceId = getTaskInstance().getProcessInstance().getId();

		getBpmFactory().getRolesManager().setTaskRolePermissionsTIScope(
				role, processInstanceId, getTaskInstanceId(), setSameForAttachments, variableIdentifier
		);
	}
	
	private IWMainApplication getIWMA() {
		
		final IWContext iwc = IWContext.getCurrentInstance();
		final IWMainApplication iwma;
//		trying to get iwma from iwc, if available, downgrading to default iwma, if not
		
		if(iwc != null) {
			
			iwma = iwc.getIWMainApplication();
			
		} else {
			iwma = IWMainApplication.getDefaultIWMainApplication();
		}
		
		return iwma;
	}

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Required
	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	@Required
	@Autowired
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}

	public BinaryVariable addAttachment(Variable variable, String fileName, String description, InputStream is) {
		
		String filesFolder = getTaskInstanceId()+System.currentTimeMillis() + CoreConstants.SLASH;
		
		getFileUploadManager().uploadToTmpDir(filesFolder, fileName, is, getUploadedResourceResolver());
		
		Collection<URI> uris = getFileUploadManager().getFilesUris(filesFolder, null, getUploadedResourceResolver());
		URI uri = uris.iterator().next();
		
		String variableName = variable.getDefaultStringRepresentation();
		
		Map<String, Object> vars = new HashMap<String, Object>(1);
		vars.put(variableName, new ExtendedFile(uri, description));

		vars = getVariablesHandler().submitVariablesExplicitly(vars, getTaskInstanceId());
		
		List<BinaryVariable> binVars = getVariablesHandler().getBinaryVariablesHandler().resolveBinaryVariablesAsList(vars);
		BinaryVariable binVar = binVars.iterator().next();
		
		getFileUploadManager().cleanup(filesFolder, null, getUploadedResourceResolver());
		
		return binVar;
	}

	TmpFilesManager getFileUploadManager() {
		return fileUploadManager;
	}

	TmpFileResolver getUploadedResourceResolver() {
		return uploadedResourceResolver;
	}
}