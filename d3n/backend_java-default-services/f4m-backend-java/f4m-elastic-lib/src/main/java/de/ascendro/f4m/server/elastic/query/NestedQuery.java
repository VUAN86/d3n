package de.ascendro.f4m.server.elastic.query;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Nested query (see https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-nested-query.html).
 */
public class NestedQuery extends JsonObjectWrapper {

	public static final String PROPERTY_PATH = "path";
	public static final String PROPERTY_QUERY = "query";
	
	private Query parent;
	
	NestedQuery(Query parent, String path) {
		this.parent = parent;
		setProperty(PROPERTY_PATH, path);
	}
	
	public Query query(Query query) {
		setProperty(PROPERTY_QUERY, query.getJsonObject());
		return parent;
	}
	
}
