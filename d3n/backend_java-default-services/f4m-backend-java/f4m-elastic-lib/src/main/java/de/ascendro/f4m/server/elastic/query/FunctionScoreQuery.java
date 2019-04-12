package de.ascendro.f4m.server.elastic.query;

import java.util.Arrays;

import com.google.gson.JsonArray;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Query string query (see https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-function-score-query.html).
 */
public class FunctionScoreQuery extends JsonObjectWrapper {

	public static final String PROPERTY_QUERY = "query";
	public static final String PROPERTY_FUNCTIONS = "functions";
	
	private Query parent;
	
	FunctionScoreQuery(Query parent) {
		this.parent = parent;
	}
	
	public Query query(Query query) {
		setProperty(PROPERTY_QUERY, query.getJsonObject());
		return parent;
	}

	public FunctionScoreQuery functions(Function... functions) {
		JsonArray fns = new JsonArray();
		Arrays.stream(functions).forEach(f -> fns.add(f.getJsonObject()));
		setProperty(PROPERTY_FUNCTIONS, fns);
		return this;
	}
	
}
