package com.idega.bpm.process.messages;

import java.util.Locale;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/10/22 14:52:28 $ by $Author: civilis $
 */
public interface LocalizedMessageTransformator {

	public abstract String apply(String original, Locale locale);
}