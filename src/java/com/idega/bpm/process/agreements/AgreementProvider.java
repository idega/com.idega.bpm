package com.idega.bpm.process.agreements;

import java.util.Locale;

import org.directwebremoting.annotations.Param;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.spring.SpringCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.bpm.BPMConstants;
import com.idega.dwr.business.DWRAnnotationPersistance;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.presentation.IWContext;
import com.idega.util.CoreUtil;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service("bpmProcessAgreementProvider")
@RemoteProxy(creator=SpringCreator.class, creatorParams={
	@Param(name="beanName", value="bpmProcessAgreementProvider"),
	@Param(name="javascript", value="ProcessAgreementProvider")
}, name="ProcessAgreementProvider")
public class AgreementProvider implements DWRAnnotationPersistance {

	private static final String DEFAULT_AGREEMENT_DOCUMENT = "default_agreement_document";
	
	@RemoteMethod
	public String getDefaultAgreementLink() {
		IWMainApplication iwma = IWMainApplication.getDefaultIWMainApplication();
		return iwma.getSettings().getProperty(DEFAULT_AGREEMENT_DOCUMENT, new StringBuilder(getAgreementsFolder()).append("agreement.pdf").toString());
	}
	
	@RemoteMethod
	public String getDefaultAgreementLinkByLocale() {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return getDefaultAgreementLink();
		}
		
		Locale locale = iwc.getCurrentLocale();
		if (locale == null) {
			return getDefaultAgreementLink();
		}
		
		return new StringBuilder(getAgreementsFolder()).append(locale.toString()).append("/agreement.pdf").toString();
	}
	
	//	TODO: make method to get agreement for specific process and locale
	
	private String getAgreementsFolder() {
		IWMainApplication iwma = IWMainApplication.getDefaultIWMainApplication();
		IWBundle bundle = iwma.getBundle(BPMConstants.IW_BUNDLE_STARTER);
		return bundle.getVirtualPathWithFileNameString("legal/");
	}
	
}
