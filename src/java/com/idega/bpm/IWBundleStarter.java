package com.idega.bpm;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.bpm.xform.BPMXFormPersistenceServiceImpl;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.util.CoreUtil;
import com.idega.util.expression.ELUtil;

public class IWBundleStarter implements IWBundleStartable {

	@Autowired(required = false)
	private BPMXFormPersistenceServiceImpl xformPersistenceService;

	@Autowired
	private VariableInstanceQuerier querier;

	private VariableInstanceQuerier getVariableInstanceQuerier() {
		if (querier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return querier;
	}

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
		if (starterBundle.getApplication().getSettings().getBoolean("xform.bind_submissions_with_bpm", Boolean.TRUE)) {
			getXFormPersistenceService().doBindSubmissionsWithBPM();
		}

		try {
			getVariableInstanceQuerier().loadVariables(Arrays.asList(
					BPMConstants.VAR_SUBJECT,
					BPMConstants.VAR_TEXT,
					BPMConstants.VAR_FROM,
					BPMConstants.VAR_FROM_ADDRESS
			));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop(IWBundle starterBundle) {
	}

}