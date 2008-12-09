package com.idega.bpm.xformsview;

/**
 * Just kinda hack for spring proxies (can't cast directly to implementation)
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 *          Last modified: $Date: 2008/12/09 02:49:00 $ by $Author: civilis $
 */
public interface IXFormViewFactory {

	public abstract XFormsView getXFormsView();

	public abstract XFormsViewSubmission getViewSubmission();
}