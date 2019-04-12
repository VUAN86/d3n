package de.ascendro.f4m.service.result.engine.model.get;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Response to GetResults request.
 */
public class GetResultsResponse implements JsonMessageContent {

	private JsonObject results;
	
	public GetResultsResponse() {
		// Initialize empty response
	}
	
	public GetResultsResponse(Results results) {
		this.results = results.getJsonObject();
	}

	public JsonObject getResults() {
		return results;
	}
	
	public void setResults(JsonObject results) {
		this.results = results;
	}
	
}
