package com.idega.bpm;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.bpm.xform.BPMXFormPersistenceServiceImpl;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.util.CoreUtil;
import com.idega.util.expression.ELUtil;

public class IWBundleStarter implements IWBundleStartable {

	@Autowired(required = false)
	private BPMXFormPersistenceServiceImpl xformPersistenceService;

	private BPMXFormPersistenceServiceImpl getXFormPersistenceService() {
		if (xformPersistenceService == null) {
			try {
				Object proxy = ELUtil.getInstance().getBean(BPMXFormPersistenceServiceImpl.BEAN_NAME);
				xformPersistenceService = CoreUtil.getUnProxied(proxy);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return xformPersistenceService;
	}

	@Override
	public void start(IWBundle starterBundle) {
		IWMainApplicationSettings settings = starterBundle.getApplication().getSettings();
		if (settings.getBoolean("xform.bind_submissions_with_bpm", Boolean.TRUE)) {
			getXFormPersistenceService().doBindSubmissionsWithBPM();
		}
	}

	@Override
	public void stop(IWBundle starterBundle) {
	}

}