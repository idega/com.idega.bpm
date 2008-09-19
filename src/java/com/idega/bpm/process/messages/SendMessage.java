package com.idega.bpm.process.messages;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/19 15:20:36 $ by $Author: civilis $
 */
public interface SendMessage {

	public abstract void send(final Object context, final ProcessInstance pi, final LocalizedMessages msgs, final Token tkn);
}