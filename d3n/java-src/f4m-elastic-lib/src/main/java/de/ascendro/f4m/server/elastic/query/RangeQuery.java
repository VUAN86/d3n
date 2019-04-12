package de.ascendro.f4m.server.elastic.query;

import static de.ascendro.f4m.server.util.ElasticUtil.convertToJsonElement;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Range query (see https://www.elastic.co/guide/en/elasticsearch/reference/5.1/query-dsl-range-query.html).
 */
public class RangeQuery extends JsonObjectWrapper {
	private static final String PROPERTY_GTE = "gte";
	private static final String PROPERTY_GT = "gt";
	private static final String PROPERTY_LTE = "lte";
	private static final String PROPERTY_LT = "lt";
	
	public static final String DATE_MATH_NOW = "now";

	private Query parent;
	private JsonObject body;

	RangeQuery(Query parent, String propertyName) {
		this.parent = parent;
		body = new JsonObject();
		setProperty(propertyName, body);
	}

	/** Greater-than or equal to */
	public RangeQuery gte(Object value) {
		body.add(PROPERTY_GTE, convertToJsonElement(value));
		return this;
	}

	/** Greater-than */
	public RangeQuery gt(Object value) {
		body.add(PROPERTY_GT, convertToJsonElement(value));
		return this;
	}

	/** Less-than or equal to */
	public RangeQuery lte(Object value) {
		body.add(PROPERTY_LTE, convertToJsonElement(value));
		return this;
	}

	/** Less-than */
	public RangeQuery lt(Object value) {
		body.add(PROPERTY_LT, convertToJsonElement(value));
		return this;
	}

	public Query build() {
		return parent;
	}

}
