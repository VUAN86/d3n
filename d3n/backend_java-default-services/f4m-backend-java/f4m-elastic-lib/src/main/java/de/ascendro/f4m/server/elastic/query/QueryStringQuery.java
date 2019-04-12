package de.ascendro.f4m.server.elastic.query;

import java.util.Arrays;

import com.google.gson.JsonPrimitive;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Query string query (see https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html).
 */
public class QueryStringQuery extends JsonObjectWrapper {

	public static final String PROPERTY_FIELDS = "fields";
	public static final String PROPERTY_QUERY = "query";
	
	private Query parent;
	
	QueryStringQuery(Query parent) {
		this.parent = parent;
	}
	
	public QueryStringQuery fields(String... fields) {
		Arrays.stream(fields).forEach(field -> addElementToArray(PROPERTY_FIELDS, new JsonPrimitive(field)));
		return this;
	}

	public QueryStringQuery query(String query) {
		setProperty(PROPERTY_QUERY, query);
		return this;
	}
	
	public Query build() {
		return parent;
	}
	
}
