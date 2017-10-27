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
import com.idega.util.StringUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class LongConverter implements DataConverter {

	@Override
	public Object convert(Element o) {
		String value = o.getTextContent();
		return convert(value);
	}

	@Override
	public Object convert(String value) {
		return StringUtil.isEmpty(value) ? Long.valueOf(-1) : Long.valueOf(value);
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

		Long value = o instanceof Long ? (Long) o : o instanceof String ? Long.valueOf((String) o) : Long.valueOf(-1);
		Node txtNode = e.getOwnerDocument().createTextNode(String.valueOf(value));
		e.appendChild(txtNode);

		return e;
	}

	@Override
	public VariableDataType getDataType() {
		return VariableDataType.LONG;
	}

}