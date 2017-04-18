package com.idega.bpm.xform;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.form.business.XFormPersistenceService;
import com.idega.block.form.data.XForm;
import com.idega.block.form.data.XFormSubmission;
import com.idega.block.form.data.dao.XFormsDAO;
import com.idega.block.form.event.FormSavedEvent;
import com.idega.bpm.data.XFormSubmissionData;
import com.idega.bpm.data.XFormSubmissionVariable;
import com.idega.core.business.DefaultSpringBean;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;

@Service(BPMXFormPersistenceServiceImpl.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BPMXFormPersistenceServiceImpl extends DefaultSpringBean implements XFormPersistenceService, ApplicationListener<FormSavedEvent> {

	public static final String BEAN_NAME = "bpmXFormPersistenceServiceImpl";

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private XFormsDAO xformsDAO;

	@Override
	public void onApplicationEvent(FormSavedEvent event) {
		doRegisterSavedForm(event.getSubmissionId());
	}

	public void doBindSubmissionsWithBPM() {
		List<XFormSubmission> submissions = null;
		try {
			submissions = xformsDAO.getResultListByInlineQuery(
					"from " + XFormSubmission.class.getName() + " s where s.provider is null",
					XFormSubmission.class
			);

			if (ListUtil.isEmpty(submissions)) {
				return;
			}

			for (XFormSubmission submission: submissions) {
				doRegisterSavedForm(submission);
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error binding XForm submissions with BPM", e);
		}
	}

	@Transactional(readOnly = false)
	private void doRegisterSavedForm(Long submissionId) {
		if (submissionId == null) {
			getLogger().warning("Submission ID is not provided");
			return;
		}

		XFormSubmission submission = xformsDAO.find(XFormSubmission.class, submissionId);
		doRegisterSavedForm(submission);
	}

	@Transactional(readOnly = false)
	public void doRegisterSavedForm(XFormSubmission submission) {
		if (submission == null) {
			getLogger().warning("Submission is not provided");
			return;
		}

		if (submission.getSubmissionId() == null) {
			getLogger().warning("Submission is not persisted");
			return;
		}

		if (submission.getProvider() != null) {
			return;
		}

		XForm form = submission.getXform();
		String procDefName = form.getJBPMProcessDefinitionName();
		if (StringUtil.isEmpty(procDefName)) {
			getLogger().warning("Unknown process definition name for submission: " + submission);
			return;
		}
		if ("standalone".equals(procDefName)) {
			getLogger().info("Standalone form, no JBPM proc. definition exist for it");
			return;
		}

		ProcessDefinitionW procDef = null;
		try {
			procDef = bpmFactory.getProcessDefinitionW(procDefName);
		} catch (Exception e) {
			getLogger().warning("Error loading proc. def. by name: " + procDefName);
		}
		if (procDef == null) {
			try {
				submission.setProvider(Long.valueOf(0));
				xformsDAO.merge(submission);
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error setting provider=0 for submission " + submission, e);
			}

			getLogger().warning("Unable to find process definition by name '" + procDefName + "' for submission: " + submission);
			return;
		}

		Map<String, String> variables = submission.getVariables();
		if (MapUtil.isEmpty(variables)) {
			return;
		}

		XFormSubmissionData data = new XFormSubmissionData(procDef.getProcessDefinition(), submission);
		for (Map.Entry<String, String> entry: variables.entrySet()) {
			data.addVariable(entry.getKey(), entry.getValue());
		}
		xformsDAO.persist(data);

		if (data.getId() == null) {
			getLogger().warning("Unable to store submission data: " + data);
			return;
		}

		submission.setProvider(data.getId());
		xformsDAO.merge(submission);
	}

	@Override
	public Map<String, String> getVariables(Long identifier) {
		if (identifier == null) {
			return Collections.emptyMap();
		}

		XFormSubmissionData data = xformsDAO.find(XFormSubmissionData.class, identifier);
		if (data == null) {
			getLogger().warning("No data found by ID: " + identifier);
			return Collections.emptyMap();
		}

		Collection<XFormSubmissionVariable> variables = data.getVariables();
		if (ListUtil.isEmpty(variables)) {
			return Collections.emptyMap();
		}

		Map<String, String> vars = new HashMap<String, String>();
		for (XFormSubmissionVariable var: variables) {
			vars.put(var.getName(), var.getValue());
		}
		return vars;
	}

}