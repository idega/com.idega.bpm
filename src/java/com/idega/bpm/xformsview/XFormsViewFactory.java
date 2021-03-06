package com.idega.bpm.xformsview;

import java.util.Collection;
import java.util.List;

import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.variables.Converter;
import com.idega.jbpm.view.TaskView;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewFactory;
import com.idega.jbpm.view.ViewFactoryType;
import com.idega.jbpm.view.ViewToTask;
import com.idega.util.StringUtil;
import com.idega.xformsmanager.business.DocumentManagerFactory;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 *          Last modified: $Date: 2009/01/27 11:31:13 $ by $Author: civilis $
 */
@Scope("singleton")
@ViewFactoryType(XFormsView.VIEW_TYPE)
@Repository("process_xforms_viewFactory")
public class XFormsViewFactory implements ViewFactory, IXFormViewFactory {

	private DocumentManagerFactory documentManagerFactory;
	private Converter converter;
	@Autowired
	private ViewToTask viewToTask;
	@Autowired
	private BPMDAO BPMDAO;

	@Override
	public View getView(String viewIdentifier, boolean submitable) {

		if (StringUtil.isEmpty(viewIdentifier))
			throw new NullPointerException("View identifier not provided");

		XFormsView view = getXFormsView();
		view.setViewId(viewIdentifier);
		view.setSubmitable(submitable);

		return view;
	}

	@Override
	public XFormsViewSubmission getViewSubmission() {

		XFormsViewSubmission view = new XFormsViewSubmission();
		view.setConverter(getConverter());

		return view;
	}

	@Override
	public XFormsView getXFormsView() {

		XFormsView view = new XFormsView();
		view.setDocumentManagerFactory(getDocumentManagerFactory());
		view.setConverter(getConverter());
		// view.setViewToTask(getViewToTask());

		return view;
	}

	@Override
	public TaskView getTaskView(Task task) {

		XFormsTaskView view = new XFormsTaskView(task);
		view.setDocumentManagerFactory(getDocumentManagerFactory());
		view.setConverter(getConverter());
		view.setViewToTask(getViewToTask());

		return view;
	}

	@Override
	public String getViewType() {
		return XFormsView.VIEW_TYPE;
	}

	public DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	@Autowired
	public void setDocumentManagerFactory(
			DocumentManagerFactory documentManagerFactory) {
		this.documentManagerFactory = documentManagerFactory;
	}

	public Converter getConverter() {
		return converter;
	}

	@Autowired
	public void setConverter(Converter converter) {
		this.converter = converter;
	}

	/*
	 * protected PersistenceManager getPersistenceManager() { return
	 * persistenceManager; }
	 *
	 * @Autowired
	 *
	 * @XFormPersistenceType("slide") public void
	 * setPersistenceManager(PersistenceManager persistenceManager) {
	 * this.persistenceManager = persistenceManager; }
	 */

	class XFormsTaskView extends XFormsView implements TaskView {

		private final Task task;

		XFormsTaskView(Task task) {
			this.task = task;
		}

		@Override
		public Task getTask() {
			return task;
		}
	}

	public ViewToTask getViewToTask() {
		return viewToTask;
	}

	public void setViewToTask(ViewToTask viewToTask) {
		this.viewToTask = viewToTask;
	}

	@Override
	@Transactional(readOnly = true)
	public Multimap<Long, TaskView> getAllViewsByProcessDefinitions(Collection<Long> processDefinitionsIds) {
		List<Object[]> procTaskViews = getBPMDAO().getProcessTasksViewsInfos(processDefinitionsIds, XFormsView.VIEW_TYPE);
		HashMultimap<Long, TaskView> pdsViews = HashMultimap.create();

		for (Object[] objects : procTaskViews) {

			Task task = (Task) objects[0];
			String viewIdentifier = (String) objects[1];

			TaskView view = getTaskView(task);
			view.setViewId(viewIdentifier);

			pdsViews.put(task.getProcessDefinition().getId(), view);
		}

		return pdsViews;
	}

	public BPMDAO getBPMDAO() {
		return BPMDAO;
	}

	public void setBPMDAO(BPMDAO bpmdao) {
		BPMDAO = bpmdao;
	}
}