package com.idega.bpm.bundle;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import com.idega.bpm.xformsview.XFormsView;
import com.idega.documentmanager.business.DocumentManager;
import com.idega.documentmanager.business.DocumentManagerFactory;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.bundle.ProcessBundleResources;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewResource;
import com.idega.util.xml.XmlUtil;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/09/17 13:09:02 $ by $Author: civilis $
 * 
 */
public class XFormViewResource implements ViewResource {

	private String taskName;
	private View view;
	private String pathWithinBundle;
	private DocumentManagerFactory documentManagerFactory;
	private ProcessBundleResources bundleResources;

	public View store(IWMainApplication iwma) throws IOException {

		if (view == null) {

			InputStream is = null;
			
			try {
				is = getBundleResources().getResourceIS(getPathWithinBundle());
				DocumentManager documentManager = getDocumentManagerFactory()
						.newDocumentManager(iwma);
				DocumentBuilder builder = XmlUtil.getDocumentBuilder();

				Document xformXml = builder.parse(is);
				com.idega.documentmanager.business.Document form = documentManager
					.openForm(xformXml);
				
				form.setFormType(XFormsView.FORM_TYPE);
				form.save();
				
				XFormsView view = new XFormsView();
				view.setFormDocument(form);
				this.view = view;

			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				
				if(is != null)
					is.close();
			}
		}

		return view;
	}
	
	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public void setResourceLocation(ProcessBundleResources bundleResources, String pathWithinBundle) {

		this.bundleResources = bundleResources;
		this.pathWithinBundle = pathWithinBundle;
	}
	
	public DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	public void setDocumentManagerFactory(
			DocumentManagerFactory documentManagerFactory) {
		this.documentManagerFactory = documentManagerFactory;
	}

	public String getPathWithinBundle() {
		return pathWithinBundle;
	}

	public ProcessBundleResources getBundleResources() {
		return bundleResources;
	}
}