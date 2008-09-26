package com.idega.bpm.pdf.business;

public interface ProcessTaskInstanceConverterToPDF {

	public static final String STRING_BEAN_IDENTIFIER = "processTaskInstanceConverterToPDF";
	
	public String getGeneratedPDFFromXForm(String taskInstanceId, String formId, String uploadPath, boolean checkExistence);
	
}
