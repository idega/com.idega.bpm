package com.idega.bpm.process.agreements;

import java.util.Locale;

import org.directwebremoting.annotations.Param;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.spring.SpringCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.dwr.business.DWRAnnotationPersistance;
import com.idega.idegaweb.IWMainApplication;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service("bpmProcessAgreementProvider")
@RemoteProxy(creator=SpringCreator.class, creatorParams={
	@Param(name="beanName", value="bpmProcessAgreementProvider"),
	@Param(name="javascript", value="ProcessAgreementProvider")
}, name="ProcessAgreementProvider")
public class AgreementProvider implements DWRAnnotationPersistance {

	private static final String DEFAULT_AGREEMENT_DOCUMENT = "default_agreement_document";
	private static final String AGREEMENT_FILE_NAME = "Agreement.pdf";
	
	@RemoteMethod
	public String getDefaultAgreementLink() {
		IWMainApplication iwma = IWMainApplication.getDefaultIWMainApplication();
		return iwma.getSettings().getProperty(DEFAULT_AGREEMENT_DOCUMENT, new StringBuilder(getAgreementsFolder()).append(AGREEMENT_FILE_NAME).toString());
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
				
		return new StringBuilder(getAgreementsFolder()).append(locale.toString()).append(CoreConstants.SLASH).append(AGREEMENT_FILE_NAME).toString();
	}
	
	@RemoteMethod
	public String getAgreementForProcess(String processName) {
		if (StringUtil.isEmpty(processName)) {
			return getDefaultAgreementLinkByLocale();
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return getDefaultAgreementLink();
		}
		
		Locale locale = iwc.getCurrentLocale();
		if (locale == null) {
			return getDefaultAgreementLink();
		}
				
		String uriInRepository = new StringBuilder(getAgreementsFolder()).append(processName).append(CoreConstants.SLASH).append(locale.toString())
			.append(CoreConstants.SLASH).append(AGREEMENT_FILE_NAME).toString();
		
		URIUtil uri = new URIUtil(IWMainApplication.getDefaultIWMainApplication().getMediaServletURI());
		uri.setParameter(MediaWritable.PRM_WRITABLE_CLASS, IWMainApplication.getEncryptedClassName(DownloadWriter.class));
		uri.setParameter(DownloadWriter.PRM_RELATIVE_FILE_PATH, uriInRepository);
		uri.setParameter(DownloadWriter.PRM_FILE_NAME, AGREEMENT_FILE_NAME);
		
		return uri.getUri();
	}
	
	private String getAgreementsFolder() {
		return CoreConstants.WEBDAV_SERVLET_URI + CoreConstants.PUBLIC_PATH + "/bpm/legal/";
	}
}