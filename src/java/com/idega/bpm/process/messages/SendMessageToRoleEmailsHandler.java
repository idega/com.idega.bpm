package com.idega.bpm.process.messages;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

@Service(SendMessageToRoleEmailsHandler.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SendMessageToRoleEmailsHandler extends SendMessagesHandler {

	private static final long serialVersionUID = -2016206012681471149L;

	public static final String BEAN_NAME = "sendMessageToRoleEmailsHandler";

	@Override
	public void execute(ExecutionContext ectx) throws Exception {
		String sendToRoles = null;
		LocalizedMessages msg = null;
		try {
			sendToRoles = getSendToRoles();

			List<String> sendToEmails = getSendToEmails();
			String customReceivers = getApplication().getSettings().getProperty("receivers_" + SendMessageToRoleEmailsHandler.BEAN_NAME);
			if (!StringUtil.isEmpty(customReceivers)) {
				sendToEmails = Arrays.asList(customReceivers.split(CoreConstants.COMMA));
			}

			final Integer recipientUserId = getRecipientUserID();
			final Token tkn = ectx.getToken();

			msg = getLocalizedMessages();
			msg.setFrom(getFromAddress());
			msg.setSendToRoles(sendToRoles);
			msg.setSendToEmails(sendToEmails);
			msg.setAttachFiles(getAttachFiles());
			msg.setRecipientUserId(recipientUserId);
			getLogger().info("Sending message " + msg);

			getSendMessage().send(null, ectx, ectx.getProcessInstance(), msg, tkn);
		} catch (Exception e) {
			String warning = "Error sending message " + msg + " to roles " + sendToRoles + ". Exception: " + e.getMessage();
			getLogger().log(Level.WARNING, warning, e);
			CoreUtil.sendExceptionNotification(warning, e);
			throw new RuntimeException(warning, e);
		}
	}

	@Override
	public SendMessage getSendMessage() {
		return ELUtil.getInstance().getBean(SendMailMessageToRoles.BEAN_NAME);
	}

}