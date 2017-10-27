package com.idega.bpm.xformsview.converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.idega.block.process.variables.VariableDataType;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/09/17 13:09:39 $ by $Author: civilis $
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class CollectionConverter implements DataConverter {

	@Override
	public Object convert(Element o) {
		String txt = o.getTextContent();
		return convert(txt);
	}

	@Override
	public Object convert(String txt) {
		if(txt != null)
			txt = txt.trim();

		List<String> values = new ArrayList<String>();

		if(txt == null || txt.equals(CoreConstants.EMPTY))
			return values;

		String[] splitted = txt.split(CoreConstants.SPACE);
		values.addAll(Arrays.asList(splitted));

		return values;
	}
	@Override
	public Element revert(Object o, Element e) {

		if(!(o instanceof List))
			throw new IllegalArgumentException("Wrong class object provided for CollectionConverter: "+o.getClass().getName()+". Should be java.util.List");

		NodeList childNodes = e.getChildNodes();

		List<Node> childs2Remove = new ArrayList<Node>();

		for (int i = 0; i < childNodes.getLength(); i++) {

			Node child = childNodes.item(i);

			if(child != null && (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.ELEMENT_NODE))
				childs2Remove.add(child);
		}

		for (Node node : childs2Remove)
			e.removeChild(node);

		@SuppressWarnings("unchecked")
		Collection<String> values = (Collection<String>)o;

		StringBuilder sb = new StringBuilder();

		for (String value : values) {

			if(value == null || value.trim().equals(CoreConstants.EMPTY))
				continue;

			sb.append(value);
			sb.append(CoreConstants.SPACE);
		}

		e.appendChild(e.getOwnerDocument().createTextNode(sb.toString().trim()));

		return e;
	}

	@Override
	public VariableDataType getDataType() {
		return VariableDataType.LIST;
	}
}