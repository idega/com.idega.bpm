package com.idega.bpm.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.bpm.exe.DefaultBPMManagersCreator;
import com.idega.bpm.graph.def.IdegaProcessDefinition;
import com.idega.xformsmanager.business.DocumentManagerFactory;
import com.idega.idegaweb.IWBundle;
import com.idega.jbpm.bundle.ProcessBundle;
import com.idega.jbpm.bundle.ProcessBundleResources;
import com.idega.jbpm.view.ViewResource;
import com.idega.jbpm.view.ViewToTask;
import com.idega.jbpm.view.ViewToTaskType;

/**
 * Default implementation of ProcessBundle, uses XFormViewResource
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 * 
 *          Last modified: $Date: 2008/11/14 10:50:47 $ by $Author: civilis $
 * 
 */
@Scope("prototype")
@Service("defaultBPMProcessBundle")
public class DefaultBPMProcessBundle implements ProcessBundle {

	private static final String processDefinitionFileName = "processdefinition.xml";
	private static final String formsPath = "forms/";
	private static final String dotRegExp = "\\.";
	private static final String taskPrefix = "task";

	private static final String initTaskProp = "init_task";
	private static final String taskNamePostfixProp = ".name";

	private static final String XFFileNamePropertyPostfix = ".view.xforms.file_name";

	private ProcessBundleResources bundleResources;
	private IWBundle bundle;
	private String bundlePropertiesLocationWithinBundle;
	private DocumentManagerFactory documentManagerFactory;
	private ProcessDefinition pd;

	private ViewToTask viewToTaskBinder;

	public ProcessDefinition getProcessDefinition() throws IOException {

		if (pd == null) {

			InputStream pdIs = getBundleResources().getResourceIS(
					processDefinitionFileName);

			if (pdIs != null) {
				ProcessDefinition pd = IdegaProcessDefinition
						.parseXmlInputStream(pdIs);
				this.pd = pd;
			}
		}

		return pd;
	}

	public List<ViewResource> getViewResources(String taskName)
			throws IOException {

		ProcessBundleResources resources = getBundleResources();
		InputStream propertiesIs = resources.getResourceIS("bundle.properties");

		final Properties properties = new Properties();
		properties.load(propertiesIs);

		for (Entry<Object, Object> entry : properties.entrySet()) {

			if (taskName.equals(entry.getValue())) {

				String key = (String) entry.getKey();

				if (!key.startsWith(taskPrefix))
					continue;

				String taskIdentifier = key.split(dotRegExp)[0];
				String fileName = properties.getProperty(taskIdentifier
						+ XFFileNamePropertyPostfix);

				XFormViewResource resource = new XFormViewResource();
				resource.setTaskName(taskName);
				resource.setDocumentManagerFactory(getDocumentManagerFactory());
				String pathWithinBundle = formsPath + fileName;

				resource.setResourceLocation(resources, pathWithinBundle);

				ArrayList<ViewResource> viewResources = new ArrayList<ViewResource>(
						1);
				viewResources.add(resource);
				return viewResources;
			}
		}

		return null;
	}

	public IWBundle getBundle() {
		return bundle;
	}

	public void setBundle(IWBundle bundle) {
		this.bundle = bundle;
	}

	public String getBundlePropertiesLocationWithinBundle() {
		return bundlePropertiesLocationWithinBundle;
	}

	public void setBundlePropertiesLocationWithinBundle(
			String bundlePropertiesLocationWithinBundle) {
		this.bundlePropertiesLocationWithinBundle = bundlePropertiesLocationWithinBundle;
	}

	protected Properties resolveBundleProperties() throws IOException {

		InputStream propertiesIs = getBundleResources().getResourceIS(
				"bundle.properties");

		if (propertiesIs != null) {

			final Properties properties = new Properties();
			properties.load(propertiesIs);

			return properties;
		} else {

			throw new RuntimeException("Expected bundle.properties not found");
		}
	}

	public void configure(ProcessDefinition pd) {

		try {
			Properties properties = resolveBundleProperties();
			String initTaskKey = properties.getProperty(initTaskProp);
			String initTaskName = properties.getProperty(initTaskKey
					+ taskNamePostfixProp);
			Task initTask = pd.getTaskMgmtDefinition().getTask(initTaskName);
			pd.getTaskMgmtDefinition().setStartTask(initTask);

		} catch (IOException e) {
			throw new RuntimeException(
					"IOException while accessing process bundle properties");
		}
	}

	public String getManagersType() {

		return DefaultBPMManagersCreator.MANAGERS_TYPE;
	}

	public ProcessBundleResources getBundleResources() {
		return bundleResources;
	}

	public void setBundleResources(ProcessBundleResources bundleResources) {
		this.bundleResources = bundleResources;
	}

	public ViewToTask getViewToTaskBinder() {
		return viewToTaskBinder;
	}

	@Autowired
	@ViewToTaskType("xforms")
	public void setViewToTaskBinder(ViewToTask viewToTaskBinder) {
		this.viewToTaskBinder = viewToTaskBinder;
	}

	public DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	@Autowired
	public void setDocumentManagerFactory(
			DocumentManagerFactory documentManagerFactory) {
		this.documentManagerFactory = documentManagerFactory;
	}
}