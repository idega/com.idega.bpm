package com.idega.bpm.graph.def;

import java.io.InputStream;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.xml.sax.InputSource;

/**
 *
 * 
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version Revision: 1.0 
 *
 * Last modified: Oct 9, 2008 by Author: Anton 
 *
 */

public class IdegaProcessDefinition extends ProcessDefinition {
	private static final long serialVersionUID = -970150868457640609L;

	public static ProcessDefinition parseXmlInputStream(InputStream inputStream)
    {
        JpdlXmlReader jpdlReader = new IdegaJpdlReader(new InputSource(inputStream));
        return jpdlReader.readProcessDefinition();
    }

}
