package com.idega.bpm.pdf.handler;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.form.business.FormConverterToPDF;

@Service("formToPDFConvertingHandler")
@Scope("prototype")
public class FormToPDFConvertingHandler implements ActionHandler {
	
	/**
     * 
     */
    private static final long serialVersionUID = 7681983044074871629L;
    
	private String taskInstanceId;
	private String pdfName;
	
	@Autowired
	private FormConverterToPDF formConverterToPDF;
	
	public void execute(ExecutionContext executionContext) throws Exception {
		getFormConverterToPDF().getHashValueForGeneratedPDFFromXForm(taskInstanceId, true,getPdfName());
	}
	
	public String getTaskInstanceId() {
		return taskInstanceId;
	}

	public FormConverterToPDF getFormConverterToPDF() {
    	return formConverterToPDF;
    }

	public String getPdfName() {
		return pdfName;
	}
	
	
	public void setTaskInstanceId(String taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}
	
	
	
	public void setPdfName(String pdfName) {
		this.pdfName = pdfName;
	}


	public void setFormConverterToPDF(FormConverterToPDF formConverterToPDF) {
    	this.formConverterToPDF = formConverterToPDF;
    }
	
}
