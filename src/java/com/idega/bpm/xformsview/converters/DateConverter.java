package com.idega.bpm.xformsview.converters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.idega.block.process.variables.VariableDataType;
import com.idega.chiba.web.xml.xforms.util.XFormsDateConverter;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $ Last modified: $Date: 2008/09/17 13:09:39 $ by $Author: civilis $
 */
@Service("xFormsDateConverter")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class DateConverter implements DataConverter, XFormsDateConverter {

	private SimpleDateFormat dateFormatter;
	private static final String dateFormat = "yyyy-MM-dd";

	public DateConverter() {
		dateFormatter = new SimpleDateFormat(dateFormat);
	}

	@Override
	public Object convert(Element o) {
		String dateStr = o.getTextContent();
		return convert(dateStr);
	}

	@Override
	public Object convert(String dateStr) {
		if (dateStr == null || dateStr.trim().equals(""))
			return null;

		try {
			return dateFormatter.parse(dateStr);

		} catch (ParseException e) {
			throw new RuntimeException("Exception while parsing date string ("
			        + dateStr + ") for format: " + dateFormat, e);
		}
	}

	@Override
	public Element revert(Object o, Element e) {

		if (!(o instanceof Date))
			throw new IllegalArgumentException(
			        "Wrong class object provided for DateConverter: "
			                + o.getClass().getName()
			                + ". Should be java.util.Date");

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

		Node txtNode = e.getOwnerDocument().createTextNode(
		    convertDateToComplyWithXForms((Date) o));
		e.appendChild(txtNode);

		return e;
	}

	@Override
	public String convertDateToComplyWithXForms(Date date) {

		return dateFormatter.format(date);
	}

	@Override
	public Date convertStringFromXFormsToDate(String dateStr)
	        throws ParseException {

		return dateFormatter.parse(dateStr);
	}

	@Override
	public VariableDataType getDataType() {
		return VariableDataType.DATE;
	}
}