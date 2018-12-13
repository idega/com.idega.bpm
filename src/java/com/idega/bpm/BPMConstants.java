package com.idega.bpm;

import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.util.CoreConstants;

public class BPMConstants {

	public static final String	IW_BUNDLE_STARTER = "com.idega.bpm",

								PDF_OF_XFORMS_PATH_IN_REPOSITORY = CoreConstants.CONTENT_PATH + "/xforms/pdf/",
								TEMP_PDF_OF_XFORMS_PATH_IN_REPOSITORY = CoreConstants.PUBLIC_PATH + "/xforms/pdf/temp/",

								TASK_CUSTOM_NAME_META_DATA = "BPM_TASK_CUSTOM_NAME",
								TASK_CUSTOM_NAME_SEPARATOR = "@BPM_TASK_CUSTOM_NAME@",

								BPM_PROCESS_HANDLER_VARIABLE = "bpm_process_handler_variable",

								PUBLIC_PROCESS = "string_processIsPublic",
								USER_ID = "string_userId",

								VAR_TEXT = "string_text",
								VAR_FROM = "string_fromPersonal",
								VAR_SUBJECT = "string_subject",
								VAR_FROM_ADDRESS = "string_fromAddress",

								VAR_MANAGER_ROLE = ProcessDefinitionW.VARIABLE_MANAGER_ROLE_NAME,

								VAR_OWNER_EMAIL = ProcessConstants.OWNER_EMAIL_ADDRESS;

}