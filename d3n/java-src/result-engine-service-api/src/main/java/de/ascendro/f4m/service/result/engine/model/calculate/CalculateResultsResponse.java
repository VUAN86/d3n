package de.ascendro.f4m.service.result.engine.model.calculate;

import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class CalculateResultsResponse implements JsonMessageContent {

    private String paidWinningComponentId;
    private String freeWinningComponentId;
    
    public CalculateResultsResponse() {
    	// Initialize empty request
    }

    public CalculateResultsResponse(String paidWinningComponentId, String freeWinningComponentId) {
		this.paidWinningComponentId = paidWinningComponentId;
		this.freeWinningComponentId = freeWinningComponentId;
	}

	public CalculateResultsResponse(Results results) {
        paidWinningComponentId = results.getPaidWinningComponentId();
        freeWinningComponentId = results.getFreeWinningComponentId();
    }

	public String getPaidWinningComponentId() {
		return paidWinningComponentId;
	}

	public String getFreeWinningComponentId() {
		return freeWinningComponentId;
	}

}
