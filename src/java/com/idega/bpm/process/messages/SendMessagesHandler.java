package com.idega.bpm.process.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.bpm.BPMConstants;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.user.data.User;
import com.idega.user.data.UserHome;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 *
 * Last modified: $Date: 2009/06/24 08:59:39 $ by $Author: valdas $
 */
@Service(SendMessagesHandler.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SendMessagesHandler extends DefaultSpringBean implements ActionHandler {

	private static final long serialVersionUID = -7421283155844789254L;

	public static final String BEAN_NAME = "sendMessagesHandler";

	private String subjectKey;
	private String subjectValues;
	private String messageKey;
	private String messageValues;
	private String messagesBundle;
	private String sendToRoles;
	private Integer recipientUserID;
	private String fromAddress;

	private List<String> sendToEmails, attachFiles;

	private UserPersonalData userData;

 	private Map<String, String> inlineSubject;
	private Map<String, String> inlineMessage;

	private String sendToCreator = null;
	private String receiverMailVariableName = null;

	public String getReceiverMailVariableName() {
		return receiverMailVariableName;
	}

	public void setReceiverMailVariableName(String receiverMailVariableName) {
		this.receiverMailVariableName = receiverMailVariableName;
	}

	public String getSendToCreator() {
		return sendToCreator;
	}

	public void setSendToCreator(String sendToCreator) {
		this.sendToCreator = sendToCreator;
	}

	public boolean isAddCreatorMail(){
		return Boolean.TRUE.toString().equalsIgnoreCase(sendToCreator);
	}

	private SendMessage sendMessage;

	@Override
	public void execute(ExecutionContext ectx) throws Exception {
		final String sendToRoles = getSendToRoles();
		List<String> sendToEmails = getSendToEmails();
		final UserPersonalData upd = getUserData();
		final Integer recipientUserId = getRecipientUserID();

		final Token tkn = ectx.getToken();

		LocalizedMessages msg = getLocalizedMessages();

		msg.setFrom(getFromAddress());
		msg.setSendToRoles(sendToRoles);
		msg.setAttachFiles(getAttachFiles());
		msg.setRecipientUserId(recipientUserId);
		if (isAddCreatorMail()) {
			if (upd == null) {
				try {
					String pId = (String) ectx.getVariable("string_userPersonalId");
					UserHome userHome = (UserHome) IDOLookup.getHome(User.class);
					User user = userHome.findByPersonalID(pId);
					msg.setRecipientUserId(Integer.valueOf(user.getId()));
				} catch (Exception e) {
					Logger.getLogger(SendMessagesHandler.class.getName()).log(Level.WARNING, "Failed getting user data", e);
				}
			} else {
				msg.setRecipientUserId(upd.getUserId());
			}
		}
		String receiverMail = getReceiverMailVariableName();
		if (!StringUtil.isEmpty(receiverMail)) {
			String mail = (String) ectx.getVariable(receiverMail);
			if (!StringUtil.isEmpty(mail)) {
				if (sendToEmails == null) {
					sendToEmails = new ArrayList<String>();
				}
				sendToEmails.add(mail);
			}
		}
		msg.setSendToEmails(sendToEmails);

		getSendMessage().send(null, upd == null ? ectx : upd, ectx.getProcessInstance(), msg, tkn);
	}

	protected Locale getLocale(String key, Map<String, Locale> knownLocales) {
		if (knownLocales.containsKey(key)) {
			return knownLocales.get(key);
		}

	    	Locale locale = ICLocaleBusiness.getLocaleFromLocaleString(key);
	    	knownLocales.put(key, locale);
	    	return locale;
	}

	protected LocalizedMessages getLocalizedMessages() {
		final LocalizedMessages msgs = new LocalizedMessages();

		msgs.setSubjectValuesExp(getSubjectValues());
		msgs.setMessageValuesExp(getMessageValues());

		Map<String, Locale> resolvedLocales = new HashMap<String, Locale>();

		boolean useInlineMessages = true;
		String subjectKey = getSubjectKey();
		String messageKey = getMessageKey();
		String bundleIdentifier = null;
		IWBundle bundle = null;
		if (!StringUtil.isEmpty(subjectKey) && !StringUtil.isEmpty(messageKey) && !subjectKey.startsWith("#{") && !messageKey.startsWith("#{")) {
			bundleIdentifier = getMessagesBundle();
			if (bundleIdentifier == null) {
				bundleIdentifier = BPMConstants.IW_BUNDLE_STARTER;
			}

			bundle = IWMainApplication.getDefaultIWMainApplication().getBundle(bundleIdentifier);
			IWResourceBundle iwrb = bundle.getResourceBundle(getCurrentLocale());
			@SuppressWarnings("deprecation")
			String subjectLocalization = iwrb.getLocalizedString(subjectKey);
			@SuppressWarnings("deprecation")
			String messageLocalization = iwrb.getLocalizedString(messageKey);
			if (subjectLocalization != null && messageLocalization != null &&
				!subjectLocalization.equals(subjectKey) && !messageLocalization.equals(messageKey)
			) {
				useInlineMessages = false;
			}
		}

		if (useInlineMessages) {
			//	Using inline messages
			if (getInlineSubject() != null && !getInlineSubject().isEmpty()) {
				Map<Locale, String> subjects = new HashMap<Locale, String>(getInlineSubject().size());
				for (Entry<String, String> entry: getInlineSubject().entrySet()) {
					Locale subjectLocale = getLocale(entry.getKey(), resolvedLocales);
					subjects.put(subjectLocale, entry.getValue());
				}

				msgs.setInlineSubjects(subjects);
			}

			if (getInlineMessage() != null && !getInlineMessage().isEmpty()) {
				Map<Locale, String> messages = new HashMap<Locale, String>(getInlineMessage().size());
				for (Entry<String, String> entry: getInlineMessage().entrySet()) {
					Locale msgLocale = getLocale(entry.getKey(), resolvedLocales);
					messages.put(msgLocale, entry.getValue());
				}

				msgs.setInlineMessages(messages);
			}
		} else {
			//	Using message keys
			msgs.setIwb(bundle);
			msgs.setSubjectKey(subjectKey);
			msgs.setMsgKey(messageKey);
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

	public Integer getRecipientUserID() {
		return recipientUserID;
	}

	public void setRecipientUserID(Integer recipientUserID) {
		this.recipientUserID = recipientUserID;
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

	public List<String> getAttachFiles() {
		return attachFiles;
	}

	public void setAttachFiles(List<String> attachFiles) {
		this.attachFiles = attachFiles;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	private Boolean sendViaEmail = null;
	private String alsoSendViaEmail;

	public boolean isSendViaEmail() {
		if (sendViaEmail != null) {
			return sendViaEmail;
		}

		sendViaEmail = !StringUtil.isEmpty(alsoSendViaEmail) && Boolean.valueOf(alsoSendViaEmail);
		return sendViaEmail;
	}

	public String getAlsoSendViaEmail() {
		return alsoSendViaEmail;
	}

	public void setAlsoSendViaEmail(String alsoSendViaEmail) {
		this.alsoSendViaEmail = alsoSendViaEmail;
	}
}