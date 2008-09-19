package com.idega.bpm.process.messages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.validator.EmailValidator;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.converter.util.StringConverterUtility;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.MessageValueHandler;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.SendMail;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/19 15:20:36 $ by $Author: civilis $
 */
@Scope("singleton")
@SendMessageType("email")
@Service
public class SendMailMessageImpl implements SendMessage {
	
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private MessageValueHandler messageValueHandler;

	public void send(final Object context, final ProcessInstance pi, final LocalizedMessages msgs, final Token tkn) {
		
		final UserPersonalData upd = (UserPersonalData)context;
	
		final IWContext iwc = IWContext.getCurrentInstance();
		final IWApplicationContext iwac;
		final IWMainApplication iwma;

		if(iwc != null)
			iwma = IWMainApplication.getIWMainApplication(iwc);
		else
			iwma = IWMainApplication.getDefaultIWMainApplication();
		
		iwac = iwma.getIWApplicationContext();
		
		List<String> sendToEmails = msgs.getSendToEmails();
		final ArrayList<String> emailAddresses;
		
		if(sendToEmails != null)
			emailAddresses = new ArrayList<String>(sendToEmails);
		else
			emailAddresses = new ArrayList<String>(1);
		
		if(upd != null && upd.getUserEmail() != null)
			emailAddresses.add(upd.getUserEmail());
		
		final Locale defaultLocale = iwma.getDefaultLocale();
		
		new Thread(new Runnable() {

			public void run() {
				
				long pid = pi.getId();
				ProcessInstanceW piw = getBpmFactory().getProcessManagerByProcessInstanceId(pid).getProcessInstance(pid);
				
				HashMap<Locale, String[]> unformattedForLocales = new HashMap<Locale, String[]>(5);
				MessageValueContext mvCtx = new MessageValueContext(5);
				
//				TODO: get default email
				String from = msgs.getFrom();
				
				if(from == null || CoreConstants.EMPTY.equals(from) || !EmailValidator.getInstance().isValid(from)) {
					from = iwac.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_MAIL_FROM_ADDRESS, "staff@idega.is");
				}
				
				String host = iwac.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_SMTP_MAILSERVER, "mail.idega.is");
				
//				TODO: if upd contains userId, we can get User here and add to message value context
				
				for (String email : emailAddresses) {
					
					Locale preferredLocale = iwc != null ? iwc.getCurrentLocale() : defaultLocale;
					
					mvCtx.setValue(MessageValueContext.updBean, upd);
					mvCtx.setValue(MessageValueContext.piwBean, piw);
					
					String[] subjAndMsg = getFormattedMessage(mvCtx, preferredLocale, msgs, unformattedForLocales, tkn);
					String subject = subjAndMsg[0];
					String text = subjAndMsg[1];
					
					try {
						SendMail.send(from, email, null, null, host, subject, text);
					} catch (javax.mail.MessagingException me) {
						Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while sending email message", me);
					}
					
				}
			}
			
		}).start();
	}
	
	protected String[] getFormattedMessage(MessageValueContext mvCtx, Locale preferredLocale, LocalizedMessages msgs, Map<Locale, String[]> unformattedForLocales, Token tkn) {
		
		String unformattedSubject;
		String unformattedMsg;
		
		if(!unformattedForLocales.containsKey(preferredLocale)) {
		
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
		
		if(unformattedMsg == null)
			formattedMsg = unformattedMsg;
		else
			formattedMsg = getFormattedMessage(unformattedMsg, msgs.getMessageValuesExp(), tkn, mvCtx);
		
		if(unformattedSubject == null)
			formattedSubject = unformattedSubject;
		else
			formattedSubject = getFormattedMessage(unformattedSubject, msgs.getSubjectValuesExp(), tkn, mvCtx);
		
		formattedMsg = StringConverterUtility.loadConvert(formattedMsg);
		
		return new String[] {formattedSubject, formattedMsg};
	}
	
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness)IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public String getFormattedMessage(String unformattedMessage, String messageValues, Token tkn, MessageValueContext mvCtx) {
		
		return getMessageValueHandler().getFormattedMessage(unformattedMessage, messageValues, tkn, mvCtx);
	}
	
	public Collection<User> getUsersToSendMessageTo(String rolesNamesAggr, ProcessInstance pi) {
		
		Collection<User> allUsers;
		
		if(rolesNamesAggr != null) {
		
			String[] rolesNames = rolesNamesAggr.trim().split(CoreConstants.SPACE);
			
			HashSet<String> rolesNamesSet = new HashSet<String>(rolesNames.length);
			
			for (int i = 0; i < rolesNames.length; i++)
				rolesNamesSet.add(rolesNames[i]);
			
			allUsers = getBpmFactory().getRolesManager().getAllUsersForRoles(rolesNamesSet, pi.getId());
		} else
			allUsers = new ArrayList<User>(0);
		
		return allUsers;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public MessageValueHandler getMessageValueHandler() {
		return messageValueHandler;
	}
}