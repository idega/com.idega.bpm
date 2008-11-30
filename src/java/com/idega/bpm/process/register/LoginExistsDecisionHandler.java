package com.idega.bpm.process.register;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.jbpm.identity.UserPersonalData;

/**
 * Checks, if user exists by username provided in UserPersonalData userName
 * property. If that's not set, personalId is used instead
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 *          Last modified: $Date: 2008/11/30 08:19:04 $ by $Author: civilis $
 */
@Service("loginExistsDecisionHandler")
@Scope("prototype")
public class LoginExistsDecisionHandler implements DecisionHandler {

	private static final long serialVersionUID = -8215519082716301605L;

	private UserPersonalData userData;

	private static final String booleanTrue = "true";
	private static final String booleanFalse = "false";

	public String decide(ExecutionContext ectx) throws Exception {

		if (getUserData() != null) {

			UserPersonalData upd = getUserData();

			String userName;

			if ((userName = upd.getUserName()) == null)
				userName = upd.getPersonalId();

			if (!LoginDBHandler.isLoginInUse(userName)) {
				return booleanFalse;
			}
		} else {
			Logger.getLogger(getClass().getName()).log(
					Level.WARNING,
					"Called locate user handler, but no user data provided. Process instance id="
							+ ectx.getProcessInstance().getId());
		}

		return booleanTrue;
	}

	public UserPersonalData getUserData() {
		return userData;
	}

	public void setUserData(UserPersonalData userData) {
		this.userData = userData;
	}
}