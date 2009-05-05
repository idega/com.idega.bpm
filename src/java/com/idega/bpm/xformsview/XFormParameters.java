package com.idega.bpm.xformsview;

import java.util.Collections;
import java.util.Map;

import com.idega.jbpm.exe.ProcessConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/05/05 14:07:41 $ by $Author: civilis $
 */
public class XFormParameters {
	
	private Map<String, String> params;
	
	public XFormParameters(Map<String, String> params) {
		
		if (params == null)
			params = Collections.emptyMap();
		
		this.params = params;
	}
	
	public Long getTaskInstance() {
		
		final Long taskInstanceId;
		
		if (params.containsKey(ProcessConstants.TASK_INSTANCE_ID)) {
			
			taskInstanceId = new Long(params
			        .get(ProcessConstants.TASK_INSTANCE_ID));
		} else
			taskInstanceId = null;
		
		return taskInstanceId;
	}
}