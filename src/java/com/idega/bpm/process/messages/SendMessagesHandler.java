package com.idega.bpm.process.messages;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.idegaweb.IWBundle;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.presentation.IWContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2009/01/08 11:48:00 $ by $Author: arunas $
 */
@Service("sendMessagesHandler")
@Scope("prototype")
public class SendMessagesHandler implements ActionHandler {

	private static final long serialVersionUID = -7421283155844789254L;
	private String subjectKey;
	private String subjectValues;
	private String messageKey;
	private String messageValues;
	private String messagesBundle;
	private String sendToRoles;
	private List<String> sendToEmails;
	private UserPersonalData userData;
 	private Map<String, String> inlineSubject;
	private Map<String, String> inlineMessage;
	
	private SendMessage sendMessage;
	
	public void execute(ExecutionContext ectx) throws Exception {
	
		final String sendToRoles = getSendToRoles();
		final List<String> sendToEmails = getSendToEmails();
		final UserPersonalData upd = getUserData();
		
		final Token tkn = ectx.getToken();
		
		LocalizedMessages msg = getLocalizedMessages();
		
		msg.setSendToRoles(sendToRoles);
		msg.setSendToEmails(sendToEmails);
		
		getSendMessage().send(null, upd, ectx.getProcessInstance(), msg, tkn);
	}
	
	protected LocalizedMessages getLocalizedMessages() {
		
		final LocalizedMessages msgs = new LocalizedMessages();
		
		msgs.setSubjectValuesExp(getSubjectValues());
		msgs.setMessageValuesExp(getMessageValues());
		
		if(getMessageKey() == null && getSubjectKey() == null) {
//			using inline messages
			
			if(getInlineSubject() != null && !getInlineSubject().isEmpty()) {
			
				HashMap<Locale, String> subjects = new HashMap<Locale, String>(getInlineSubject().size());
				
				for (Entry<String, String> entry : getInlineSubject().entrySet()) {
				    
				    	Locale subjectLocale = ICLocaleBusiness.getLocaleFromLocaleString(entry.getKey());
				    	subjects.put(subjectLocale, entry.getValue());
				}
				
				msgs.setInlineSubjects(subjects);
			}
			
			if(getInlineMessage() != null && !getInlineMessage().isEmpty()) {
				
				HashMap<Locale, String> messages = new HashMap<Locale, String>(getInlineMessage().size());
				
				for (Entry<String, String> entry : getInlineMessage().entrySet()) {
					Locale msgLocale = ICLocaleBusiness.getLocaleFromLocaleString(entry.getKey());
					messages.put(msgLocale, entry.getValue());
				}
				
				msgs.setInlineMessages(messages);
			}

		} else {
//			using message keys
			
			String bundleIdentifier = getMessagesBundle();
		
			if(bundleIdentifier == null)
				bundleIdentifier = "com.idega.bpm";
			
			final IWBundle iwb = IWContext.getCurrentInstance().getIWMainApplication().getBundle(bundleIdentifier);
			msgs.setIwb(iwb);
			msgs.setSubjectKey(getSubjectKey());
			msgs.setMsgKey(getMessageKey());
		}
		
		return msgs;
	}
	
	public String getSubjectKey() {
		return subjectKey;
	}

	public void setSubjectKey(String subjectKey) {
		this.subjectKey = subjectKey;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}

	public String getSubjectValues() {
		return subjectValues;
	}

	public void setSubjectValues(String subjectValues) {
		this.subjectValues = subjectValues;
	}

	public String getMessageValues() {
		return messageValues;
	}

	public void setMessageValues(String messageValues) {
		this.messageValues = messageValues;
	}

	public String getMessagesBundle() {
		return messagesBundle;
	}

	public void setMessagesBundle(String messagesBundle) {
		this.messagesBundle = messagesBundle;
	}
	
	public SendMessage getSendMessage() {
		return sendMessage;
	}

	@Autowired
	public void setSendMessage(@SendMessageType("email") SendMessage sendMessage) {
		this.sendMessage = sendMessage;
	}

	public Map<String, String> getInlineSubject() {
		return inlineSubject;
	}

	public void setInlineSubject(Map<String, String> inlineSubject) {
		this.inlineSubject = inlineSubject;
	}

	public Map<String, String> getInlineMessage() {
		return inlineMessage;
	}

	public void setInlineMessage(Map<String, String> inlineMessage) {
		this.inlineMessage = inlineMessage;
	}

	public List<String> getSendToEmails() {
		return sendToEmails;
	}

	public void setSendToEmails(List<String> sendToEmails) {
		this.sendToEmails = sendToEmails;
	}

	public String getSendToRoles() {
		return sendToRoles;
	}

	public void setSendToRoles(String sendToRoles) {
		this.sendToRoles = sendToRoles;
	}

	public UserPersonalData getUserData() {
		return userData;
	}

	public void setUserData(UserPersonalData userData) {
		this.userData = userData;
	}
}