package com.idega.bpm.pdf.presentation;

import java.util.Locale;

import javax.faces.context.FacesContext;

import com.idega.bpm.jsfcomponentview.BPMCapableJSFComponent;
import com.idega.bpm.jsfcomponentview.JSFComponentView;
import com.idega.bpm.pdf.servlet.BPMTaskPDFPrinter;
import com.idega.idegaweb.IWMainApplication;
import com.idega.io.MediaWritable;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.ui.IFrame;
import com.idega.util.URLUtil;

/**
 * Default class that show task view from attached pdf document.
 * 
 * @author <a href="mailto:juozas@idega.com>Juozapas Zabukas</a> Created:
 * @version $Revision: 1.5 $ Last modified: $Date: 2009/01/30 13:56:40 $ by
 *          $Author: civilis $
 */
public class BPMTaskPDFViewer extends IWBaseComponent implements
		BPMCapableJSFComponent {

	public static final String DOCUMENT_VARIABLE_NAME = "files_pdfTaskView";
	public static final String COMPONENT_TYPE = "BPMTaskPDFViewer";

	private JSFComponentView view;
	private Long taskInstanceId;

	public BPMTaskPDFViewer() {
		super();
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

		URLUtil uriToPDFPrinter = new URLUtil(iwc.getIWMainApplication()
				.getMediaServletURI());
		uriToPDFPrinter.addParameter(MediaWritable.PRM_WRITABLE_CLASS,
				IWMainApplication
						.getEncryptedClassName(BPMTaskPDFPrinter.class));

		Long taskIntanceId = getTaskInstanceId() != null ? getTaskInstanceId()
				: view.getTaskInstanceId();

		uriToPDFPrinter.addParameter(BPMTaskPDFPrinter.PARAM_TASK_INSTANCE_ID,
				taskIntanceId.toString());
		uriToPDFPrinter.addParameter(BPMTaskPDFPrinter.PARAM_VARIABLE_NAME,
				DOCUMENT_VARIABLE_NAME);

		IFrame frame = new IFrame("documentView", uriToPDFPrinter.toString());
		frame.setWidth("100%");
		frame.setHeight("100%");
		div.add(frame);

		add(div);
	}

	public String getDefaultDisplayName() {

//		return IWMainApplication.getDefaultIWMainApplication()
//				.getLocalisedStringMessage("sign_document", "Sign document",
//						"com.idega.ascertia");
		
//		TODO: resolve from process (in view, not here)
		
		return "Útgefið íbúakort til prentunar";
	}

	public String getDisplayName(Locale locale) {

//		TODO: resolve from process (in view, not here)
		
		return locale.equals(new Locale("en")) ? "Issued Parking Card for printing" : "Útgefið íbúakort til prentunar";
//		
//		
//		return IWMainApplication.getDefaultIWMainApplication()
//				.getLocalisedStringMessage("sign_document", "Sign document",
//						"com.idega.ascertia", locale);
	}

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
}