package com.idega.bpm.xformsview.converters;

import org.w3c.dom.Element;

import com.idega.jbpm.variables.VariableDataType;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/16 17:48:15 $ by $Author: civilis $
 */
public interface DataConverter {

	public abstract Object convert(Element o);
	public abstract Element revert(Object o, Element e);
	public abstract VariableDataType getDataType();
}