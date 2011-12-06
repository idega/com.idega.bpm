package com.idega.bpm.xformsview.converters;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import com.idega.block.process.variables.VariableDataType;
import com.idega.chiba.ChibaConstants;
import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.impl.BinaryVariableImpl;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/10/14 18:23:43 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class FileConverter implements DataConverter {
	
	private TmpFilesManager uploadsManager;
	private TmpFileResolver uploadResourceResolver;
	private static final String mappingAtt = ChibaConstants.MAPPING;

	public Object convert(Element ctx) {
		
		String variableName = ctx.getAttribute(mappingAtt);
		
		Collection<URI> filesUris = getUploadsManager().getFilesUris(variableName, ctx, getUploadResourceResolver());
		URI uri = filesUris.isEmpty() ? null : filesUris.iterator().next();
		
		if(uri != null) {
			ArrayList<BinaryVariable> binVars = new ArrayList<BinaryVariable>(1);
			BinaryVariableImpl binaryVariable = new BinaryVariableImpl();
			binaryVariable.setUri(uri);
			binVars.add(binaryVariable);
			
			return binVars;
			
		} else {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "No uri resolved when converting uploaded file. Variable name="+variableName);			
		}
		
		return null;
	}
	public Element revert(Object o, Element e) {
	
		Logger.getLogger(getClass().getName()).log(Level.WARNING, "UNSUPPORTED OPERATION");
		return e;
	}
	
	public VariableDataType getDataType() {
		return VariableDataType.FILE;
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
}