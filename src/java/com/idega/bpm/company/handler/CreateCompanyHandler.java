package com.idega.bpm.company.handler;

import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.event.CompanyCreatedEvent;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.identity.CompanyData;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.jbpm.identity.authentication.CreateUserHandler;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.expression.ELUtil;

@Service(CreateCompanyHandler.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateCompanyHandler extends CreateUserHandler {

	private static final long serialVersionUID = 6987169880677900872L;

	public static final String BEAN_NAME = "createCompanyHandler";

	private UserPersonalData userData;
	private CompanyData companyData;

	@Override
	public void execute(ExecutionContext context) throws Exception {
		if (getUserData() == null || getCompanyData() == null) {
			throw new RuntimeException("Not enough data to create company!");
		}

		setPublishEvent(Boolean.FALSE);
		try {
			super.execute(context);
		} catch(Exception e) {
			throw new RuntimeException("Error creating user!", e);
		}

		User createdUser = null;
		try {
			createdUser = getUser();
		} catch(Exception e) {
			throw new RuntimeException("User was not found by personal id: " + getUserData().getPersonalId(), e);
		}
		if (createdUser == null) {
			throw new RuntimeException("User was not found by personal id: " + getUserData().getPersonalId());
		}

		ELUtil.getInstance().publishEvent(
			new CompanyCreatedEvent(
				this,
				createdUser,
				getCompanyData().getSsn(),
				getCompanyData().getName(),
				getCompanyData().getAddress(),
				getCompanyData().getPostalCode()
			)
		);
	}

	private User getUser() throws Exception {
		UserBusiness userBusiness = IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), UserBusiness.class);
		return userBusiness.getUser(getUserData().getPersonalId());
	}

	@Override
	public UserPersonalData getUserData() {
		return userData;
	}

	@Override
	public void setUserData(UserPersonalData userData) {
		this.userData = userData;
	}

	public CompanyData getCompanyData() {
		return companyData;
	}

	public void setCompanyData(CompanyData companyData) {
		this.companyData = companyData;
	}
}