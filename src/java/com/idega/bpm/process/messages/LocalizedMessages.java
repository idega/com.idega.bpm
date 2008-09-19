package com.idega.bpm.process.messages;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.idega.idegaweb.IWBundle;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/19 15:20:36 $ by $Author: civilis $
 */
public class LocalizedMessages {
	
	private String from;
	private String sendToRoles;
	private List<String> sendToEmails;
	private String subjectValuesExp;
	private String messageValuesExp;
	
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
	public void setInlineMessages(Map<Locale, String> inlineMessages) {
		this.inlineMessages = inlineMessages;
	}
	public void setIwb(IWBundle iwb) {
		this.iwb = iwb;
	}
	
	public String getLocalizedSubject(Locale locale) {
	
		if(iwb != null) {
			return iwb.getResourceBundle(locale).getLocalizedString(subjectKey, subjectKey);
		} else if (inlineSubjects != null) {
			return inlineSubjects.get(locale);
		} else {
			
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to get localized subject, but neither iwb, nor inlineSubjects set");
			return null;
		}
	}
	
	public String getLocalizedMessage(Locale locale) {
		
		if(iwb != null) {
			return iwb.getResourceBundle(locale).getLocalizedString(msgKey, msgKey);
		} else if (inlineMessages != null) {
			return inlineMessages.get(locale);
		} else {
			
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to get localized message, but neither iwb, nor inlineMessages set");
			return null;
		}
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
	public void setSendToRoles(String sendToRoles) {
		this.sendToRoles = sendToRoles;
	}
	public List<String> getSendToEmails() {
		return sendToEmails;
	}
	public void setSendToEmails(List<String> sendToEmails) {
		this.sendToEmails = sendToEmails;
	}
}