package com.idega.bpm.xformsview.converters.bean;

import java.io.Serializable;

import com.idega.builder.bean.AdvancedProperty;

public class JSONFixer implements Serializable {

	private static final long serialVersionUID = 1667384584772469221L;

	private String expression, pattern, replace;
	private AdvancedProperty[] injections;

	public JSONFixer(String expression, String pattern, String replace) {
		this.expression = expression;
		this.pattern = pattern;
		this.replace = replace;
	}

	public JSONFixer(String expression, String pattern, String replace, AdvancedProperty... injections) {
		this(expression, pattern, replace);

		this.injections = injections;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getReplace() {
		return replace;
	}

	public void setReplace(String replace) {
		this.replace = replace;
	}

	public AdvancedProperty[] getInjections() {
		return injections;
	}

	public void setInjections(AdvancedProperty[] injections) {
		this.injections = injections;
	}

}