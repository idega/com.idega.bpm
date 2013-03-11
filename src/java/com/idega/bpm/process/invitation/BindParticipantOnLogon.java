package com.idega.bpm.process.invitation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.login.business.UserLoggedInEvent;
import com.idega.business.IBOLookup;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.BPMUser;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $ Last modified: $Date: 2009/01/22 11:33:33 $ by $Author: civilis $
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BindParticipantOnLogon implements ApplicationListener<UserLoggedInEvent> {

	private BPMFactory bpmFactory;

	@Override
	public void onApplicationEvent(UserLoggedInEvent ae) {
		UserLoggedInEvent ule = ae;
		IWContext iwc = ule.getIWC();
		com.idega.user.data.bean.User user = ule.getLoggedInUsr();
		User usr = null;
		try {
			UserBusiness userBusiness = IBOLookup.getServiceInstance(iwc, UserBusiness.class);
			usr = userBusiness.getUser(user.getId());
		} catch (Exception e) {
			e.printStackTrace();
		}

		BPMUser bpmUser = getBpmFactory().getBpmUserFactory().getLoggedInBPMUser(iwc, null, usr);
		if (bpmUser != null)
			// would associate automatically if not associated
			bpmUser.getIsAssociated(true);
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}