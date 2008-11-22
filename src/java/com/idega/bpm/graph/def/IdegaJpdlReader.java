package com.idega.bpm.graph.def;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dom4j.Element;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.InputSource;

import com.idega.util.LocaleUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.messages.MessageResourceFactory;

/**
 *
 * 
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version Revision: 1.0 
 *
 * Last modified: Oct 9, 2008 by Author: Anton 
 *
 */

public class IdegaJpdlReader extends JpdlXmlReader {

	private static final long serialVersionUID = -4956191449989922971L;
	
//	private Map<Locale, String> localizedStrings;
	private InputSource processDefinitionXMLSource;
	
	private static final String ROLE = "role";
	private static final String NAME = "name";
	private static final String LANG = "lang";
	private static final String LABEL = "label";
	private static final String ROLES_LOCALIZATION_TAG = "roles-localization";
	
    @Autowired
	private MessageResourceFactory messageFactory;
	
	private static final Logger log = Logger.getLogger(IdegaJpdlReader.class.getName());
	
//	public Map<Locale, String> getAllLocalizedStrings() {
//		if(localizedStrings == null) {
//			localizedStrings = new HashMap<Locale, String>();
//		}
//		return localizedStrings;
//	}

//	public void setLocalizedStrings(
//			Map<Locale, String> localizedStrings) {
//		this.localizedStrings = localizedStrings;
//	}

	public InputSource getProcessDefinitionXMLSource() {
		return processDefinitionXMLSource;
	}

	public void setProcessDefinitionXMLSource(InputSource processDefinitionXMLSource) {
		this.processDefinitionXMLSource = processDefinitionXMLSource;
	}

	public IdegaJpdlReader(InputSource inputSource) {
		super(inputSource);
		this.inputSource = inputSource;
		
	}
	
    @Override
	public ProcessDefinition readProcessDefinition() {
    	ProcessDefinition procDef = super.readProcessDefinition();
    	readMapStrings();
    	return procDef;
    }
    
    @SuppressWarnings("unchecked")
	protected void readMapStrings() {
    	Element root = null;
    	try {
			root = document.getRootElement();
			Element localization = (Element) root.elements(ROLES_LOCALIZATION_TAG).iterator().next();
			List<Element> roles = localization.elements(ROLE);
			
			Map<Locale, Map<Object, Object>> localisedRoles = new HashMap<Locale, Map<Object, Object>>();
			
			for(Element role : roles) {
				String roleName = role.attributeValue(NAME);
				List<Element> labels = role.elements(LABEL);
				
				for(Element roleLabel : labels) {
					String localeStr = roleLabel.attributeValue(LANG);
					Locale locale = LocaleUtil.getLocale(localeStr);
					String localizedLabel = roleLabel.getTextTrim();
					
					Map<Object, Object> roleLabels = localisedRoles.get(locale);
					if(roleLabels == null) {
						roleLabels = new HashMap<Object, Object>();
						roleLabels.put(roleName, localizedLabel);
						localisedRoles.put(locale, roleLabels);
					} else {
						roleLabels.put(roleName, localizedLabel); 
					}
				}
			}
			
//	    	MessageResource resource = getMessageFactory().getResourceByIdentifier(IWSlideResourceBundle.RESOURCE_IDENTIFIER);
			for(Locale locale : localisedRoles.keySet()) {
				getMessageFactory().setLocalisedMessages(localisedRoles.get(locale), null, locale);
//				resource.setMessages(localisedRoles.get(locale), null, locale);
			}
    	} catch(NoSuchElementException e) {
    		log.log(Level.WARNING, root.attributeValue(NAME) + " - process definition has no role localization tags");
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
        
    public MessageResourceFactory getMessageFactory() {
		if(messageFactory == null) {
			ELUtil.getInstance().autowire(this);
			return messageFactory;
		} else {
			return messageFactory;
		}
	}
}
