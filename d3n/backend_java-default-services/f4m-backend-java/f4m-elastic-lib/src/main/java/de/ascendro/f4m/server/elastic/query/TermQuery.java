package de.ascendro.f4m.server.elastic.query;

import static de.ascendro.f4m.server.util.ElasticUtil.convertToJsonElement;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Term query (see https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-term-query.html).
 */
public class TermQuery extends JsonObjectWrapper {

	public static final String PROPERTY_VALUE = "value";
	
	private Query parent;
	private JsonObject body;
	
	TermQuery(Query parent, String propertyName) {
		this.parent = parent;
		body = new JsonObject();
		setProperty(propertyName, body);
	}

	TermQuery(Query parent, String propertyName, Object value) {
		this.parent = parent;
		setProperty(propertyName, value);
	}
	
	public TermQuery value(Object value) {
		body.add(PROPERTY_VALUE, convertToJsonElement(value));
		return this;
	}
	
	public Query build() {
		return parent;
	}
	
}
