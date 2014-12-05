package com.idega.bpm.company.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.builder.business.BuilderLogic;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.contact.data.Email;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.presentation.BPMTaskViewer;
import com.idega.presentation.IWContext;
import com.idega.user.business.NoEmailFoundException;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.SendMail;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Service("sendInvitationToConfirmUserAndCompanyHandler")
public class SendInvitationToConfirmUserAndCompanyHandler implements ActionHandler {

	private static final long serialVersionUID = 3038274093701390134L;

	private static final Logger LOGGER = Logger.getLogger(SendInvitationToConfirmUserAndCompanyHandler.class.getName());

	@Autowired
	private BPMFactory bpmFactory;

	@Override
	public void execute(ExecutionContext context) throws Exception {
		ProcessInstance processInstance = context.getProcessInstance();
		if (processInstance == null) {
			throw new RuntimeException("Process instace is null!");
		}

		long id = processInstance.getId();
		ProcessInstanceW piw = getBpmFactory().getProcessManagerByProcessInstanceId(id).getProcessInstance(id);
		List<TaskInstanceW> tasks = piw.getUnfinishedTaskInstancesForTask("Confirm user and company");
		if (ListUtil.isEmpty(tasks)) {
			throw new RuntimeException("Task 'Confirm user and company' was not found!");
		}

		long taskinstanceId = tasks.get(0).getTaskInstanceId();

		final List<String> emails = getEmails(getHandlers());
		if (ListUtil.isEmpty(emails)) {
			throw new RuntimeException("There are no users to confirm registration! User must have role 'bpm_user_and_company_confirmation_handler' to be able to confirm it.");
		}

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			throw new RuntimeException("IWContext is unavailable");
		}

		String uri = getConfirmationUri(iwc, taskinstanceId);
		if (StringUtil.isEmpty(uri)) {
			throw new RuntimeException("Page with type 'bpm_app_starter' was not found!");
		}

		IWMainApplicationSettings settings = iwc.getApplicationSettings();
		final String from = settings.getProperty(CoreConstants.PROP_SYSTEM_MAIL_FROM_ADDRESS);
		final String host = settings.getProperty(CoreConstants.PROP_SYSTEM_SMTP_MAILSERVER);

		IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle("com.idega.company").getResourceBundle(iwc);
		final String subject = iwrb.getLocalizedString("confirm.user_and_company_registration_email_subject", "Confirm new registration");
		final String text = new StringBuilder(iwrb.getLocalizedString("confirm.new_registration_received_confirm_it",
				"New registration was received, please confirm it at: ")).append(uri).toString();

		if (StringUtil.isEmpty(host) || StringUtil.isEmpty(from)) {
			throw new RuntimeException("Unable to send emails because of missing parameters");
		}

		Thread sender = new Thread(new Runnable() {
			@Override
			public void run() {
				for (String email: emails) {
					try {
						SendMail.send(from, email, null, null, host, subject, text);
					} catch(Exception e) {
						LOGGER.log(Level.WARNING, "Error sending email to: '" + email + "' about new user and company registration ('"+text+"')", e);
					}
				}
			}
		});
		sender.start();
	}

	private String getConfirmationUri(IWContext iwc, long taskInstanceId) {
		BuilderLogic builder = BuilderLogic.getInstance();
		String url = builder.getFullPageUrlByPageType(iwc, "bpm_app_starter", false);
		if (StringUtil.isEmpty(url)) {
			return null;
		}

		URIUtil uri = new URIUtil(url);
		uri.setParameter(BPMTaskViewer.TASK_INSTANCE_PROPERTY, String.valueOf(taskInstanceId));

		return uri.getUri();
	}

	private List<String> getEmails(Collection<User> handlers) {
		if (ListUtil.isEmpty(handlers)) {
			return null;
		}

		UserBusiness userBusiness = getUserBusiness();
		if (userBusiness == null) {
			return null;
		}

		List<String> emails = new ArrayList<String>(handlers.size());
		for (User user: handlers) {
			Email email = getEmail(user);
			String emailAddress = email == null ? null : email.getEmailAddress();
			if (!StringUtil.isEmpty(emailAddress) && !emails.contains(emailAddress)) {
				emails.add(emailAddress);
			}
		}

		return emails;
	}

	private Collection<User> getHandlers() {
		AccessController accessController = IWMainApplication.getDefaultIWMainApplication().getAccessController();
		Collection<Group> groups = accessController.getAllGroupsForRoleKeyLegacy("bpm_user_and_company_confirmation_handler",
				IWMainApplication.getDefaultIWApplicationContext());
		if (ListUtil.isEmpty(groups)) {
			return null;
		}

		UserBusiness userBusiness = getUserBusiness();
		if (userBusiness == null) {
			return null;
		}

		List<User> users = new ArrayList<User>();
		for (Group group: groups) {
			if (group instanceof User) {
				User user = (User) group;
				if (!users.contains(user)) {
					users.add(user);
				}
			} else {
				Collection<User> usersInGroup = null;
				try {
					usersInGroup = userBusiness.getUsersInGroup(group);
				} catch(Exception e) {
					LOGGER.log(Level.WARNING, "Error getting users in group: " + group, e);
				}
				if (!ListUtil.isEmpty(usersInGroup)) {
					for (User user: usersInGroup) {
						if (!users.contains(user)) {
							users.add(user);
						}
					}
				}
			}
		}

		return users;
	}

	private Email getEmail(User user) {
		try {
			return getUserBusiness().getUsersMainEmail(user);
		} catch(NoEmailFoundException e) {
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting main email for user: " + user, e);
		}
		return null;
	}

	private UserBusiness getUserBusiness() {
		try {
			return IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), UserBusiness.class);
		} catch (IBOLookupException e) {
			LOGGER.log(Level.WARNING, "Error getting UserBusiness", e);
		}
		return null;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

}