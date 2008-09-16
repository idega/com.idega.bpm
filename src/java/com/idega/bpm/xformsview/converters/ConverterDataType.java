package com.idega.bpm.xformsview.converters;


/**
 *  TODO: use data types from VariableDataType 
 *  Last modified: $Date: 2008/09/16 17:48:15 $ by $Author: civilis $
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 */
public enum ConverterDataType {
	
	DATE {
		public DataConverter getConverter() { 
			return new DateConverter();
		}
	},	
	STRING {
		public DataConverter getConverter() { 
			return new StringConverter();
		}
	},	
	LIST {
		public DataConverter getConverter() { 
			return new CollectionConverter();
		}
	};
	
	public abstract DataConverter getConverter();
}