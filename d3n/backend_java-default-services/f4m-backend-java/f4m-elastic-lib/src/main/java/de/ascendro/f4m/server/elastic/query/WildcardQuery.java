package de.ascendro.f4m.server.elastic.query;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Wildcard query (see https://www.elastic.co/guide/en/elasticsearch/reference/5.1/query-dsl-wildcard-query.html).
 */
public class WildcardQuery extends JsonObjectWrapper {

	private Query parent;
	
	WildcardQuery(Query parent) {
		this.parent = parent;
	}
	
	public WildcardQuery query(String propertyName, String value) {
		setProperty(propertyName, value);
		return this;
	}
	
	public Query build() {
		return parent;
	}
	
}
