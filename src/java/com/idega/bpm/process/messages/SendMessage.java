package com.idega.bpm.process.messages;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

import com.idega.jbpm.process.business.messages.MessageValueContext;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/10/22 14:53:18 $ by $Author: civilis $
 */
public interface SendMessage {

	public abstract void send(MessageValueContext mvCtx, Object context, ProcessInstance pi, LocalizedMessages msgs, Token tkn);
}