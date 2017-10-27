package com.idega.bpm.xformsview.converters;

import org.w3c.dom.Element;

import com.idega.block.process.variables.VariableDataType;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/09/17 13:09:39 $ by $Author: civilis $
 */
public interface DataConverter {

	public abstract Object convert(Element o);
	public abstract Object convert(String value);
	public abstract Element revert(Object o, Element e);
	public abstract VariableDataType getDataType();
}