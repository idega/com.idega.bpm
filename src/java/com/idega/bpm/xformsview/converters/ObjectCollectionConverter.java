package com.idega.bpm.xformsview.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chiba.xml.dom.DOMUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.idega.jbpm.variables.VariableDataType;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * @author <a href="mailto:anton@idega.com">Anton Makarov</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/09/16 17:48:15 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class ObjectCollectionConverter implements DataConverter {

	private static final String listElName = "list";
	private static final String rowElName = "row";

	public Object convert(Element o) {

		Element listEl = DOMUtil.getChildElement(o, listElName);
		@SuppressWarnings("unchecked")
		List<Element> rowNodes = DOMUtil.getChildElements(listEl);
		List<String> rowList = new ArrayList<String>();

		for (Element rowElem : rowNodes) {
			@SuppressWarnings("unchecked")
			List<Element> columns = DOMUtil.getChildElements(rowElem);
			Map<String, String> columnMap = new LinkedHashMap<String, String>();

			for (Element columnElem : columns) {
				columnMap.put(columnElem.getNodeName(), columnElem
						.getTextContent());
			}
			rowList.add(ObjToJSON(columnMap));
		}

		return rowList;
	}

	public Element revert(Object o, Element e) {

		if (!(o instanceof List))
			throw new IllegalArgumentException(
					"Wrong class object provided for ObjCollectionConverter: "
							+ o.getClass().getName()
							+ ". Should be java.util.List");

		e = DOMUtil.getChildElement(e, listElName);
		NodeList childNodes = e.getChildNodes();

		List<Node> childs2Remove = new ArrayList<Node>();

		for (int i = 0; i < childNodes.getLength(); i++) {

			Node child = childNodes.item(i);

			if (child != null
					&& (child.getNodeType() == Node.TEXT_NODE || child
							.getNodeType() == Node.ELEMENT_NODE))
				childs2Remove.add(child);
		}

		for (Node node : childs2Remove)
			e.removeChild(node);

		@SuppressWarnings("unchecked")
		Collection<String> rowList = (Collection<String>) o;

		for (String rowStr : rowList) {
			Map<String, String> columnMap = JSONToObj(rowStr);

			Element rowElem = e.getOwnerDocument().createElement(rowElName);

			for (String column : columnMap.keySet()) {
				Element columnEl = rowElem.getOwnerDocument().createElement(
						column);
				columnEl.setTextContent(columnMap.get(column));
				rowElem.appendChild(columnEl);
			}

			e.appendChild(rowElem);
		}
		return e;
	}

	private String ObjToJSON(Map<String, String> obj) {
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		String jsonOut = xstream.toXML(obj);
		return jsonOut;
	}

	private Map<String, String> JSONToObj(String jsonIn) {
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		@SuppressWarnings("unchecked")
		Map<String, String> obj = (Map<String, String>) xstream.fromXML(jsonIn);
		return obj;
	}

	public VariableDataType getDataType() {
		return VariableDataType.OBJLIST;
	}
}