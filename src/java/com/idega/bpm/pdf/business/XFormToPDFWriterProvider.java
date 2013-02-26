package com.idega.bpm.pdf.business;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.pdf.business.PDFWriterProvider;
import com.idega.bpm.pdf.servlet.XFormToPDFWriter;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class XFormToPDFWriterProvider implements PDFWriterProvider {

	private static final long serialVersionUID = 6113705258295407573L;

	@Override
	public Class<XFormToPDFWriter> getPDFWriterClass() {
		return XFormToPDFWriter.class;
	}

	@Override
	public String getFormSubmissionUniqueIdParameterName() {
		return XFormToPDFWriter.XFORM_SUBMISSION_UNIQUE_ID_PARAMETER;
	}

}
