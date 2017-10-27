package com.idega.bpm.xformsview.converters;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.idega.block.process.variables.VariableDataType;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/10/28 10:32:53 $ by $Author: civilis $
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class StringConverter implements DataConverter {

	@Override
	public Object convert(Element o) {
		String txt = o.getTextContent();
		return convert(txt);
	}

	@Override
	public Object convert(String txt) {
		return txt == null || txt.length() == 0 ? null : txt;
	}
	@Override
	public Element revert(Object o, Element e) {

		NodeList childNodes = e.getChildNodes();

		List<Node> childs2Remove = new ArrayList<Node>();

		for (int i = 0; i < childNodes.getLength(); i++) {

			Node child = childNodes.item(i);

			if(child != null && (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.ELEMENT_NODE))
				childs2Remove.add(child);
		}

		for (Node node : childs2Remove)
			e.removeChild(node);

		Node txtNode = e.getOwnerDocument().createTextNode(o instanceof String ? (String)o : o.toString());
		e.appendChild(txtNode);

		return e;
	}

	@Override
	public VariableDataType getDataType() {
		return VariableDataType.STRING;
	}
}