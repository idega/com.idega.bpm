package com.idega.bpm.process.invitation;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.validator.EmailValidator;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.bpm.BPMConstants;
import com.idega.bpm.process.messages.LocalizedMessages;
import com.idega.bpm.process.messages.PrependToMessageStartTransformatorImpl;
import com.idega.bpm.process.messages.SendMessage;
import com.idega.bpm.process.messages.SendMessageType;
import com.idega.bpm.process.messages.SendMessagesHandler;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/11/13 15:08:12 $ by $Author: juozas $
 */
@Service("sendParticipantInvitationMessageHandler")
@Scope("prototype")
public class SendParticipantInvitationMessageHandler extends SendMessagesHandler {

	private static final long serialVersionUID = -4337747330253308754L;
	
	private Long processInstanceIdExp;
	private Integer bpmUserIdExp;
	private UserPersonalData userDataExp;
	private Message messageExp;
	
	private SendMessage sendMessage;
	@Autowired private BPMUserFactory bpmUserFactory;
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		if(getProcessInstanceIdExp() != null && getUserDataExp() != null && getBpmUserIdExp() != null) {
			
			ELUtil.getInstance().autowire(this);
			
			final Long pid = getProcessInstanceIdExp();//(Long)JbpmExpressionEvaluator.evaluate(getProcessInstanceIdExp(), ectx);
			final Integer bpmUserId = 	getBpmUserIdExp();//	(Integer)JbpmExpressionEvaluator.evaluate(getBpmUserIdExp(), ectx);
			final ProcessInstance pi = 				ectx.getJbpmContext().getProcessInstance(pid);
			final UserPersonalData upd =  getUserDataExp();//			(UserPersonalData)JbpmExpressionEvaluator.evaluate(getUserDataExp(), ectx);
			final Message msg = getMessageExp();//getMessageExp() != null ? (Message)JbpmExpressionEvaluator.evaluate(getMessageExp(), ectx) : null;
			
			final IWContext iwc = IWContext.getCurrentInstance();
//			final IWResourceBundle iwrb = getResourceBundle(iwc);
			
			String recipientEmail = upd.getUserEmail();
			
			if(recipientEmail == null || !EmailValidator.getInstance().isValid(recipientEmail)) {
				
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Participant email address provided is not valid: "+recipientEmail);
				return;
			}
			
//			TODO: think about language choice
			
//			String caseIdentifier = msg != null ? msg.getCaseIdentifier() : null;
//			boolean setCaseIdentifier = msg != null ? msg.isSetCaseIdentifier() : false;
			String subject = msg != null ? msg.getSubject() : null;
			String text = msg != null ? msg.getText() : null;
			String from = msg != null ? msg.getFrom() : null;
			
//			if(subject == null || subject.length() == 0) {
//				subject = iwrb.getLocalizedString("cases_bpm.case_invitation", "You've been invited to participate in case");
//				
//				if(setCaseIdentifier) {
//					if(caseIdentifier == null || CoreConstants.EMPTY.equals(caseIdentifier) ) {
//						caseIdentifier = (String)pi.getContextInstance().getVariable(ProcessArtifactsProvider.CASE_IDENTIFIER);
//					}
//					
//					subject += CoreConstants.SPACE + caseIdentifier;
//				}
//			}
			
			LocalizedMessages msgs = getLocalizedMessages();
			msgs.setFrom(from);
			ArrayList<String> emailsToSendTo = new ArrayList<String>(1);
			emailsToSendTo.add(recipientEmail);
			msgs.setSendToEmails(emailsToSendTo);
			
			if(text == null) {
				text = CoreConstants.EMPTY;
			} else {
			
				PrependToMessageStartTransformatorImpl subjTransformator = new PrependToMessageStartTransformatorImpl();
				subjTransformator.setPrependText(subject);
				msgs.setSubjectTransformator(subjTransformator);
				
				PrependToMessageStartTransformatorImpl msgTransformator = new PrependToMessageStartTransformatorImpl();
				msgTransformator.setPrependText(text+"\n");
				msgs.setSubjectTransformator(msgTransformator);
			}
			
			if(subject != null && subject.length() != 0) {
				
				msgs.setInlineSubject(iwc.getCurrentLocale(), subject);
			}
			
			if(!msgs.hasMessage() || !msgs.hasSubject()) {
				
				IWMainApplication app = iwc.getIWMainApplication();
				IWBundle bundle = app.getBundle(BPMConstants.IW_BUNDLE_STARTER);
				msgs.setIwb(bundle);
				
				if(!msgs.hasSubject()) {

					msgs.setSubjectKey("cases_bpm.case_invitation");
				}
			
				if(!msgs.hasMessage()) {
					
					msgs.setMsgKey("cases_bpm.case_invitation_message");
					msgs.setMessageValuesExp("{list: {mv: [{type: \"bean\", value: \"bpmUser.urlToTheProcess\"}]}}");
				}
			}
			
			BPMUser bpmUser = getBpmUserFactory().getBPMUser(iwc, bpmUserId);
			
			MessageValueContext mvCtx = new MessageValueContext();
			mvCtx.setValue(MessageValueContext.bpmUserBean, bpmUser);
			
			getSendMessage().send(mvCtx, null, pi, msgs, pi.getRootToken());
		}
	}
	
	public UserPersonalData getUserDataExp() {
		return userDataExp;
	}

	public void setUserDataExp(UserPersonalData userDataExp) {
		this.userDataExp = userDataExp;
	}

	public Message getMessageExp() {
		return messageExp;
	}

	public void setMessageExp(Message messageExp) {
		this.messageExp = messageExp;
	}

	public Long getProcessInstanceIdExp() {
		return processInstanceIdExp;
	}

	public void setProcessInstanceIdExp(Long processInstanceIdExp) {
		this.processInstanceIdExp = processInstanceIdExp;
	}

	public Integer getBpmUserIdExp() {
		return bpmUserIdExp;
	}

	public void setBpmUserIdExp(Integer bpmUserIdExp) {
		this.bpmUserIdExp = bpmUserIdExp;
	}
	
	public SendMessage getSendMessage() {
		return sendMessage;
	}

	@Autowired
	public void setSendMessage(@SendMessageType("email") SendMessage sendMessage) {
		this.sendMessage = sendMessage;
	}

	BPMUserFactory getBpmUserFactory() {
		return bpmUserFactory;
	}

	void setBpmUserFactory(BPMUserFactory bpmUserFactory) {
		this.bpmUserFactory = bpmUserFactory;
	}
}