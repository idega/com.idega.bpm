package com.idega.bpm.bundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.bundle.ProcessBundleResources;
import com.idega.jbpm.view.ViewResource;
import com.idega.jbpm.view.ViewResourceResolveStrategy;
import com.idega.util.StringUtil;
import com.idega.xformsmanager.business.DocumentManagerFactory;

/**
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/01/25 15:44:13 $ by
 *          $Author: civilis $
 */
@Service
@Scope("singleton")
public class ViewResourceResolveStrategyXFormImpl implements
		ViewResourceResolveStrategy {

	@Autowired
	private DocumentManagerFactory documentManagerFactory;

	public ViewResource resolve(ProcessBundleResources resources,
			String taskName, String identifier) {

		String pathWithinBundle = identifier;

		if (StringUtil.isEmpty(pathWithinBundle)) {
			throw new IllegalArgumentException(
					"Tried to resolve xform view resource, but no identifier specified");
		}

		ViewResourceXFormImpl resource = new ViewResourceXFormImpl();
		resource.setTaskName(taskName);
		resource.setDocumentManagerFactory(getDocumentManagerFactory());

		resource.setResourceLocation(resources, pathWithinBundle);

		return resource;
	}

	DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	public String getViewResourceTypeHandler() {
		return "xforms";
	}
}