package com.idega.bpm.process.messages;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBException;

import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.contact.data.Email;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.SendMail;
import com.idega.util.SendMailMessageValue;

@Service(SendMailMessageToRoles.BEAN_NAME)
@SendMessageType("emailToRoles")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class SendMailMessageToRoles extends SendMailMessageImpl {

	public static final String BEAN_NAME = "sendMailMessageToRoles";

	protected void addUserEmailsToList(List<String> emails,User user) throws EJBException, RemoteException {
		Email email = null;
		try {
			email = user == null ? null : user.getUsersEmail();
		} catch (Exception e) {}

		if (email != null) {
			if (emails == null) {
				emails = new ArrayList<String>();
			}

			emails.add(email.getEmailAddress());
		}
	}

	@Override
	protected List<String> getMailsToSendTo(Object context,LocalizedMessages msgs,ProcessInstance pi){
		List<String> emails = new ArrayList<String>();
		List<String> mails = msgs.getSendToEmails();
		if (!ListUtil.isEmpty(mails)) {
			emails.addAll(mails);
		}

		Collection<User> users = getUsersToSendMessageTo(msgs.getSendToRoles(), pi);
		if (ListUtil.isEmpty(users)) {
			return emails;
		}

		for (User user: users) {
			try {
				addUserEmailsToList(emails,user);
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "failed sending message of process instance " + pi.getId() + " to user " + user.getId(), e);
				continue;
			}
		}
		return emails;
	}

	@Override
	protected UserPersonalData getUserPersonalData(Object context){
		// This data is not needed here
		return null;
	}

	@Override
	protected void setBeans(MessageValueContext mvCtx,IWContext iwc, ProcessInstanceW piw, Object context){
		mvCtx.setValue(MessageValueContext.piwBean, piw);
	}

	@Override
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
							File attachment = mv.getAttachedFile();
							SendMail.send(
									mv.getFrom(),
									mv.getTo(),
									mv.getCc(),
									mv.getBcc(),
									mv.getReplyTo(),
									mv.getHost(),
									mv.getSubject(),
									mv.getText(),
									mv.getHeaders(),
									false,
									false,
									attachment
							);
						} catch (Exception me) {
							String message = "Exception while sending email message: " + mv;
							Logger.getLogger(this.getClass().getName()).log(Level.WARNING, message, me);
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

}