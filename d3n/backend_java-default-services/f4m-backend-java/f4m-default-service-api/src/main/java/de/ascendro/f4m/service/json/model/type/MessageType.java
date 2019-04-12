package de.ascendro.f4m.service.json.model.type;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.CaseFormat;

/**
 * Interface describing properties of a F4M message (that is - message name).
 * All message type enumerations should implement this interface.
 */
public interface MessageType {
	/**
	 * Namespace of the message
	 * 
	 * @return namespace, e.g. profile
	 */
	String getNamespace();
	
	/**
	 * Short name of a message - without the namespace
	 * 
	 * @return
	 */
	String getShortName();
	
	/**
	 * Fully qualified message name
	 * 
	 * @return namespace/name
	 */
	default String getMessageName(){
		return (StringUtils.isEmpty(getNamespace()) ? "" : getNamespace() + "/") + getShortName();
	}

	/**
	 * Converts ENUM_NAME in upper underscore to messageName in camel case.
	 * 
	 * @param name
	 * @return
	 */
	default String convertEnumNameToMessageShortName(String name){
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
	}
}
