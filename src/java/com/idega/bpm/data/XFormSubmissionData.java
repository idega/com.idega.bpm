package com.idega.bpm.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.jbpm.graph.def.ProcessDefinition;

import com.idega.block.form.data.XFormSubmission;
import com.idega.util.StringUtil;

@Entity
@Table(name = "bpm_frm_sub_data")
public class XFormSubmissionData implements Serializable {

	private static final long serialVersionUID = 787494502478778862L;

	public XFormSubmissionData() {
		super();
	}

	public XFormSubmissionData(ProcessDefinition processDefinition, XFormSubmission submission) {
		this();

		this.processDefinition = processDefinition;
		this.submission = submission;
	}

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@OneToOne
	private ProcessDefinition processDefinition;

	@OneToOne
	private XFormSubmission submission;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Collection<XFormSubmissionVariable> variables;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ProcessDefinition getProcessDefinition() {
		return processDefinition;
	}

	public void setProcessDefinition(ProcessDefinition processDefinition) {
		this.processDefinition = processDefinition;
	}

	public XFormSubmission getSubmission() {
		return submission;
	}

	public void setSubmission(XFormSubmission submission) {
		this.submission = submission;
	}

	public Collection<XFormSubmissionVariable> getVariables() {
		return variables;
	}

	public void setVariables(Collection<XFormSubmissionVariable> variables) {
		this.variables = variables;
	}

	public void addVariable(String name, String value) {
		if (StringUtil.isEmpty(name) || StringUtil.isEmpty(value))
			return;

		if (variables == null) {
			variables = new ArrayList<XFormSubmissionVariable>();
		}

		variables.add(new XFormSubmissionVariable(name, value));
	}

	@Override
	public String toString() {
		return "ID: " + getId() + ", proc. def.: " + getProcessDefinition() + ", submission: " + getSubmission() + ", variables: " + getVariables();
	}

}