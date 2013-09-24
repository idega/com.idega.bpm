package com.idega.bpm.process.messages;

import java.util.List;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.expression.ELUtil;

@Service(SendMessageToRoleEmailsHandler.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SendMessageToRoleEmailsHandler extends SendMessagesHandler{
	
	private static final long serialVersionUID = -2016206012681471149L;
	
	public static final String BEAN_NAME = "sendMessageToRoleEmailsHandler";
	
	
	@Override
	public void execute(ExecutionContext ectx) throws Exception {
		final String sendToRoles = getSendToRoles();
		final List<String> sendToEmails = getSendToEmails();
		final Integer recipientUserId = getRecipientUserID();

		final Token tkn = ectx.getToken();

		LocalizedMessages msg = getLocalizedMessages();

		msg.setFrom(getFromAddress());
		msg.setSendToRoles(sendToRoles);
		msg.setSendToEmails(sendToEmails);
		msg.setAttachFiles(getAttachFiles());
		msg.setRecipientUserId(recipientUserId);
		getSendMessage().send(null, ectx, ectx.getProcessInstance(), msg, tkn);
	}
	
	@Override
	public SendMessage getSendMessage() {
		return ELUtil.getInstance().getBean(SendMailMessageToRoles.BEAN_NAME);
	}
	
}
