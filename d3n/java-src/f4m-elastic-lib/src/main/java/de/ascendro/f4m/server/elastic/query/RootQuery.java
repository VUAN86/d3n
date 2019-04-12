package de.ascendro.f4m.server.elastic.query;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Entry point for defining an elasticsearch query.
 */
public class RootQuery extends JsonObjectWrapper {
	
	public static final String PROPERTY_QUERY = "query";
	public static final String PROPERTY_FROM = "from";
	public static final String PROPERTY_SIZE = "size";
	public static final String PROPERTY_SORT = "sort";

	private RootQuery() {
	}
	
	public static RootQuery query(Query query) {
		RootQuery rootQuery = new RootQuery();
		rootQuery.setProperty(PROPERTY_QUERY, query.getJsonObject());
		return rootQuery;
	}
	
	public RootQuery from(long from) {
		setProperty(PROPERTY_FROM, from);
		return this;
	}
	
	public RootQuery size(long size) {
		setProperty(PROPERTY_SIZE, size);
		return this;
	}
	
	public RootQuery sort(Sort sort) {
		setProperty(PROPERTY_SORT, sort.getJsonObject());
		return this;
	}
	
}
