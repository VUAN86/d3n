package de.ascendro.f4m.server.elastic.query;

import static de.ascendro.f4m.server.util.ElasticUtil.convertToJsonElement;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Match query (see https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-match-query.html).
 */
public class MatchQuery extends JsonObjectWrapper {

	public static final String PROPERTY_QUERY = "query";
	public static final String PROPERTY_MINIMUM_SHOULD_MATCH = "minimumShouldMatch";

	private Query parent;
	private JsonObject body;

	MatchQuery(Query parent, String propertyName) {
		this.parent = parent;
		body = new JsonObject();
		setProperty(propertyName, body);
	}

	MatchQuery(Query parent, String propertyName, Object value) {
		this.parent = parent;
		setProperty(propertyName, value);
	}

	public MatchQuery query(String propertyName, String value) {
		setProperty(propertyName, value);
		return this;
	}
	
	public MatchQuery query(Object value) {
		body.add(PROPERTY_QUERY, convertToJsonElement(value));
		return this;
	}

	public MatchQuery minimumShouldMatch(int minimumShouldMatch) {
		body.add(PROPERTY_MINIMUM_SHOULD_MATCH, new JsonPrimitive(minimumShouldMatch));
		return this;
	}
	
	public Query build() {
		return parent;
	}

}
