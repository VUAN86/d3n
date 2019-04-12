package de.ascendro.f4m.server.elastic.query;

import com.google.gson.JsonPrimitive;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Bool query (see https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-bool-query.html).
 */
public class BoolQuery extends JsonObjectWrapper {

	public static final String PROPERTY_MUST = "must";
	public static final String PROPERTY_MUST_NOT = "must_not";
	public static final String PROPERTY_SHOULD = "should";
	public static final String PROPERTY_MINIMUM_SHOULD_MATCH = "minimum_should_match";

	private Query parent;
	
	BoolQuery(Query parent) {
		this.parent = parent;
	}
	
	public BoolQuery addMust(Query query) {
		addElementToArray(PROPERTY_MUST, query.getJsonObject());
		return this;
	}
	
	public BoolQuery addMustNot(Query query) {
		addElementToArray(PROPERTY_MUST_NOT, query.getJsonObject());
		return this;
	}
	
	public BoolQuery addShould(Query query) {
		addElementToArray(PROPERTY_SHOULD, query.getJsonObject());
		return this;
	}
	
	public BoolQuery minimumShouldMatch(int minimumShouldMatch) {
		setProperty(PROPERTY_MINIMUM_SHOULD_MATCH, new JsonPrimitive(minimumShouldMatch));
		return this;
	}
	
	public Query build() {
		return parent;
	}
	
}
