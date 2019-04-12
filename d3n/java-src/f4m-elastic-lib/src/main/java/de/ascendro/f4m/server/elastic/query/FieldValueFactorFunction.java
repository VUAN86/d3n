package de.ascendro.f4m.server.elastic.query;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Field value factor function.
 */
public class FieldValueFactorFunction extends JsonObjectWrapper {

	private static final String FUNCTION_NAME_FIELD_VALUE_FACTOR = "field_value_factor";

	public static final String PROPERTY_FIELD = "field";
	public static final String PROPERTY_MISSING = "missing";
	public static final String PROPERTY_QUERY = "query";
	
	private Function parent;
	
	FieldValueFactorFunction(Function parent) {
		this.parent = parent;
		parent.getJsonObject().add(FUNCTION_NAME_FIELD_VALUE_FACTOR, getJsonObject());
	}

	public FieldValueFactorFunction field(String fieldName) {
		setProperty(PROPERTY_FIELD, fieldName);
		return this;
	}
	
	public FieldValueFactorFunction missing(double factor) {
		setProperty(PROPERTY_MISSING, factor);
		return this;
	}
	
	public Function build() {
		return parent;
	}

}
