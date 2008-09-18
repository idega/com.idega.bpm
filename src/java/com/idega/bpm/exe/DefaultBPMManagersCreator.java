package com.idega.bpm.exe;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMManagersFactory;
import com.idega.jbpm.exe.ProcessManager;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/18 17:11:10 $ by $Author: civilis $
 */
@Scope("singleton")
@Service(DefaultBPMManagersCreator.BEAN_IDENTIFIER)
public class DefaultBPMManagersCreator implements BPMManagersFactory {
	
	public static final String MANAGERS_TYPE = "default";
	static final String BEAN_IDENTIFIER = "defaultBPMManagersCreator";
	private ProcessManager processManager;
	
	public ProcessManager getProcessManager() {
		
		return processManager;
	}
	
	public String getManagersType() {
		
		return MANAGERS_TYPE; 
	}
	
	public String getBeanIdentifier() {

		return BEAN_IDENTIFIER;
	}

	@Resource(name="defaultBpmProcessManager")
	public void setProcessManager(ProcessManager processManager) {
		this.processManager = processManager;
	}
}