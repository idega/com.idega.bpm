package com.idega.bpm.process.register;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;

import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.util.expression.ELUtil;

/**
 * Checks, if user exists by username provided in UserPersonalData userName property. If that's not set, personalId is used instead
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/09/18 17:11:40 $ by $Author: civilis $
 */
public class LoginExistsDecisionHandler implements DecisionHandler {

	private static final long serialVersionUID = -8215519082716301605L;

	private String userDataExp;

	private static final String booleanTrue = 	"true";
	private static final String booleanFalse = 	"false";
	
	public String decide(ExecutionContext ectx) throws Exception {
		
		ELUtil.getInstance().autowire(this);
		
		if(getUserDataExp() != null) {
			
			UserPersonalData upd = (UserPersonalData)JbpmExpressionEvaluator.evaluate(getUserDataExp(), ectx);
			
			String userName;
			
			if((userName = upd.getUserName()) == null)
				userName = upd.getPersonalId();
			
			if(!LoginDBHandler.isLoginInUse(userName)) {
				return booleanFalse;
			}
		}
		
		return booleanTrue;
	}

	public String getUserDataExp() {
		return userDataExp;
	}

	public void setUserDataExp(String userDataExp) {
		this.userDataExp = userDataExp;
	}
}