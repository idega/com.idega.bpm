package com.idega.bpm;

import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.util.CoreConstants;

public class BPMConstants {

	public static final String IW_BUNDLE_STARTER = "com.idega.bpm";

	public static final String PDF_OF_XFORMS_PATH_IN_SLIDE = CoreConstants.CONTENT_PATH + "/xforms/pdf/";
	public static final String TEMP_PDF_OF_XFORMS_PATH_IN_SLIDE = CoreConstants.PUBLIC_PATH + "/xforms/pdf/temp/";

	public static final String TASK_CUSTOM_NAME_META_DATA = "BPM_TASK_CUSTOM_NAME",
								TASK_CUSTOM_NAME_SEPARATOR = "@BPM_TASK_CUSTOM_NAME@",

								BPM_PROCESS_HANDLER_VARIABLE = "bpm_process_handler_variable",

								PUBLIC_PROCESS = "string_processIsPublic",

								VAR_TEXT = "string_text",
								VAR_FROM = "string_fromPersonal",
								VAR_SUBJECT = "string_subject",
								VAR_FROM_ADDRESS = "string_fromAddress",

								VAR_MANAGER_ROLE = ProcessDefinitionW.VARIABLE_MANAGER_ROLE_NAME;
}