package de.ascendro.f4m.server.elastic.query;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Class for creating elastic search queries.
 */
public class Query extends JsonObjectWrapper {

	public static final String PROPERTY_MATCH = "match";
	public static final String PROPERTY_MATCH_ALL = "matchAll";
	public static final String PROPERTY_TERM = "term";
	public static final String PROPERTY_TERMS = "terms";
	public static final String PROPERTY_QUERY_STRING = "query_string";
	public static final String PROPERTY_WILDCARD = "wildcard";
	public static final String PROPERTY_BOOL = "bool";
	public static final String PROPERTY_NOT = "not";
	public static final String PROPERTY_NESTED = "nested";
	public static final String PROPERTY_EXISTS = "exists";
	public static final String PROPERTY_HAS_PARENT = "has_parent";
	public static final String PROPERTY_HAS_CHILD = "has_child";
	public static final String PROPERTY_FUNCTION_SCORE = "function_score";
	public static final String PROPERTY_RANGE = "range";
	
	private Query() {
	}
	
	public static MatchQuery match(String propertyName) {
		Query parent = new Query();
		MatchQuery query = new MatchQuery(parent, propertyName);
		parent.setProperty(PROPERTY_MATCH, query.getJsonObject());
		return query;
	}

	public static Query match(String propertyName, Object value) {
		Query parent = new Query();
		MatchQuery query = new MatchQuery(parent, propertyName, value);
		parent.setProperty(PROPERTY_MATCH, query.getJsonObject());
		return parent;
	}

	public static TermQuery term(String propertyName) {
		Query parent = new Query();
		TermQuery query = new TermQuery(parent, propertyName);
		parent.setProperty(PROPERTY_TERM, query.getJsonObject());
		return query;
	}

	/** If value is provided directly, no customization possible */
	public static Query term(String propertyName, Object value) {
		Query parent = new Query();
		TermQuery query = new TermQuery(parent, propertyName, value);
		parent.setProperty(PROPERTY_TERM, query.getJsonObject());
		return parent;
	}

	/** For terms, no customization possible */
	public static Query terms(String propertyName, Object[] values) {
		Query parent = new Query();
		JsonObject child = new JsonObject();
		if (ArrayUtils.isNotEmpty(values)) {
			JsonArray array = new JsonArray();
			Arrays.stream(values).forEach(value -> array.add(ElasticUtil.convertToJsonElement(value)));
			child.add(propertyName, array);
		}
		parent.setProperty(PROPERTY_TERMS, child);
		return parent;
	}

	public static QueryStringQuery queryString() {
		Query parent = new Query();
		QueryStringQuery query = new QueryStringQuery(parent);
		parent.setProperty(PROPERTY_QUERY_STRING, query.getJsonObject());
		return query;
	}

	public static WildcardQuery wildcard() {
		Query parent = new Query();
		WildcardQuery query = new WildcardQuery(parent);
		parent.setProperty(PROPERTY_WILDCARD, query.getJsonObject());
		return query;
	}

	public static BoolQuery bool() {
		Query parent = new Query();
		BoolQuery query = new BoolQuery(parent);
		parent.setProperty(PROPERTY_BOOL, query.getJsonObject());
		return query;
	}

	public static NestedQuery nested(String path) {
		Query parent = new Query();
		NestedQuery query = new NestedQuery(parent, path);
		parent.setProperty(PROPERTY_NESTED, query.getJsonObject());
		return query;
	}

	public static Query exists(String fieldName) {
		Query parent = new Query();
		ExistsQuery query = new ExistsQuery(parent, fieldName);
		parent.setProperty(PROPERTY_EXISTS, query.getJsonObject());
		return parent;
	}

	public static Query not(Query query) {
		Query parent = new Query();
		parent.setProperty(PROPERTY_NOT, query.getJsonObject());
		return parent;
	}

	public static HasParentQuery hasParent(String type) {
		Query parent = new Query();
		HasParentQuery hasParentQuery = new HasParentQuery(parent, type);
		parent.setProperty(PROPERTY_HAS_PARENT, hasParentQuery.getJsonObject());
		return hasParentQuery;
	}

	public static HasChildQuery hasChild(String type) {
		Query parent = new Query();
		HasChildQuery hasChildQuery = new HasChildQuery(parent, type);
		parent.setProperty(PROPERTY_HAS_CHILD, hasChildQuery.getJsonObject());
		return hasChildQuery;
	}

	public static FunctionScoreQuery functionScore() {
		Query parent = new Query();
		FunctionScoreQuery functionScoreQuery = new FunctionScoreQuery(parent);
		parent.setProperty(PROPERTY_FUNCTION_SCORE, functionScoreQuery.getJsonObject());
		return functionScoreQuery;
	}

	public static Query matchAll() {
		Query parent = new Query();
		parent.setProperty(PROPERTY_MATCH_ALL, new JsonObject());
		return parent;
	}
	
	public static RangeQuery range(String propertyName) {
		Query parent = new Query();
		RangeQuery query = new RangeQuery(parent, propertyName);
		parent.setProperty(PROPERTY_RANGE, query.getJsonObject());
		return query;
	}
	
}
