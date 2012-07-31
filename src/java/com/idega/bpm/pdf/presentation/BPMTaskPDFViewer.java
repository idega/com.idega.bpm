package com.idega.bpm.pdf.presentation;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.idega.bpm.BPMConstants;
import com.idega.bpm.jsfcomponentview.BPMCapableJSFComponent;
import com.idega.bpm.jsfcomponentview.JSFComponentView;
import com.idega.bpm.pdf.servlet.BPMTaskPDFPrinter;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.io.MediaWritable;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.PDFRenderedComponent;
import com.idega.presentation.ui.IFrame;
import com.idega.util.CoreUtil;
import com.idega.util.StringHandler;
import com.idega.util.URLUtil;
import com.idega.util.expression.ELUtil;

/**
 * Default class that show task view from attached pdf document.
 *
 * @author <a href="mailto:juozas@idega.com>Juozapas Zabukas</a> Created:
 * @version $Revision: 1.6 $ Last modified: $Date: 2009/06/11 07:18:18 $ by
 *          $Author: valdas $
 */
public class BPMTaskPDFViewer extends IWBaseComponent implements BPMCapableJSFComponent, PDFRenderedComponent {

	public static final String DOCUMENT_VARIABLE_NAME = "files_pdfTaskView";
	public static final String COMPONENT_TYPE = "BPMTaskPDFViewer";

	private JSFComponentView view;
	private Long taskInstanceId;

	@Autowired
	private BPMFactory bpmFactory;

	public BPMTaskPDFViewer() {
		super();
	}

	private BPMFactory getBPMFactory() {
		if (bpmFactory == null)
			ELUtil.getInstance().autowire(this);
		return bpmFactory;
	}

	@Override
	protected void initializeComponent(FacesContext context) {
		super.initializeComponent(context);
		initializeJSFView(context);
	}

	protected void initializeJSFView(FacesContext context) {
		Layer div = new Layer();
		div.setWidth("100%");
		div.setHeight("800");

		IWContext iwc = IWContext.getIWContext(context);

		URLUtil uriToPDFPrinter = new URLUtil(iwc.getIWMainApplication().getMediaServletURI());
		uriToPDFPrinter.addParameter(MediaWritable.PRM_WRITABLE_CLASS, IWMainApplication.getEncryptedClassName(BPMTaskPDFPrinter.class));

		Long taskIntanceId = getTaskInstanceId() != null ? getTaskInstanceId() : view.getTaskInstanceId();

		uriToPDFPrinter.addParameter(BPMTaskPDFPrinter.PARAM_TASK_INSTANCE_ID, taskIntanceId.toString());
		uriToPDFPrinter.addParameter(BPMTaskPDFPrinter.PARAM_VARIABLE_NAME, DOCUMENT_VARIABLE_NAME);

		IFrame frame = new IFrame("documentView", uriToPDFPrinter.toString());
		frame.setWidth("100%");
		frame.setHeight("100%");
		div.add(frame);

		add(div);
	}

	@Override
	public String getDefaultDisplayName() {
		IWContext iwc = CoreUtil.getIWContext();
		Locale locale = iwc == null ? Locale.ENGLISH : iwc.getCurrentLocale();
		locale = locale == null ? Locale.ENGLISH : locale;
		return getDisplayName(locale);
	}

	@Override
	@Transactional(readOnly = true)
	public String getDisplayName(Locale locale) {
		IWResourceBundle iwrb = IWMainApplication.getDefaultIWMainApplication().getBundle(BPMConstants.IW_BUNDLE_STARTER).getResourceBundle(locale);
		if (taskInstanceId == null) {
			IWContext iwc = CoreUtil.getIWContext();
			if (iwc != null) {
				HttpServletRequest request = iwc.getRequest();
				Object tiId = request.getAttribute(ProcessConstants.TASK_INSTANCE_ID);
				if (tiId instanceof Long) {
					taskInstanceId = (Long) tiId;
					iwc.getRequest().removeAttribute(ProcessConstants.TASK_INSTANCE_ID);
				}
			}
		}
		if (taskInstanceId == null)
			return iwrb.getLocalizedString("default_pdf_task_name", "Submitted document in PDF format");

		try {
			TaskInstanceW taskInstance = getBPMFactory().getTaskInstanceW(taskInstanceId);
			String systemName = taskInstance.getTaskInstance().getName();
			String localizationKey = StringHandler.stripNonRomanCharacters(systemName);
			return iwrb.getLocalizedString("pdf_task." + localizationKey, systemName);
		} catch (Exception e) {
			String message = "Error generating task name for task instance: " + taskInstanceId;
			Logger.getLogger(BPMTaskPDFViewer.class.getName()).log(Level.WARNING, message, e);
			CoreUtil.sendExceptionNotification(message, e);
		}

		return iwrb.getLocalizedString("default_pdf_task_name", "Submitted document in PDF format");
	}

	@Override
	public void setView(JSFComponentView view) {
		this.view = view;
	}

	@Override
	public Object saveState(FacesContext ctx) {
		Object values[] = new Object[3];
		values[0] = super.saveState(ctx);
		values[1] = view;
		values[2] = taskInstanceId;
		return values;
	}

	@Override
	public void restoreState(FacesContext ctx, Object state) {
		Object values[] = (Object[]) state;
		super.restoreState(ctx, values[0]);
		view = (JSFComponentView) values[1];
		taskInstanceId = (Long) values[2];
	}

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	@Override
	public boolean isPdfViewer() {
		return true;
	}

	@Override
	public void setPdfViewer(boolean pdfViewer) {}
}