package com.idega.bpm.pdf.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.data.Case;
import com.idega.block.process.message.business.MessageBusiness;
import com.idega.block.process.message.data.Message;
import com.idega.block.process.message.presentation.MessageViewer;
import com.idega.bpm.BPMConstants;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.graphics.generator.business.PDFGenerator;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.io.DownloadWriter;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.util.FileUtil;
import com.idega.util.ListUtil;
import com.idega.util.expression.ELUtil;


/**
 * Writes Case Logs to PDF
 * @author <a href="mailto:aleksandras@idega.com>Aleksandras sivkovas</a>
 * Created: 2011.04.29
 */

public class CaseLogsToPDFWriter extends DownloadWriter {

	public static final String CASE_ID_PARAMETER = "caseLogIdToDownload";

	private static final Logger LOGGER = Logger.getLogger(CaseLogsToPDFWriter.class.getName());

	private String caseId;

	@Autowired
	private PDFGenerator pdfGenerator;

	private byte[] pdfBytes = null;



	@Override
	public String getMimeType() {
		// TODO Auto-generated method stub
		return MimeTypeUtil.MIME_TYPE_PDF_1;
	}

	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		ELUtil.getInstance().autowire(this);

		this.caseId = iwc.getParameter(CASE_ID_PARAMETER);

		if (this.caseId == null) {
			LOGGER.log(Level.SEVERE, "Do not know what to download: taskInstanceId, caseId is null");
			return;
		}

		IWBundle bundle = iwc.getIWMainApplication().getBundle(BPMConstants.IW_BUNDLE_STARTER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		Layer container = new Layer();
		container.add("<link href=\"" + bundle.getVirtualPathWithFileNameString("style/case_logs_pdf_style.css") +"\" type=\"text/css\" />");

		Collection<Message> messages = this.getMessages(iwc);
		if(ListUtil.isEmpty(messages)){
			container.add(iwrb.getLocalizedString("no_messages_found", "There are no messages"));
		} else {
			for (Message msg: messages) {
				container.add(new MessageViewer(msg));
			}
		}

		pdfBytes = this.pdfGenerator.getBytesOfGeneratedPDF(iwc, container, true, true);

		setAsDownload(iwc, this.getFileName(), this.pdfBytes.length);
	}

	private Collection<Message> getMessages(IWContext iwc) {
		try {
			MessageBusiness msgBusiness = IBOLookup.getServiceInstance(iwc, MessageBusiness.class);
			return msgBusiness.findMessages( iwc.getCurrentUser(),"SYMEDAN", caseId);
		}
		catch (FinderException fe) {
			LOGGER.log(Level.SEVERE, fe.getMessage());
		}
		catch (RemoteException re) {
			LOGGER.log(Level.SEVERE, re.getMessage());
		}
		return Collections.emptyList();
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		// TODO Auto-generated method stub

		InputStream streamIn = new ByteArrayInputStream(pdfBytes);
		FileUtil.streamToOutputStream(streamIn, out);

		out.flush();
		out.close();
		out.close();
		streamIn.close();

	}

	@Override
	public String getFileName() {
		CaseBusiness caseBusines;
		try {
			caseBusines = IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), CaseBusiness.class);
			Case theCase = caseBusines.getCase(caseId);
			return "Case_" + theCase.getCaseIdentifier() + "_logs.pdf";
		} catch (IBOLookupException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (FinderException e) {
			e.printStackTrace();
		}

		return "Case_default_name_logs.pdf";
	}

}
