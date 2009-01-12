package com.idega.bpm.pdf.presentation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.webdav.lib.WebdavResource;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.bpm.jsfcomponentview.BPMCapableJSFComponent;
import com.idega.bpm.jsfcomponentview.JSFComponentView;
import com.idega.bpm.pdf.servlet.BPMTaskPDFPrinter;
import com.idega.business.IBOLookup;
import com.idega.idegaweb.IWMainApplication;
import com.idega.io.MediaWritable;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.jbpm.view.ViewSubmissionImpl;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.text.Heading1;
import com.idega.presentation.ui.IFrame;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;
import com.idega.util.URLUtil;
import com.idega.util.expression.ELUtil;

/**
 * Default class that show task view from attached pdf document.
 * 
 * @author <a href="mailto:juozas@idega.com>Juozapas Zabukas</a> Created:
 * @version $Revision: 1.2 $ Last modified: $Date: 2009/01/12 11:38:28 $ by $Author: juozas $
 */
public class BPMTaskPDFViewer extends IWBaseComponent implements
        BPMCapableJSFComponent {
	
	public static final String DOCUMENT_VARIABLE_NAME = "files_pdfTaskView";
	
	private JSFComponentView view;
	
	@Autowired
	private VariablesHandler variablesHandler;
	
	@Autowired
	private BPMFactory bpmFactory;
	
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
		    IWMainApplication.getEncryptedClassName(BPMTaskPDFPrinter.class));
		uriToPDFPrinter.addParameter(BPMTaskPDFPrinter.PARAM_TASK_INSTANCE_ID,
		    view.getTaskInstanceId().toString());
		uriToPDFPrinter.addParameter(BPMTaskPDFPrinter.PARAM_VARIABLE_NAME,
			DOCUMENT_VARIABLE_NAME);
		
		IFrame frame = new IFrame("documentView", uriToPDFPrinter.toString());
		frame.setWidth("100%");
		frame.setHeight("100%");
		div.add(frame);
		
		add(div);
	}
	
	public String getDefaultDisplayName() {
		
		return IWMainApplication.getDefaultIWMainApplication()
		        .getLocalisedStringMessage("sign_document", "Sign document",
		            "com.idega.ascertia");
		
	}
	
	public String getDisplayName(Locale locale) {
		return IWMainApplication.getDefaultIWMainApplication()
		        .getLocalisedStringMessage("sign_document", "Sign document",
		            "com.idega.ascertia", locale);
	}
	
	public void setView(JSFComponentView view) {
		this.view = view;
		
	}
	
	@Override
	public Object saveState(FacesContext ctx) {
		Object values[] = new Object[4];
		values[0] = super.saveState(ctx);
		values[1] = this.view;
		return values;
	}
	
	@Override
	public void restoreState(FacesContext ctx, Object state) {
		Object values[] = (Object[]) state;
		super.restoreState(ctx, values[0]);
		this.view = (JSFComponentView) values[1];
	}
	
	public VariablesHandler getVariablesHandler() {
		if (variablesHandler == null) {
			ELUtil.getInstance().autowire(this);
		}
		return variablesHandler;
	}
	
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}
	
	public BPMFactory getBpmFactory() {
		if (bpmFactory == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmFactory;
	}
	
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
}
