package com.idega.bpm.pdf.business;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.pdf.business.PDFWriterProvider;
import com.idega.bpm.pdf.servlet.XFormToPDFWriter;

@Service
@Scope("singleton")
public class XFormToPDFWriterProvider implements PDFWriterProvider {

	private static final long serialVersionUID = 6113705258295407573L;

	public Class<XFormToPDFWriter> getPDFWriterClass() {
		return XFormToPDFWriter.class;
	}

	public String getFormSubmissionUniqueIdParameterName() {
		return XFormToPDFWriter.XFORM_SUBMISSION_UNIQUE_ID_PARAMETER;
	}

}
