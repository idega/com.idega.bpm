package com.idega.bpm.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.image.business.ImagesProviderGeneric;
import com.idega.block.image.presentation.AdvancedImage;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.slide.business.IWSlideService;
import com.idega.slide.util.WebdavExtendedResource;

/**
 * image provider by binary variables set
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/30 13:17:06 $ by $Author: civilis $
 */
@Service
@Scope("prototype")
public class ImageProviderBPMBinaryVarsImpl implements ImagesProviderGeneric {
	
	private List<BinaryVariable> binaryVariables;
	
	public int getImageCount() {
		
		// we expect, that those binary variables contain only images that we need
		return getBinaryVariables().size();
	}
	
	public List<AdvancedImage> getImagesFromTo(int startPosition, int endPosition) {
		List<BinaryVariable> binaryVariables = getBinaryVariables();
		List<AdvancedImage> result = new ArrayList<AdvancedImage>(binaryVariables.size());
		
		int realEnd = Math.min(endPosition, binaryVariables.size());
		
		for (int i = (startPosition - 1); i < realEnd; i++) {
			AdvancedImage image = null;
			BinaryVariable var = binaryVariables.get(i);
			Object persistentResource = var.getPersistentResource();
			
			if (persistentResource instanceof WebdavExtendedResource) {
				WebdavExtendedResource variableResource = (WebdavExtendedResource) persistentResource;
				image = new AdvancedImage(variableResource);
			} else if (persistentResource instanceof IWSlideService) {
				image = new AdvancedImage(var.getIdentifier());
			} else {
				Logger.getLogger(ImageProviderBPMBinaryVarsImpl.class.getName()).warning("Unkown persistent resource: " + persistentResource);
			}
			
			if (image != null) {
				result.add(image);
			}
		}
		
		return result;
	}
	
	public List<BinaryVariable> getBinaryVariables() {
		
		if (binaryVariables == null)
			binaryVariables = Collections.emptyList();
		
		return binaryVariables;
	}
	
	public ImageProviderBPMBinaryVarsImpl setBinaryVariables(
	        List<BinaryVariable> binaryVariables) {
		this.binaryVariables = binaryVariables;
		return this;
	}
}