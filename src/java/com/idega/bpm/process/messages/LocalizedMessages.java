package com.idega.bpm.process.messages;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.idega.idegaweb.IWBundle;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/10/22 14:52:21 $ by $Author: civilis $
 */
public class LocalizedMessages {

	private String from;
	private String sendToRoles;
	private Integer recipientUserId;
	private List<String> sendToEmails, attachFiles, sendCcEmails;
	private String subjectValuesExp;
	private String messageValuesExp;
	private LocalizedMessageTransformator subjectTransformator;
	private LocalizedMessageTransformator messageTransformator;

	private IWBundle iwb;

	private String subjectKey;
	private String msgKey;

	private Map<Locale, String> inlineSubjects;
	private Map<Locale, String> inlineMessages;

	public void setSubjectKey(String subjectKey) {
		this.subjectKey = subjectKey;
	}
	public void setMsgKey(String msgKey) {
		this.msgKey = msgKey;
	}
	public void setInlineSubjects(Map<Locale, String> inlineSubjects) {
		this.inlineSubjects = inlineSubjects;
	}
	public void setInlineSubject(Locale locale, String msg) {
		if (inlineSubjects == null)
			inlineSubjects = new HashMap<Locale, String>(1);

		inlineSubjects.put(locale, msg);
	}
	public void setInlineMessage(Locale locale, String msg) {

		if(inlineMessages == null)
			inlineMessages = new HashMap<Locale, String>(1);

		inlineMessages.put(locale, msg);
	}
	public void setInlineMessages(Map<Locale, String> inlineMessages) {
		this.inlineMessages = inlineMessages;
	}
	public void setIwb(IWBundle iwb) {
		this.iwb = iwb;
	}

	public String getLocalizedSubject(Locale locale) {
		String subject;

		if (iwb != null) {
			subject = subjectKey != null ? iwb.getResourceBundle(locale).getLocalizedString(subjectKey, subjectKey) : CoreConstants.EMPTY;
		} else if (inlineSubjects != null) {
			subject = inlineSubjects.get(locale);
		} else {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to get localized subject, but neither iwb, nor inlineSubjects set");
			subject = null;
		}

		if (getSubjectTransformator() != null)
			subject = getSubjectTransformator().apply(subject, locale);

		return subject;
	}

	public String getLocalizedMessage(Locale locale) {
		String message;

		if (iwb != null) {
			message = msgKey != null ? iwb.getResourceBundle(locale).getLocalizedString(msgKey, msgKey) : CoreConstants.EMPTY;
		} else if (inlineMessages != null) {
			message = inlineMessages.get(locale);
		} else {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to get localized message, but neither iwb, nor inlineMessages set");
			message = null;
		}

		if (getMessageTransformator() != null)
			message = getMessageTransformator().apply(message, locale);

		return message;
	}
	public String getSubjectValuesExp() {
		return subjectValuesExp;
	}
	public void setSubjectValuesExp(String subjectValuesExp) {
		this.subjectValuesExp = subjectValuesExp;
	}
	public String getMessageValuesExp() {
		return messageValuesExp;
	}
	public void setMessageValuesExp(String messageValuesExp) {
		this.messageValuesExp = messageValuesExp;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getSendToRoles() {
		return sendToRoles;
	}
	public Integer getRecipientUserId() {
		return recipientUserId;
	}
	public void setSendToRoles(String sendToRoles) {
		this.sendToRoles = sendToRoles;
	}
	public void setRecipientUserId(Integer recipientUserId) {
		this.recipientUserId = recipientUserId;
	}
	public List<String> getSendToEmails() {
		return sendToEmails;
	}
	public void setSendToEmails(List<String> sendToEmails) {
		this.sendToEmails = sendToEmails;
	}
	public LocalizedMessageTransformator getSubjectTransformator() {
		return subjectTransformator;
	}
	public void setSubjectTransformator(
			LocalizedMessageTransformator subjectTransformator) {
		this.subjectTransformator = subjectTransformator;
	}
	public LocalizedMessageTransformator getMessageTransformator() {
		return messageTransformator;
	}
	public void setMessageTransformator(LocalizedMessageTransformator messageTransformator) {
		this.messageTransformator = messageTransformator;
	}

	public boolean hasSubject() {
		return (inlineSubjects != null && !inlineSubjects.isEmpty()) || (subjectKey != null && subjectKey.length() != 0);
	}

	public boolean hasMessage() {
		return (inlineMessages != null && !inlineMessages.isEmpty()) || (msgKey != null && msgKey.length() != 0);
	}

	public List<String> getAttachFiles() {
		return attachFiles;
	}

	public void setAttachFiles(List<String> attachFiles) {
		this.attachFiles = attachFiles;
	}

	public List<String> getSendCcEmails() {
		return sendCcEmails;
	}

	public void setSendCcEmails(List<String> sendCcEmails) {
		this.sendCcEmails = sendCcEmails;
	}

	@Override
	public String toString() {
		return "To " + sendToEmails + " CC " + sendCcEmails + " or/and to role(s): " + sendToRoles + ". Recipient user: " + getRecipientUserId() + ". Subject: " +
				subjectValuesExp + "; message: " + messageValuesExp + ". Inline subjects: " + inlineSubjects + ", inline messages: " + inlineMessages;
	}

}