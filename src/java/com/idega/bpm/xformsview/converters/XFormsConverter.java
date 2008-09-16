package com.idega.bpm.xformsview.converters;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.idega.jbpm.variables.Converter;
import com.idega.jbpm.variables.VariableDataType;
import com.idega.util.CoreConstants;
import com.idega.util.xml.XPathUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/16 17:48:15 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class XFormsConverter implements Converter {

//	TODO: move this mapping att to some public place
	private static final String MAPPING_ATT = "mapping";
	private DataConvertersFactory convertersFactory;
	final private XPathUtil mappingXPUT = new XPathUtil("//*[@mapping]");
	
	public Map<String, Object> convert(Object submissionData) {
		Node sdNode = (Node)submissionData;
		
		NodeList result;
		
		synchronized (mappingXPUT) {
			result = mappingXPUT.getNodeset(sdNode);
		}
		
		if(result.getLength() == 0)
			return null;
		
		Map<String, Object> variables = new HashMap<String, Object>();
		
		for (int i = 0; i < result.getLength(); i++) {
			
			Element element = (Element)result.item(i);
			String mapping = element.getAttribute(MAPPING_ATT);
			
			Object variableValue = getConvertersFactory().createConverter(getDataType(mapping)).convert(element);
			
			if(variableValue != null)
				variables.put(mapping, variableValue);
		}
		
		return variables;
	}
	
	protected VariableDataType getDataType(String mapping) {
		
		String strRepr = mapping.contains(CoreConstants.UNDER) ? mapping.substring(0, mapping.indexOf(CoreConstants.UNDER)) : "string";
		return VariableDataType.getByStringRepresentation(strRepr);
	}

	public Object revert(Map<String, Object> variables, Object submissionData) {
		
		if(variables == null || variables.isEmpty())
			return submissionData;
		
		Node sdNode = (Node)submissionData;
		NodeList result;
		
		synchronized (mappingXPUT) {
			result = mappingXPUT.getNodeset(sdNode);
		}
		
		if(result.getLength() == 0)
			return null;
		
		for (int i = 0; i < result.getLength(); i++) {
			
			Element element = (Element)result.item(i);
			String mapping = element.getAttribute(MAPPING_ATT);
			
			if(variables.containsKey(mapping)) {
				
				Object o = variables.get(mapping);
				if(o != null)
					getConvertersFactory().createConverter(getDataType(mapping)).revert(o, element);
			}
		}
		
		return sdNode;
	}
	
	public static void main(String[] args) {
	
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setNamespaceAware(true);
			Node sdNode = dbFactory.newDocumentBuilder().parse(new File("/Users/civilis/dev/workspace/eplatform-4/com.idega.formbuilder/src/test/java/com/idega/formbuilder/tests/basic/submissionTest.xml"));
			
			XFormsConverter converter = new XFormsConverter();
			converter.setConvertersFactory(new DataConvertersFactory());
			
			Map<String, Object> variables = converter.convert(sdNode);
			System.out.println("variables got: "+variables.keySet());
			
			
			sdNode = dbFactory.newDocumentBuilder().parse(new File("/Users/civilis/dev/workspace/eplatform-4/com.idega.formbuilder/src/test/java/com/idega/formbuilder/tests/basic/submissionTestRevert.xml"));
			
			converter.revert(variables, sdNode);
			
			org.chiba.xml.dom.DOMUtil.prettyPrintDOM(sdNode);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public DataConvertersFactory getConvertersFactory() {
		return convertersFactory;
	}

	@Autowired
	public void setConvertersFactory(DataConvertersFactory convertersFactory) {
		this.convertersFactory = convertersFactory;
	}
}