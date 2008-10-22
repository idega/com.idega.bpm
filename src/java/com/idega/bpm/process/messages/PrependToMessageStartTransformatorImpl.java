package com.idega.bpm.process.messages;

import java.util.Locale;

import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/10/22 14:52:28 $ by $Author: civilis $
 */
public class PrependToMessageStartTransformatorImpl implements LocalizedMessageTransformator {
	
	private String addText;

	public String apply(String original, Locale locale) {
		
		if(original == null)
			original = CoreConstants.EMPTY;

		if(getAddText() != null)
			original = getAddText() + original;
			
		return original;
	}

	public String getAddText() {
		return addText;
	}

//	TODO: add support to add text for locale (not needed now)
	public void setPrependText(String addText) {
		this.addText = addText;
	}
}