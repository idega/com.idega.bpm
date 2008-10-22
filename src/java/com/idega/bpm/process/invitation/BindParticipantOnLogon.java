package com.idega.bpm.process.invitation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.login.business.UserLoggedInEvent;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.BPMUser;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/10/22 14:51:54 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class BindParticipantOnLogon implements ApplicationListener {
	
	private BPMFactory bpmFactory;

	public void onApplicationEvent(ApplicationEvent ae) {

		if(ae instanceof UserLoggedInEvent) {
			
			UserLoggedInEvent ule = (UserLoggedInEvent)ae;
			IWContext iwc = ule.getIWC();
			User usr = ule.getLoggedInUsr();

			BPMUser bpmUser = getBpmFactory().getBpmUserFactory().getLoggedInBPMUser(iwc, null, usr);
			
			if(bpmUser != null)
//				would associate automatically if not associated
				bpmUser.getIsAssociated(true);
		}
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}