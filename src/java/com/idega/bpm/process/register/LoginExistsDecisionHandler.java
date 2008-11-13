package com.idega.bpm.process.register;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.util.expression.ELUtil;

/**
 * Checks, if user exists by username provided in UserPersonalData userName property. If that's not set, personalId is used instead
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/11/13 15:08:12 $ by $Author: juozas $
 */
@Service("loginExistsDecisionHandler")
@Scope("prototype")
public class LoginExistsDecisionHandler implements DecisionHandler {

	private static final long serialVersionUID = -8215519082716301605L;

	private UserPersonalData userDataExp;

	private static final String booleanTrue = 	"true";
	private static final String booleanFalse = 	"false";
	
	public String decide(ExecutionContext ectx) throws Exception {
		
		ELUtil.getInstance().autowire(this);
		
		if(getUserDataExp() != null) {
			
			UserPersonalData upd = getUserDataExp();//(UserPersonalData)JbpmExpressionEvaluator.evaluate(getUserDataExp(), ectx);
			
			String userName;
			
			if((userName = upd.getUserName()) == null)
				userName = upd.getPersonalId();
			
			if(!LoginDBHandler.isLoginInUse(userName)) {
				return booleanFalse;
			}
		}
		
		return booleanTrue;
	}

	public UserPersonalData getUserDataExp() {
		return userDataExp;
	}

	public void setUserDataExp(UserPersonalData userDataExp) {
		this.userDataExp = userDataExp;
	}
}