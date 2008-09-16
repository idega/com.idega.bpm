package com.idega.bpm.xformsview;

/**
 * Just kinda hack for spring proxies (can't cast directly to implementation)
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/16 17:48:15 $ by $Author: civilis $
 */
public interface IXFormViewFactory {

	public abstract XFormsView getXFormsView();
}