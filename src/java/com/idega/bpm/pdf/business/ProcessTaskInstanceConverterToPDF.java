package com.idega.bpm.pdf.business;

public interface ProcessTaskInstanceConverterToPDF {

	public static final String STRING_BEAN_IDENTIFIER = "processTaskInstanceConverterToPDF";
	
	public String getGeneratedPDFFromXForm(String taskInstanceId, String formId, String formSubmitionId, String uploadPath, String pdfName, boolean checkExistence);
	
	public String getHashValueForGeneratedPDFFromXForm(String taskInstanceId, boolean checkExistence);
	
}
