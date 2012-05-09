package com.idega.bpm.xformsview.converters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chiba.xml.dom.DOMUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.idega.block.process.variables.VariableDataType;
import com.idega.bpm.xformsview.converters.bean.JSONFixer;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.business.DefaultSpringBean;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * @author <a href="mailto:anton@idega.com">Anton Makarov</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/09/17 13:09:39 $ by $Author: civilis $
 */

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ObjectCollectionConverter extends DefaultSpringBean implements DataConverter {

	private static final String listElName = "list";
	private static final String rowElName = "row";

	@Override
	public Object convert(Element o) {
		Element listEl = DOMUtil.getChildElement(o, listElName);
		@SuppressWarnings("unchecked")
		List<Element> rowNodes = DOMUtil.getChildElements(listEl);
		List<String> rowList = new ArrayList<String>();

		HierarchicalStreamDriver driver = new JettisonMappedXmlDriver();
		for (Element rowElem : rowNodes) {
			@SuppressWarnings("unchecked")
			List<Element> columns = DOMUtil.getChildElements(rowElem);
			Map<String, String> columnMap = new LinkedHashMap<String, String>();

			for (Element columnElem : columns) {
				columnMap.put(columnElem.getNodeName(), columnElem.getTextContent());
			}
			rowList.add(getSerializedObjectToJSON(columnMap, driver));
		}

		return rowList;
	}

	@Override
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
			if (child != null && (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.ELEMENT_NODE))
				childs2Remove.add(child);
		}

		for (Node node : childs2Remove)
			e.removeChild(node);

		@SuppressWarnings("unchecked")
		Collection<String> rowList = (Collection<String>) o;

		for (String rowStr : rowList) {
			Map<String, String> columnMap = JSONToObj(rowStr);
			if (MapUtil.isEmpty(columnMap))
				throw new RuntimeException("Unable to transform provided JSON into the object:\n" + rowStr);

			Element rowElem = e.getOwnerDocument().createElement(rowElName);

			for (String column : columnMap.keySet()) {
				Element columnEl = rowElem.getOwnerDocument().createElement(column);
				columnEl.setTextContent(columnMap.get(column));
				rowElem.appendChild(columnEl);
			}

			e.appendChild(rowElem);
		}
		return e;
	}

	private String getSerializedObjectToJSON(Map<String, String> obj, HierarchicalStreamDriver driver) {
		if (driver == null)
			driver = new JettisonMappedXmlDriver();

		XStream xstream = new XStream(driver);
		String jsonOut = xstream.toXML(obj);

		try {
			Map<String, String> object = JSONToObj(jsonOut);
			if (object != null) {
				Map<String, Boolean> alreadyReplaced = new HashMap<String, Boolean>();

				for (String key: object.keySet()) {
					String parsedValue = object.get(key);
					String originalValue = obj.get(key);
					if (!StringUtil.isEmpty(parsedValue) && !StringUtil.isEmpty(originalValue) && !originalValue.equals(parsedValue)) {
						boolean canReplace = true;
						boolean addQoutes = true;

						if (originalValue.indexOf(CoreConstants.DOT) != -1) {
							Double tmpValue = null;
							try {
								tmpValue = Double.valueOf(parsedValue);
							} catch (NumberFormatException e) {}
							canReplace = tmpValue == null;
						} else if (key.toLowerCase().indexOf("id") != -1 || key.toLowerCase().indexOf("kennitala") != -1) {
							if (parsedValue.length() < 10 && originalValue.length() == 10)
								addQoutes = false;
						}

						if (!canReplace)
							continue;

						if (StringHandler.isNaturalNumber(originalValue.substring(0, 1)))
							if (addQoutes)
								originalValue = CoreConstants.QOUTE_MARK.concat(originalValue).concat(CoreConstants.QOUTE_MARK);

						String replacement = CoreConstants.QOUTE_MARK.concat(key).concat(CoreConstants.QOUTE_MARK)
								.concat(CoreConstants.COMMA);
						String whatToReplace = replacement.concat(parsedValue);

						if (alreadyReplaced.containsKey(whatToReplace))
							continue;

						String withWhatToReplace = replacement.concat(originalValue);

						getLogger().info("Replacing incorrectly parsed value '" + parsedValue + "' with original one '" + originalValue + "'");
						jsonOut = StringHandler.replace(jsonOut, whatToReplace, withWhatToReplace);
						alreadyReplaced.put(whatToReplace, Boolean.TRUE);
					}
				}
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error while ensuring that values (" + obj + ") were correctly parsed into the JSON format:\n" + jsonOut,
					e);
		}

		return jsonOut;
	}

	private static <T extends Serializable> T getDeserializedObjectFromJSON(String json) {
		return getDeserializedObjectFromJSON(json, null);
	}

	private static <T extends Serializable> T getDeserializedObjectFromJSON(String json, HierarchicalStreamDriver driver) {
		try {
			if (driver == null)
				driver = new JettisonMappedXmlDriver();

			XStream xstream = new XStream(driver);
			@SuppressWarnings("unchecked")
			T obj = (T) xstream.fromXML(json);
			return obj;
		} catch (Exception e) {
			getLogger(ObjectCollectionConverter.class).log(Level.WARNING, "Error converting from JSON to object:\n" + json, e);
		}
		return null;
	}

	public static Map<String, String> JSONToObj(String jsonIn) {
		Serializable tmp = getObjectFromJSON(jsonIn);
		@SuppressWarnings("unchecked")
		Map<String, String> object = (Map<String, String>) tmp;
		return object;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T getObjectFromJSON(String jsonIn) {
		T obj = getDeserializedObjectFromJSON(jsonIn);
		boolean checkParsedObject = true;

		if (obj == null) {
			//	Trying to fix invalid JSON
			List<JSONFixer> fixers = Arrays.asList(
					new JSONFixer("\"\\d+\"\\d+", CoreConstants.QOUTE_MARK, CoreConstants.EMPTY),
					new JSONFixer("\".\\w+-map\"", null, "\"linked-hash-map\""),
					new JSONFixer("\"\\w+.\\w+-map\"", null, "\"linked-hash-map\""),
					new JSONFixer("\"*\\w+\"\"]}", "\"\"]}", "\"]}"),
					new JSONFixer("\\,\".......\"\\d ..\"]}", "\"", CoreConstants.EMPTY,
							new AdvancedProperty("1", "\""),
							new AdvancedProperty("13", "\"")
					),
					new JSONFixer(".\",\"\\d+d\"\\d+]}", "d\"", CoreConstants.EMPTY),
					new JSONFixer(".\",\"\\d+]}", "\",\"", "\",")
			);

			int i = 0;
			for (Iterator<JSONFixer> fixersIter = fixers.iterator(); (fixersIter.hasNext() && obj == null);) {
				JSONFixer fixer = fixersIter.next();

				boolean ableToMofidyJSON = false;
				Pattern pattern = Pattern.compile(fixer.getExpression());
				Matcher matcher = pattern.matcher(jsonIn);
				while (matcher.find()) {
					String errorCausingSection = jsonIn.substring(matcher.start(), matcher.end());
					String fixedSection = StringHandler.replace(errorCausingSection, fixer.getPattern() == null ?
							errorCausingSection :
							fixer.getPattern(),
					fixer.getReplace());
					if (!StringUtil.isEmpty(errorCausingSection) && !StringUtil.isEmpty(fixedSection)) {
						if (errorCausingSection.equals(fixedSection))
							break;

						if (fixer.getInjections() != null) {
							try {
								int lastStart = 0;
								for (AdvancedProperty injection: fixer.getInjections()) {
									int index = Integer.valueOf(injection.getId());
									fixedSection = fixedSection.substring(lastStart, index).concat(injection.getValue())
											.concat(fixedSection.substring(index));
									if (injection.isSelected())
										lastStart = index + injection.getValue().length();
								}
							} catch (Exception e) {
								getLogger(ObjectCollectionConverter.class).log(Level.WARNING, "Error while trying to inject additional symbols (" +
										fixer.getInjections() + ") to almost fixed section: " + fixedSection, e);
							}
						}

						jsonIn = StringHandler.replace(jsonIn, errorCausingSection, fixedSection);
						matcher = pattern.matcher(jsonIn);
						ableToMofidyJSON = true;
						checkParsedObject = i == 0;
					} else
						break;
				}

				if (ableToMofidyJSON)
					obj = getDeserializedObjectFromJSON(jsonIn);

				i++;
			}
		}

		if (obj == null) {
			String cleanedJSON = null;
			try {
				cleanedJSON = getCleanedJSON(jsonIn);
				obj = getDeserializedObjectFromJSON(cleanedJSON);
				checkParsedObject = false;
			} catch (Exception e) {
				getLogger(ObjectCollectionConverter.class).log(Level.WARNING, "Failed to load object from cleaned JSON: " + cleanedJSON, e);
			}
		}

		if (obj == null) {
			getLogger(ObjectCollectionConverter.class).warning("Unable to transform JSON string '" + jsonIn + "' into the object");
			return null;
		}

		if (checkParsedObject && obj instanceof Map<?, ?>) {
			try {
				Map<String, String> object = (Map<String, String>) obj;

				//	Checking the parsed object
				for (String key: object.keySet()) {
					String parsedValue = object.get(key);
					if (StringUtil.isEmpty(parsedValue) || CoreConstants.MINUS.equals(parsedValue))
						continue;

					int index = jsonIn.indexOf(parsedValue);
					if (index <= 0)
						continue;

					index--;
					int length = parsedValue.length() + 1;
					String tmp = jsonIn.substring(index, index + length);
					while (index > 0 && !tmp.startsWith(CoreConstants.COMMA) && !tmp.startsWith(CoreConstants.QOUTE_MARK)) {
						index--;
						length++;
						tmp = jsonIn.substring(index, index + length);
					}

					tmp = tmp.substring(1);
					if (!tmp.equals(parsedValue)) {
						getLogger(ObjectCollectionConverter.class).info("Value was parsed from JSON string ('" + jsonIn +
								"') incorrectly! Using value '" + tmp + "' instead of '" + parsedValue + "'");
						object.put(key, tmp);
					}
				}

				return (T) object;
			} catch (Exception e) {
				getLogger(ObjectCollectionConverter.class).log(Level.WARNING, "Error while ensuring that object (" + obj +
						") was re-constructed correctly from the JSON string:\n" + jsonIn, e);
			}
		}

		return obj;
	}

	@Override
	public VariableDataType getDataType() {
		return VariableDataType.OBJLIST;
	}

	private static String getCleanedJSON(String json) {
		try {
			int fromIndex = 0;
			String endPattern = "\"]}";
			int endIndex = json.indexOf(endPattern);
			String startPattern = "\",\"";
			int valueIndex = json.indexOf(startPattern, fromIndex);
			while (valueIndex != -1 && endIndex != -1) {
				String value = json.substring(valueIndex + startPattern.length(), endIndex);
				String fixedValue = value.trim();
				if (!StringUtil.isEmpty(value)) {
					//	Removing multiple " characters to avoid JSON syntax exceptions
					fixedValue = StringHandler.replace(fixedValue, CoreConstants.QOUTE_MARK, CoreConstants.QOUTE_SINGLE_MARK);

					if (!StringUtil.isEmpty(fixedValue))
						json = StringHandler.replace(json, value, fixedValue);
				}

				fromIndex = valueIndex;
				endIndex = json.indexOf(endPattern, fromIndex + startPattern.length() + fixedValue.length() + endPattern.length());
				valueIndex = json.indexOf(startPattern, fromIndex + 1);
			}
		} catch (Exception e) {
			String message = "Error cleaning provided JSON:\n" + json;
			getLogger(ObjectCollectionConverter.class).log(Level.WARNING, message, e);
			CoreUtil.sendExceptionNotification(message, e);
		}

		return json;
	}

}