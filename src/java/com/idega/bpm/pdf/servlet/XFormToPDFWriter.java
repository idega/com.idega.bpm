package com.idega.bpm.pdf.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpException;
import org.apache.webdav.lib.WebdavResource;

import com.idega.bpm.BPMConstants;
import com.idega.bpm.pdf.business.ProcessTaskInstanceConverterToPDF;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;
import com.idega.util.FileUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * Downloads PDF for provided XForm
 * @author <a href="mailto:valdas@idega.com>Valdas Žemaitis</a>
 * Created: 2008.05.10
 * @version $Revision: 1.1 $
 * Last modified: $Date: 2008/09/26 15:03:34 $ by $Author: valdas $
 */
public class XFormToPDFWriter extends DownloadWriter implements MediaWritable { 
	
	public static final String XFORM_ID_PARAMETER = "XFormIdToDownload";
	public static final String TASK_INSTANCE_ID_PARAMETER = "taskInstanceId";
	public static final String PATH_IN_SLIDE_PARAMETER = "pathInSlideForXFormPDF";
	public static final String DO_NOT_CHECK_EXISTENCE_OF_XFORM_IN_PDF_PARAMETER = "doNotCheckExistence";
	
	private static final Logger logger = Logger.getLogger(XFormToPDFWriter.class.getName());
	
	private WebdavResource xformInPDF = null;
	
	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		String taskInstanceId = iwc.getParameter(TASK_INSTANCE_ID_PARAMETER);
		String formId = iwc.getParameter(XFORM_ID_PARAMETER);
		if (taskInstanceId == null && formId == null) {
			logger.log(Level.SEVERE, "Do not know what to download: taskInstanceId and formId are nulls");
			return;
		}
		
		String pathInSlide = null;
		if (iwc.isParameterSet(PATH_IN_SLIDE_PARAMETER)) {
			pathInSlide = iwc.getParameter(PATH_IN_SLIDE_PARAMETER);
		
			if (pathInSlide == null || CoreConstants.EMPTY.equals(pathInSlide)) {
				logger.log(Level.SEVERE, "Unknown path for XForm in Slide");
				return;
			}
		}
		else {
			pathInSlide = BPMConstants.PDF_OF_XFORMS_PATH_IN_SLIDE;
		}
		
		boolean checkExistence = false;
		if (iwc.isParameterSet(DO_NOT_CHECK_EXISTENCE_OF_XFORM_IN_PDF_PARAMETER)) {
			checkExistence = Boolean.valueOf(iwc.getParameter(DO_NOT_CHECK_EXISTENCE_OF_XFORM_IN_PDF_PARAMETER));
		}
		
		ProcessTaskInstanceConverterToPDF pdfGenerator = null;
		try {
			pdfGenerator = ELUtil.getInstance().getBean(ProcessTaskInstanceConverterToPDF.STRING_BEAN_IDENTIFIER);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error getting Spring bean!", e);
		}
		if (pdfGenerator == null) {
			return;
		}
		
		String pathToPdf = pdfGenerator.getGeneratedPDFFromXForm(taskInstanceId, formId, pathInSlide, checkExistence);
		if (StringUtil.isEmpty(pathToPdf)) {
			logger.log(Level.SEVERE, "PDF from XForm was not generated!");
			return;
		}
		
		IWSlideService slide = null;
		try {
			slide = (IWSlideService) IBOLookup.getServiceInstance(iwc, IWSlideService.class);
		} catch (IBOLookupException e) {
			logger.log(Level.SEVERE, "Error getting IWSlideService!", e);
		}
		if (slide == null) {
			return;
		}
		
		if (!setXForm(slide, pathToPdf)) {
			logger.log(Level.SEVERE, "Error reading PDF document: " + pathToPdf);
			return;
		}
		if (xformInPDF == null || !xformInPDF.exists()) {
			return;
		}
		Long length = Long.valueOf(xformInPDF.getGetContentLength());
		setAsDownload(iwc, xformInPDF.getDisplayName(), length.intValue());
	}

	private boolean setXForm(IWSlideService slide, String pathToForm) {
		try {
			xformInPDF = slide.getWebdavResourceAuthenticatedAsRoot(pathToForm);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return xformInPDF == null ? false : true;
	}
	
	@Override
	public void writeTo(OutputStream streamOut) throws IOException {
		if (xformInPDF == null) {
			logger.log(Level.SEVERE, "Unable to get XForm");
			return;
		}
		
		InputStream streamIn = xformInPDF.getMethodData();
		FileUtil.streamToOutputStream(streamIn, streamOut);
		
		streamOut.flush();
		streamOut.close();
		streamIn.close();
	}
	
	@Override
	public String getMimeType() {
		return MimeTypeUtil.MIME_TYPE_PDF_1;
	}
	
}