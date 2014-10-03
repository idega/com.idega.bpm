package com.idega.bpm.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.image.business.ImagesProviderGeneric;
import com.idega.block.image.presentation.AdvancedImage;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jackrabbit.bean.JackrabbitRepositoryItem;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.repository.RepositoryService;
import com.idega.repository.bean.RepositoryItem;
import com.idega.user.data.bean.User;
import com.idega.util.CoreConstants;

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

	@Override
	public int getImageCount() {

		// we expect, that those binary variables contain only images that we need
		return getBinaryVariables().size();
	}

	@Override
	public List<AdvancedImage> getImagesFromTo(int startPosition, int endPosition) {
		List<BinaryVariable> binaryVariables = getBinaryVariables();
		List<AdvancedImage> result = new ArrayList<AdvancedImage>(binaryVariables.size());

		int realEnd = Math.min(endPosition, binaryVariables.size());

		AccessController accessController = IWMainApplication.getDefaultIWMainApplication().getAccessController();
		User superAdmin = null;
		try {
			superAdmin = accessController.getAdministratorUser();
		} catch (Exception e) {}
		for (int i = (startPosition - 1); i < realEnd; i++) {
			AdvancedImage image = null;
			BinaryVariable var = binaryVariables.get(i);
			Object persistentResource = var.getPersistentResource();

			if (persistentResource instanceof RepositoryItem) {
				RepositoryItem variableResource = (RepositoryItem) persistentResource;

				String path = variableResource.getPath();
				if (path != null && !path.startsWith(CoreConstants.WEBDAV_SERVLET_URI)) {
					path = CoreConstants.WEBDAV_SERVLET_URI.concat(path);
					variableResource = new JackrabbitRepositoryItem(path, superAdmin);
				}

				image = new AdvancedImage(variableResource);
			} else if (persistentResource instanceof RepositoryService) {
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