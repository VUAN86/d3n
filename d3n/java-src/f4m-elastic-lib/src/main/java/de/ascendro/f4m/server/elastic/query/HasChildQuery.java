package de.ascendro.f4m.server.elastic.query;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Query string query (see https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-has-parent-query.html).
 */
public class HasChildQuery extends JsonObjectWrapper {

	public static final String PROPERTY_TYPE = "type";
	public static final String PROPERTY_QUERY = "query";
	public static final String PROPERTY_SCORE_TYPE = "score_type";
	
	private Query parent;
	
	HasChildQuery(Query parent, String type) {
		this.parent = parent;
		setProperty(PROPERTY_TYPE, type);
	}
	
	public Query query(Query query) {
		setProperty(PROPERTY_QUERY, query.getJsonObject());
		return parent;
	}

	public HasChildQuery scoreType(String scoreType) {
		setProperty(PROPERTY_SCORE_TYPE, scoreType);
		return this;
	}
	
}
