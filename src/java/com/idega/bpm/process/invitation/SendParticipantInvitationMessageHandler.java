package com.idega.bpm.process.invitation;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.validator.EmailValidator;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
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

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 *          Last modified: $Date: 2009/01/16 12:18:51 $ by $Author: juozas $
 */
@Service("sendParticipantInvitationMessageHandler")
@Scope("prototype")
public class SendParticipantInvitationMessageHandler extends
		SendMessagesHandler {

	private static final long serialVersionUID = -4337747330253308754L;

	private Long processInstanceId;
	private Integer bpmUserId;
	private Message message;

	private SendMessage sendMessage;
	@Autowired
	private BPMUserFactory bpmUserFactory;

	public void execute(ExecutionContext ectx) throws Exception {

		if (getProcessInstanceId() != null && getUserData() != null
				&& getBpmUserId() != null) {

			final Long pid = getProcessInstanceId();
			final Integer bpmUserId = getBpmUserId();
			final ProcessInstance pi = ectx.getJbpmContext()
					.getProcessInstance(pid);
			final UserPersonalData upd = getUserData();
			final Message msg = getMessage();

			final IWContext iwc = IWContext.getCurrentInstance();

			String recipientEmail = upd.getUserEmail();

			if (recipientEmail == null
					|| !EmailValidator.getInstance().isValid(recipientEmail)) {

				Logger.getLogger(getClass().getName()).log(
						Level.SEVERE,
						"Participant email address provided is not valid: "
								+ recipientEmail);
				return;
			}

			// TODO: think about language choice

			String subject = msg != null ? msg.getSubject() : null;
			String text = msg != null ? msg.getText() : null;
			String from = msg != null ? msg.getFrom() : null;

			LocalizedMessages msgs = getLocalizedMessages();
			msgs.setFrom(from);
			ArrayList<String> emailsToSendTo = new ArrayList<String>(1);
			emailsToSendTo.add(recipientEmail);
			msgs.setSendToEmails(emailsToSendTo);

			if (text == null) {
				text = CoreConstants.EMPTY;
			} else {

				PrependToMessageStartTransformatorImpl subjTransformator = new PrependToMessageStartTransformatorImpl();
				subjTransformator.setPrependText(subject);
				msgs.setSubjectTransformator(subjTransformator);

				PrependToMessageStartTransformatorImpl msgTransformator = new PrependToMessageStartTransformatorImpl();
				msgTransformator.setPrependText(text + "\n");
				msgs.setMessageTransformator(msgTransformator);
			}

			if (subject != null && subject.length() != 0) {

				msgs.setInlineSubject(iwc.getCurrentLocale(), subject);
			}

			if (!msgs.hasMessage() || !msgs.hasSubject()) {

				IWMainApplication app = iwc.getIWMainApplication();
				IWBundle bundle = app.getBundle(BPMConstants.IW_BUNDLE_STARTER);
				msgs.setIwb(bundle);

				if (!msgs.hasSubject()) {

					msgs.setSubjectKey("cases_bpm.case_invitation");
				}

				if (!msgs.hasMessage()) {

					msgs.setMsgKey("cases_bpm.case_invitation_message");
					msgs
							.setMessageValuesExp("{list: {mv: [{type: \"bean\", value: \"bpmUser.urlToTheProcess\"}]}}");
				}
			}

			BPMUser bpmUser = getBpmUserFactory().getBPMUser(iwc, bpmUserId);

			MessageValueContext mvCtx = new MessageValueContext();
			mvCtx.setValue(MessageValueContext.bpmUserBean, bpmUser);

			getSendMessage().send(mvCtx, null, pi, msgs, pi.getRootToken());
		} else {
			Logger
					.getLogger(getClass().getName())
					.log(
							Level.WARNING,
							"Called sendParticipantInvitationMessageHandler, but inssufficient parameters provided. Process instance id="
									+ getProcessInstanceId()
									+ ", user data bean = "
									+ getUserData()
									+ ", bpm user id="
									+ getBpmUserId()
									+ ", called from process instance id = "
									+ ectx.getProcessInstance().getId());
		}
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

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public Integer getBpmUserId() {
		return bpmUserId;
	}

	public void setBpmUserId(Integer bpmUserId) {
		this.bpmUserId = bpmUserId;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}
}