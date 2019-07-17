package com.idega.bpm.process.messages;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.validator.EmailValidator;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.business.EmailSenderHelper;
import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.data.Case;
import com.idega.bpm.BPMConstants;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.contact.data.Email;
import com.idega.core.converter.util.StringConverterUtility;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.MessageValueHandler;
import com.idega.jbpm.process.business.messages.TypeRef;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.user.data.UserHome;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.SendMail;
import com.idega.util.SendMailMessageValue;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 *          Last modified: $Date: 2009/01/10 12:34:20 $ by $Author: civilis $
 */
@Service
@SendMessageType("email")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class SendMailMessageImpl extends DefaultSpringBean implements SendMessage {

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private MessageValueHandler messageValueHandler;

	@Autowired
	private EmailSenderHelper emailSenderHelper;

	@Override
	public String getSubject() {
		return null;
	}

	protected List<String> getMailsToSendTo(Object context, LocalizedMessages msgs, ProcessInstance pi) {
		List<String> sendToEmails = msgs.getSendToEmails();
		UserPersonalData upd = getUserPersonalData(context);
		List<String> emailAddresses = null;
		if (sendToEmails != null) {
			emailAddresses = new ArrayList<String>(sendToEmails);
		} else {
			emailAddresses = new ArrayList<String>(1);
		}
		if (upd != null && upd.getUserEmail() != null) {
			emailAddresses.add(upd.getUserEmail());
		}

		if (ListUtil.isEmpty(sendToEmails)) {
			String sendToRoles = msgs.getSendToRoles();
			if (!StringUtil.isEmpty(sendToRoles)) {
				Collection<User> recipients = getUsersToSendMessageTo(sendToRoles, pi);
				if (!ListUtil.isEmpty(recipients)) {
					for (User recipient: recipients) {
						Email email = null;
						try {
							email = recipient.getUsersEmail();
						} catch (Exception e) {}
						if (email != null) {
							emailAddresses.add(email.getEmailAddress());
						}
					}
				}
			}
		}

		String sendToRoles = msgs.getSendToRoles();
		if (sendToRoles != null && sendToRoles.indexOf("owner") != -1) {
			String ownerEmail = (String) pi.getContextInstance().getVariable(BPMConstants.VAR_OWNER_EMAIL);
			if (com.idega.util.EmailValidator.getInstance().isValid(ownerEmail) && !emailAddresses.contains(ownerEmail)) {
				getLogger().info("Resolved owner's email ('" + ownerEmail + "') from application, it was not among receivers: " + emailAddresses + ". Proc. inst. ID: " + pi.getId());
				emailAddresses.add(ownerEmail);
			}
		}

		Integer receipientId = msgs.getRecipientUserId();
		if (receipientId != null) {
			try {
				UserHome userHome = (UserHome) IDOLookup.getHome(User.class);
				User user = userHome.findByPrimaryKey(receipientId);
				Collection<Email> emails = user.getEmails();
				if (ListUtil.isEmpty(emails)) {
					getLogger().log(Level.WARNING, "User " + receipientId + "has no email");
				} else {
					Email email = emails.iterator().next();
					emailAddresses.add(email.getEmailAddress());
				}
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Failed getting mail of user " + receipientId, e);
			}
		}

		return emailAddresses;
	}

	protected UserPersonalData getUserPersonalData(Object context) {
		if (context instanceof UserPersonalData) {
			return (UserPersonalData) context;
		}
		return null;
	}

	private Case getCase(ProcessInstanceW piw) {
		try {
			CaseBusiness caseBusiness = IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CaseBusiness.class);
			return caseBusiness.getCaseByIdentifier(piw.getProcessIdentifier());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void send(MessageValueContext mvCtx, final Object context, final ProcessInstance pi, final LocalizedMessages msgs, final Token tkn) {
		final List<String> emailAddresses = getMailsToSendTo(context, msgs, pi);
		send(emailAddresses, mvCtx, context, pi, msgs, tkn);
	}

	@Override
	public void send(MessageValueContext mvCtx, Object context, ProcessInstance pi, LocalizedMessages msgs, Token tkn, List<User> receivers) {
		List<String> emailAddresses = new ArrayList<>();
		if (!ListUtil.isEmpty(receivers)) {
			UserBusiness userBusiness = getServiceInstance(UserBusiness.class);
			for (User receiver: receivers) {
				Email email = null;
				try {
					email = userBusiness.getUsersMainEmail(receiver);
				} catch (Exception e) {}
				if (email != null) {
					emailAddresses.add(email.getEmailAddress());
				}
			}
		}
		send(emailAddresses, mvCtx, context, pi, msgs, tkn);
	}

	private void send(List<String> emailAddresses, MessageValueContext mvCtx, Object context, ProcessInstance pi, LocalizedMessages msgs, Token tkn) {
		ExecutionContext ectx = null;
		if (context instanceof ExecutionContext) {
			ectx = (ExecutionContext) context;
		} else {
			getLogger().log(Level.WARNING, "Context " + context + (context == null ? " (not provided)" : ", class: " + context.getClass()) +
					" is not instance of " + ExecutionContext.class.getName());
		}

		final IWContext iwc = CoreUtil.getIWContext();
		final IWMainApplication iwma = iwc == null ? getApplication() : IWMainApplication.getIWMainApplication(iwc);
		final IWApplicationContext iwac = iwma.getIWApplicationContext();

		if (ListUtil.isEmpty(emailAddresses)) {
			getLogger().warning("No recipients resolved for " + msgs);
			return;
		}

		final Locale defaultLocale = iwma.getDefaultLocale();

		long pid = pi.getId();
		ProcessInstanceW piw = null;
		if (context instanceof ExecutionContext) {
			piw = getBpmFactory().getProcessInstanceW(((ExecutionContext) context).getJbpmContext(), pid);
		} else {
			piw = getBpmFactory().getProcessManagerByProcessInstanceId(pid).getProcessInstance(pid);
		}

		Map<Locale, String[]> unformattedForLocales = new HashMap<Locale, String[]>(5);

		String from = msgs.getFrom();
		if (StringUtil.isEmpty(from) || !EmailValidator.getInstance().isValid(from)) {
			from = iwac.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_MAIL_FROM_ADDRESS, CoreConstants.EMAIL_DEFAULT_FROM);
		}
		String host = iwac.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_SMTP_MAILSERVER, CoreConstants.EMAIL_DEFAULT_HOST);

		Locale preferredLocale = iwc != null ? iwc.getCurrentLocale() : defaultLocale;
		final List<SendMailMessageValue> messageValuesToSend = new ArrayList<SendMailMessageValue>(emailAddresses.size());

		Case theCase = getCase(piw);
		if (mvCtx == null) {
			mvCtx = new MessageValueContext(theCase == null ? 3: 4);
		}
		setBeans(mvCtx, iwc, piw, context);
		if (theCase != null) {
			mvCtx.setValue(MessageValueContext.caseBean, theCase);
		}

		List<User> usersConnectedToProcess = null;
		File attachedFile = getAttachedFile(msgs.getAttachFiles(), piw, ectx);
		UserBusiness userBusiness = getServiceInstance(UserBusiness.class);
		for (String email: emailAddresses) {
			if (StringUtil.isEmpty(email)) {
				continue;
			}

			Object userBean = mvCtx.getValue(MessageValueContext.userBean);
			Object updBean = mvCtx.getValue(MessageValueContext.updBean);
			if (userBean == null && updBean == null) {
				User user = null;
				Collection<User> users = userBusiness.getUsersByEmail(email);
				if (!ListUtil.isEmpty(users)) {
					usersConnectedToProcess = usersConnectedToProcess == null ? piw.getUsersConnectedToProcess() : usersConnectedToProcess;
					if (ListUtil.isEmpty(usersConnectedToProcess)) {
						getLogger().warning("Unable to find users connected to process " + pid + ", case: " + theCase);
						continue;
					}

					for (Iterator<User> usersIter = users.iterator(); (user == null && usersIter.hasNext());) {
						user = usersIter.next();
						if (user != null && !ListUtil.isEmpty(usersConnectedToProcess) && usersConnectedToProcess.contains(user)) {
						} else {
							user = null;
						}
					}
				}

				if (user == null) {
					getLogger().warning("Unable to find user by email " + email + " connected to process " + pid + ", case: " + theCase);
				} else {
					getLogger().info("Found user (" + user + ") connected to process " + pid + ", case: " + theCase);
					mvCtx.setValue(MessageValueContext.userBean, user);
				}
			}

			String[] subjAndMsg = getFormattedMessage(mvCtx, preferredLocale, msgs, unformattedForLocales, tkn);
			String subject = getSubject();
			if (StringUtil.isEmpty(subject)) {
				subject = subjAndMsg[0];
			}
			String text = subjAndMsg[1];

			//Creating CC
			List<String> ccEmailsList = msgs.getSendCcEmails();
			StringBuilder ccEmails = null;
			if (!ListUtil.isEmpty(ccEmailsList)) {
				ccEmails = new StringBuilder();
				for (Iterator<String> ccEmailsIter = ccEmailsList.iterator(); ccEmailsIter.hasNext();) {
					String ccEmail = ccEmailsIter.next();
					if (EmailValidator.getInstance().isValid(ccEmail)) {
						ccEmails.append(ccEmail);
						if (ccEmailsIter.hasNext()) {
							ccEmails.append(CoreConstants.SEMICOLON);
						}
					}
				}
			}

			SendMailMessageValue mail = new SendMailMessageValue(attachedFile, null, ccEmails == null ? null : ccEmails.toString(), from, host, subject, text, email, null);
			mail.setHeaders(getMailHeaders());
			messageValuesToSend.add(mail);
		}

		sendMails(messageValuesToSend, attachedFile);
	}

	protected void setBeans(MessageValueContext mvCtx, IWContext iwc, ProcessInstanceW piw, Object context) {
		UserPersonalData upd = getUserPersonalData(context);
		if (upd != null) {
			mvCtx.setValue(MessageValueContext.updBean, upd);
		}
		mvCtx.setValue(MessageValueContext.piwBean, piw);
		mvCtx.setValue(MessageValueContext.iwcBean, iwc);
	}

	protected void sendMails(final List<SendMailMessageValue> messages, final File attachedFile) {
		if (ListUtil.isEmpty(messages)) {
			return;
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					for (SendMailMessageValue mv: messages) {
						try {
							SendMail.send(mv);
						} catch (Exception me) {
							String message = "Exception while sending email message: " + mv;
							getLogger().log(Level.WARNING, message, me);
							CoreUtil.sendExceptionNotification(message, me);
						}
					}
				} finally {
					if (attachedFile != null && attachedFile.exists() && attachedFile.canWrite()) {
						attachedFile.delete();
					}
				}
			}

		}).start();
	}

	protected List<AdvancedProperty> getMailHeaders() {
		return Arrays.asList(
				new AdvancedProperty(SendMail.HEADER_AUTO_SUBMITTED, "auto-generated"),
				new AdvancedProperty(SendMail.HEADER_PRECEDENCE, "bulk")
		);
	}

	protected File getAttachedFile(List<String> filesToAttach, ProcessInstanceW piw, ExecutionContext ectx) {
		if (ectx == null || ListUtil.isEmpty(filesToAttach)) {
			return null;
		}

		IWContext iwc = CoreUtil.getIWContext();
		List<BinaryVariable> attachments = piw.getAttachments(iwc);
		if (ListUtil.isEmpty(attachments)) {
			return null;
		}

		List<String> filesInRepository = new ArrayList<String>();
		for (BinaryVariable bv: attachments) {
			try {
				String name = VariableInstanceType.BYTE_ARRAY.getPrefix().concat(bv.getVariable().getName());
				if (!filesToAttach.contains(name)) {
					continue;
				}

				filesInRepository.add(bv.getIdentifier());
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Unable to attach file: " + bv, e);
			}
		}

		String name = piw.getProcessIdentifier();
		if (StringUtil.isEmpty(name)) {
			name = String.valueOf(piw.getProcessInstanceId());
		}
		return getEmailSenderHelper().getFileToAttach(filesInRepository, name);
	}

	protected String[] getFormattedMessage(
			MessageValueContext mvCtx,
			Locale preferredLocale,
			LocalizedMessages msgs,
			Map<Locale, String[]> unformattedForLocales,
			Token tkn
	) {
		String unformattedSubject = null;
		String unformattedMsg = null;

		if (!unformattedForLocales.containsKey(preferredLocale)) {
			unformattedSubject = msgs.getLocalizedSubject(preferredLocale);
			unformattedMsg = msgs.getLocalizedMessage(preferredLocale);

			unformattedForLocales.put(preferredLocale, new String[] {unformattedSubject, unformattedMsg});
		} else {
			String[] unf = unformattedForLocales.get(preferredLocale);

			unformattedSubject = unf[0];
			unformattedMsg = unf[1];
		}

		TypeRef[] dateAndTime = new TypeRef[] {TypeRef.CREATION_DATE, TypeRef.CREATION_TIME};
		if (!StringUtil.isEmpty(unformattedSubject)) {
			for (TypeRef typeRef: dateAndTime) {
				if (unformattedSubject.indexOf(typeRef.getRef()) != -1) {
					unformattedSubject = StringHandler.replace(unformattedSubject, typeRef.getRef(), (String) mvCtx.getValue(typeRef));
				}
			}
		}
		if (!StringUtil.isEmpty(unformattedMsg)) {
			for (TypeRef typeRef: dateAndTime) {
				if (unformattedMsg.indexOf(typeRef.getRef()) != -1) {
					unformattedMsg = StringHandler.replace(unformattedMsg, typeRef.getRef(), (String) mvCtx.getValue(typeRef));
				}
			}
		}

		String formattedMsg = null;
		String formattedSubject = null;

		if (unformattedMsg == null) {
			formattedMsg = unformattedMsg;
		} else {
			formattedMsg = getFormattedMessage(unformattedMsg, msgs.getMessageValuesExp(), tkn, mvCtx);
		}

		if (unformattedSubject == null) {
			formattedSubject = unformattedSubject;
		} else {
			formattedSubject = getFormattedMessage(unformattedSubject, msgs.getSubjectValuesExp(), tkn, mvCtx);
		}

		formattedMsg = StringConverterUtility.loadConvert(formattedMsg);
		formattedSubject = StringConverterUtility.loadConvert(formattedSubject);

		return new String[] { formattedSubject, formattedMsg };
	}

	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		return getServiceInstance(iwac, UserBusiness.class);
	}

	public String getFormattedMessage(String unformattedMessage, String messageValues, Token tkn, MessageValueContext mvCtx) {
		return getMessageValueHandler().getFormattedMessage(unformattedMessage, messageValues, tkn, mvCtx);
	}

	private List<User> getAllUsersByRoles(String[] roles, ProcessInstance pi) {
		List<User> allUsers = new ArrayList<User>();
		for (String string: roles) {
			Set<String> rolesNamesSet = new HashSet<String>(roles.length);
			rolesNamesSet.add(string);

			Collection<User> users = getBpmFactory().getRolesManager().getAllUsersForRoles(rolesNamesSet, pi.getId());

			/* Override so that users that are not directly related to the case can also receive email */
			if (ListUtil.isEmpty(users)) {
				IWApplicationContext iwac = IWMainApplication.getDefaultIWApplicationContext();
				IWMainApplication iwma = IWMainApplication.getDefaultIWMainApplication();

				for (String role: rolesNamesSet) {
					Collection<Group> groups = iwma.getAccessController().getAllGroupsForRoleKeyLegacy(role, iwac);
					for (Group group : groups) {
						try {
							users = getUserBusiness(iwac).getUsersInGroup(group);
							for (User user : users) {
								if (!allUsers.contains(user)) {
									allUsers.add(user);
								}
							}
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}
			} else {
				allUsers.addAll(users);
			}
		}
		return allUsers;
	}

	public List<User> getUsersToSendMessageTo(String rolesNamesAggr, ProcessInstance pi) {
		List<User> allUsers = new ArrayList<User>();
		if (rolesNamesAggr == null) {
			getLogger().warning("Roles expression is not provided");
			return allUsers;
		}

		String[] rolesNames = rolesNamesAggr.trim().split(CoreConstants.SPACE);
		if (ArrayUtil.isEmpty(rolesNames)) {
			getLogger().warning("No roles recognized from expression: " + rolesNamesAggr);
			return allUsers;
		}

		@SuppressWarnings("unchecked")
		List<User> usersConnectedToProcess = getBpmFactory().getBPMDAO().getUsersConnectedToProcess(
				pi.getId(),
				pi.getProcessDefinition().getName(),
				pi.getContextInstance().getVariables()
		);
		if (ListUtil.isEmpty(usersConnectedToProcess)) {
			return getAllUsersByRoles(rolesNames, pi);
		}

		AccessController accessController = IWMainApplication.getDefaultIWMainApplication().getAccessController();
		for (String role: rolesNames) {
			for (User user: usersConnectedToProcess) {
				if (accessController.hasRole(user, role)) {
					allUsers.add(user);
				}
			}
		}

		if (ListUtil.isEmpty(allUsers)) {
			getLogger().warning("No users (" + usersConnectedToProcess + ") connectected to proc. inst. (ID: " + pi.getId() +
					") have role(s) " + rolesNamesAggr + " will try to send to all users having these roles");
			return getAllUsersByRoles(rolesNames, pi);
		}

		getLogger().info("Will send message to users " + allUsers + ". Receivers were resolved by proc. inst. (ID: " + pi.getId() +
				") and role(s): " + rolesNamesAggr);
		return allUsers;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public MessageValueHandler getMessageValueHandler() {
		return messageValueHandler;
	}

	public EmailSenderHelper getEmailSenderHelper() {
		return emailSenderHelper;
	}

	public void setEmailSenderHelper(EmailSenderHelper emailSenderHelper) {
		this.emailSenderHelper = emailSenderHelper;
	}

}