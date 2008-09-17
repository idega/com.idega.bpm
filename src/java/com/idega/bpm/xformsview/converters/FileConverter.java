package com.idega.bpm.xformsview.converters;

import java.net.URI;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import com.idega.block.process.variables.VariableDataType;
import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/09/17 13:09:39 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class FileConverter implements DataConverter {
	
	private TmpFilesManager uploadsManager;
	private TmpFileResolver uploadResourceResolver;
	private static final String mappingAtt = "mapping";

	public Object convert(Element ctx) {
		
		String variableName = ctx.getAttribute(mappingAtt);
		
		Collection<URI> filesUris = getUploadsManager().getFilesUris(variableName, ctx, getUploadResourceResolver());
		return filesUris.isEmpty() ? null : filesUris.iterator().next();
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