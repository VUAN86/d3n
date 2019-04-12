package de.ascendro.f4m.server.elastic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class ElasticResponse extends JsonObjectWrapper {

	private static final String PROPERTY_TOTAL = "total";
	private static final String PROPERTY_HITS = "hits";
	private static final String PROPERTY_SOURCE = "_source";
	
	public ElasticResponse(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	public long getTotal() {
		JsonObject hitsObject = getHits();
		JsonElement total = hitsObject == null ? null : hitsObject.get(PROPERTY_TOTAL);
		Long result = total == null || ! total.isJsonPrimitive() ? null : total.getAsLong();
		return result == null ? 0 : result;
	}
	
	public List<JsonObject> getResults() {
		JsonObject hitsObject = getHits();
		JsonElement hitsElement = hitsObject == null ? null : hitsObject.get(PROPERTY_HITS);
		JsonArray hits = hitsElement == null || ! hitsElement.isJsonArray() ? null : hitsElement.getAsJsonArray();
		
		if (hits == null || hits.size() == 0) {
			return Collections.emptyList();
		}
		
		List<JsonObject> results = new ArrayList<>(hits.size());
		hits.forEach(hit -> {
			if (hit != null && hit.isJsonObject()) {
				JsonElement sourceElement = hit.getAsJsonObject().get(PROPERTY_SOURCE);
				if (sourceElement != null && sourceElement.isJsonObject()) {
					results.add(sourceElement.getAsJsonObject());
				}
			}
		});

		return results;
	}
	
	private JsonObject getHits() {
		return getPropertyAsJsonObject(PROPERTY_HITS);
	}
	
}
