package com.idega.bpm.company.event;

import com.idega.event.UserCreatedEvent;
import com.idega.jbpm.identity.CompanyData;
import com.idega.user.data.User;

public class CompanyCreatedEvent extends UserCreatedEvent {

	private static final long serialVersionUID = -7439546891158797572L;

	private CompanyData company;

	public CompanyCreatedEvent(Object source, User user, CompanyData company) {
		super(source, user);

		this.company = company;
	}

	public CompanyData getCompany() {
		return company;
	}

}