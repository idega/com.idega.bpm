package com.idega.bpm.jsfcomponentview;

import java.util.Locale;

import com.idega.jbpm.view.JSFComponentView;

/**
 *  Interface that should be implemented by jsf component, which will be used
 * 	as bmp view.
 * 
 * @author <a href="mailto:juozas@idega.com">Juozapas Zabukas</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/12/08 10:15:19 $ by $Author: juozas $
 */
public interface BPMCapableJSFComponent {

	public void setView(JSFComponentView view);
	public String getDefaultDisplayName();
	public String getDisplayName(Locale locale);
	
}
