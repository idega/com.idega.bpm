package com.idega.bpm.data;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Index;

@Entity
@Table(name = "bpm_frm_sub_var")
public class XFormSubmissionVariable implements Serializable {

	private static final long serialVersionUID = -2800442217051360892L;

	public XFormSubmissionVariable() {
		super();
	}

	public XFormSubmissionVariable(String name, String value) {
		this();

		name = name.trim();
		if (name.length() > 255) {
			name = name.substring(0, 255);
		}
		this.name = name;

		value = value.trim();
		if (value.length() > 255) {
			value = value.substring(0, 255);
		}
		this.value = value;
	}

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Index(name = "var_name_index", columnNames = {"name"})
	@Column(name = "name", length = 255)
	private String name;

	@Index(name = "var_value_index", columnNames = {"value"})
	@Column(name = "value", length = 255)
	private String value;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "ID: " + getId() + ", name: " + getName() + ", value: " + getValue();
	}
}