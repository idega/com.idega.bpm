package com.idega.bpm.process.messages;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.validator.EmailValidator;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.business.EmailSenderHelper;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.converter.util.StringConverterUtility;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.MessageValueHandler;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.SendMail;
import com.idega.util.SendMailMessageValue;
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
	
	public void send(MessageValueContext mvCtx, final Object context, final ProcessInstance pi, final LocalizedMessages msgs, final Token tkn) {

		final UserPersonalData upd = (UserPersonalData) context;

		final IWContext iwc = CoreUtil.getIWContext();
		final IWMainApplication iwma = iwc == null ? getApplication() : IWMainApplication.getIWMainApplication(iwc);
		final IWApplicationContext iwac = iwma.getIWApplicationContext();

		List<String> sendToEmails = msgs.getSendToEmails();
		final ArrayList<String> emailAddresses;

		if (sendToEmails != null)
			emailAddresses = new ArrayList<String>(sendToEmails);
		else
			emailAddresses = new ArrayList<String>(1);

		if (upd != null && upd.getUserEmail() != null)
			emailAddresses.add(upd.getUserEmail());

		final Locale defaultLocale = iwma.getDefaultLocale();

		long pid = pi.getId();
		ProcessInstanceW piw = getBpmFactory().getProcessManagerByProcessInstanceId(pid).getProcessInstance(pid);

		HashMap<Locale, String[]> unformattedForLocales = new HashMap<Locale, String[]>(5);

		// TODO: get default email
		String from = msgs.getFrom();
		if (StringUtil.isEmpty(from) || !EmailValidator.getInstance().isValid(from)) {
			from = iwac.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_MAIL_FROM_ADDRESS, CoreConstants.EMAIL_DEFAULT_FROM);
		}
		String host = iwac.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_SMTP_MAILSERVER, CoreConstants.EMAIL_DEFAULT_HOST);

		// TODO: if upd contains userId, we can get User here and add to message value context

		Locale preferredLocale = iwc != null ? iwc.getCurrentLocale() : defaultLocale;
		final ArrayList<SendMailMessageValue> messageValuesToSend = new ArrayList<SendMailMessageValue>(emailAddresses.size());

		if (mvCtx == null)
			mvCtx = new MessageValueContext(3);

		mvCtx.setValue(MessageValueContext.updBean, upd);
		mvCtx.setValue(MessageValueContext.piwBean, piw);
		mvCtx.setValue(MessageValueContext.iwcBean, iwc);

		final File attachedFile = getAttachedFile(msgs.getAttachFiles(), piw);
		
		for (String email : emailAddresses) {
			String[] subjAndMsg = getFormattedMessage(mvCtx, preferredLocale, msgs, unformattedForLocales, tkn);
			String subject = subjAndMsg[0];
			String text = subjAndMsg[1];

			messageValuesToSend.add(new SendMailMessageValue(attachedFile, null, null, from, host, subject, text, email, null));
		}

		if (!ListUtil.isEmpty(messageValuesToSend)) {
			new Thread(new Runnable() {
				public void run() {
					for (SendMailMessageValue mv : messageValuesToSend) {
						try {
							SendMail.send(mv);
						} catch (javax.mail.MessagingException me) {
							getLogger().log(Level.SEVERE, "Exception while sending email message", me);
						}
					}
					
					if (attachedFile != null) {
						try {
							attachedFile.delete();
						} catch (SecurityException e) {}
					}
				}
			}).start();
		}
	}
	
	private File getAttachedFile(List<String> filesToAttach, ProcessInstanceW piw) {
		if (ListUtil.isEmpty(filesToAttach)) {
			return null;
		}
		
		List<BinaryVariable> attachments = piw.getAttachments();
		if (ListUtil.isEmpty(attachments)) {
			return null;
		}
		
		List<String> filesInSlide = new ArrayList<String>();
		for (BinaryVariable bv: attachments) {
			try {
				String name = "files_".concat(bv.getVariable().getName());
				if (!filesToAttach.contains(name)) {
					continue;
				}
				
				filesInSlide.add(bv.getIdentifier());
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Unable to attach file: " + bv, e);
			}
		}
		
		return getEmailSenderHelper().getFileToAttach(filesInSlide);
	}

	protected String[] getFormattedMessage(MessageValueContext mvCtx, Locale preferredLocale, LocalizedMessages msgs,Map<Locale, String[]> unformattedForLocales,
			Token tkn) {

		String unformattedSubject;
		String unformattedMsg;

		if (!unformattedForLocales.containsKey(preferredLocale)) {
			unformattedSubject = msgs.getLocalizedSubject(preferredLocale);
			unformattedMsg = msgs.getLocalizedMessage(preferredLocale);

			unformattedForLocales.put(preferredLocale, new String[] {unformattedSubject, unformattedMsg});
		} else {
			String[] unf = unformattedForLocales.get(preferredLocale);

			unformattedSubject = unf[0];
			unformattedMsg = unf[1];
		}

		String formattedMsg;
		String formattedSubject;

		if (unformattedMsg == null)
			formattedMsg = unformattedMsg;
		else
			formattedMsg = getFormattedMessage(unformattedMsg, msgs.getMessageValuesExp(), tkn, mvCtx);

		if (unformattedSubject == null)
			formattedSubject = unformattedSubject;
		else
			formattedSubject = getFormattedMessage(unformattedSubject, msgs.getSubjectValuesExp(), tkn, mvCtx);

		formattedMsg = StringConverterUtility.loadConvert(formattedMsg);
		formattedSubject = StringConverterUtility.loadConvert(formattedSubject);

		return new String[] { formattedSubject, formattedMsg };
	}

	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		return getServiceInstance(iwac, UserBusiness.class);
	}

	public String getFormattedMessage(String unformattedMessage, String messageValues, Token tkn, MessageValueContext mvCtx) {
		return getMessageValueHandler().getFormattedMessage(unformattedMessage,	messageValues, tkn, mvCtx);
	}

	public Collection<User> getUsersToSendMessageTo(String rolesNamesAggr, ProcessInstance pi) {
		Collection<User> allUsers;
		if (rolesNamesAggr != null) {
			String[] rolesNames = rolesNamesAggr.trim().split(CoreConstants.SPACE);

			HashSet<String> rolesNamesSet = new HashSet<String>(rolesNames.length);

			for (int i = 0; i < rolesNames.length; i++)
				rolesNamesSet.add(rolesNames[i]);

			allUsers = getBpmFactory().getRolesManager().getAllUsersForRoles(rolesNamesSet, pi.getId());
		} else {
			allUsers = Collections.emptyList();
		}

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