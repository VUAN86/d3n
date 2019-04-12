package de.ascendro.f4m.server.elastic.query;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Class for creating elastic search functions.
 */
public class Function extends JsonObjectWrapper {

	private Function() {
	}
	
	public static FieldValueFactorFunction fieldValueFactor() {
		return new FieldValueFactorFunction(new Function());
	}

}
