package com.idega.bpm.xformsview.converters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import com.idega.block.process.variables.VariableDataType;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/09/17 13:09:39 $ by $Author: civilis $
 */
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class DataConvertersFactory {

	final private Map<VariableDataType, DataConverter> dataConverters;

	public DataConvertersFactory() {
		dataConverters = new HashMap<VariableDataType, DataConverter>();
	}

	public DataConverter createConverter(VariableDataType dataType) {
		return getDataConverters().get(dataType);
	}

	public Map<VariableDataType, DataConverter> getDataConverters() {
		return dataConverters;
	}

	@Autowired
	public void setInjDataConverters(List<DataConverter> injDataConverters) {

		final Map<VariableDataType, DataConverter> dataConverters = getDataConverters();

		for (DataConverter dataConverter : injDataConverters) {

			dataConverters.put(dataConverter.getDataType(), dataConverter);
		}
	}
}