package com.idega.bpm.xformsview.converters;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.idega.block.process.variables.VariableDataType;
import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.BinaryVariablesHandler;
import com.idega.jbpm.variables.impl.BinaryVariableImpl;
import com.idega.util.ListUtil;
import com.idega.util.xml.XPathUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/10/14 18:23:43 $ by $Author: civilis $
 */

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class FilesConverter implements DataConverter {
	
	private static final Logger LOGGER = Logger.getLogger(FilesConverter.class.getName());
	
	private TmpFilesManager uploadsManager;
	private TmpFileResolver uploadResourceResolver;
	private static final String mappingAtt = "mapping";
	private final XPathUtil entriesXPUT;
	
	@Autowired
	private BinaryVariablesHandler binaryVariablesHandler;
	
	public FilesConverter() {
		entriesXPUT = new XPathUtil("./entry");
	}

	public Object convert(Element ctx) {
		
		String variableName = ctx.getAttribute(mappingAtt);
		
		Collection<URI> filesUris = getUploadsManager().getFilesUris(variableName, ctx, getUploadResourceResolver());
		
		if(filesUris != null && !filesUris.isEmpty()) {
		
			ArrayList<BinaryVariable> binVars = new ArrayList<BinaryVariable>(filesUris.size());
			
			for(URI fileUri : filesUris) {
				
				String description = getDescriptionByUri(variableName, ctx, fileUri);
				
				BinaryVariableImpl binVar = new BinaryVariableImpl();
				binVar.setDescription(description);
				binVar.setUri(fileUri);
				
				binVars.add(binVar);
			}
			
			return binVars;
		}
		
		return null;
	}
	
	private String getDescriptionByUri(String identifier, Object resource, URI uri) {
		
		if(!(resource instanceof Node)) {	
			LOGGER.log(Level.WARNING, "Wrong resource provided. Expected of type "+Node.class.getName()+", but got "+resource.getClass().getName());
			return null;
		}
		
		String desc = null;
		
		Node instance = (Node)resource;
		Element node = getUploadsElement(identifier, instance);
		NodeList entries;
		
		entries = entriesXPUT.getNodeset(node);
		
		if(entries != null) {
			
			String uriStrMatch = uri.toString();
			
			for (int i = 0; i < entries.getLength(); i++) {
				
				String uriStr = entries.item(i).getChildNodes().item(0).getTextContent();
		    	
				if(uriStrMatch.equals(uriStr)) {
					
					Node descNode = entries.item(i).getChildNodes().item(1);
					
					if(descNode != null) {
						desc = descNode.getTextContent();
						break;
					}
				}
			}
		}
		
		return desc;
	}
	
	protected Element getUploadsElement(String identifier, Node context) {
		if(context instanceof Element && identifier.equals(((Element)context).getAttribute("mapping"))) {	
			return (Element)context;
		} else {
			return null;
		}
	}
	
	public Element revert(Object o, Element e) {
		if (o instanceof String) {
			NodeList children = e.getChildNodes();
			if (children == null || children.getLength() == 0) {
				return e;
			}
			
			List<String> variablesRepresentation = null;
			try {
				variablesRepresentation = getBinaryVariablesHandler().convertToBinaryVariablesRepresentation((String) o);
			} catch (Exception ex) {
				LOGGER.warning("Unable to convert string " + o + " to representation of binary variables");
			}
			if (ListUtil.isEmpty(variablesRepresentation)) {
				return e;
			}
			
			List<Attr> files = new ArrayList<Attr>();
			for (int i = 0; i < children.getLength(); i++) {
				Node n = children.item(i);
				NamedNodeMap att = n.getAttributes();
				if (att != null) {
					Node fileName = att.getNamedItem("filename");
					if (fileName instanceof Attr) {
						files.add((Attr) fileName);
					}
				}
			}
			
			if (files.size() < variablesRepresentation.size()) {
				return e;
			}
			
			int index = 0;
			for (String representation: variablesRepresentation) {
				BinaryVariable variable = null;
				try {
					variable = getBinaryVariablesHandler().convertToBinaryVariable(representation);
				} catch (Exception ex) {
					LOGGER.warning("Unable to convert string " + o + " to " + BinaryVariable.class);
				}
					
				if (variable != null) {
					Attr attr = files.get(index);
					attr.setValue(variable.getFileName());
				}
				
				index++;
			}
			
			return e;
		}
		
		LOGGER.warning("UNSUPPORTED OPERATION");
		return e;
	}
	
	public VariableDataType getDataType() {
		return VariableDataType.FILES;
	}
	
	public TmpFilesManager getUploadsManager() {
		return uploadsManager;
	}
	
	@Autowired
	public void setUploadsManager(TmpFilesManager uploadsManager) {
		this.uploadsManager = uploadsManager;
	}
	public TmpFileResolver getUploadResourceResolver() {
		return uploadResourceResolver;
	}
	
	@Autowired
	public void setUploadResourceResolver(@TmpFileResolverType("xformVariables")
			TmpFileResolver uploadResourceResolver) {
		this.uploadResourceResolver = uploadResourceResolver;
	}

	public BinaryVariablesHandler getBinaryVariablesHandler() {
		return binaryVariablesHandler;
	}

	public void setBinaryVariablesHandler(BinaryVariablesHandler binaryVariablesHandler) {
		this.binaryVariablesHandler = binaryVariablesHandler;
	}
}