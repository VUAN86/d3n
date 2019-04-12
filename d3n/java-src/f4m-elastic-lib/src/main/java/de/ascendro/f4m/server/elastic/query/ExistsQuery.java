package de.ascendro.f4m.server.elastic.query;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Exists query (see https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-exists-query.html).
 */
public class ExistsQuery extends JsonObjectWrapper {

	public static final String PROPERTY_FIELD = "field";
	
	private Query parent;
	
	ExistsQuery(Query parent, String fieldName) {
		this.parent = parent;
		setProperty(PROPERTY_FIELD, fieldName);
	}

	public Query build() {
		return parent;
	}
	
}
