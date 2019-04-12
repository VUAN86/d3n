package de.ascendro.f4m.service.result.engine.model.get;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Response to GetResults request.
 */
public class GetMultiplayerResultsResponse implements JsonMessageContent {

	private JsonObject playerResults;
	private JsonObject duelOpponentResults;
	private int place;
	private Integer globalPlace;
	private Integer placeAmongBuddies;
	private Integer globalPlaceAmongBuddies;
	
	public GetMultiplayerResultsResponse() {
		// Initialize empty response
	}
	
	/** Constructor for duels */
	public GetMultiplayerResultsResponse(Results playerResults, Results duelOpponentResults) {
		setPlayerResults(playerResults);
		if (duelOpponentResults != null) {
			this.duelOpponentResults = duelOpponentResults.getJsonObject();
		}
	}

	/** Constructor for tournaments */
	public GetMultiplayerResultsResponse(Results playerResults, int globalPlace, int placeAmongBuddies, int globalPlaceAmongBuddies) {
		setPlayerResults(playerResults);
		this.globalPlace = globalPlace;
		this.placeAmongBuddies = placeAmongBuddies;
		this.globalPlaceAmongBuddies = globalPlaceAmongBuddies;
	}

	public Results getPlayerResults() {
		return playerResults == null ? null : new Results(playerResults);
	}
	
	private void setPlayerResults(Results results) {
		this.playerResults = results == null ? null : results.getJsonObject();
		place = results == null ? 0 : results.getPlacement();
	}
	
	public Results getDuelOpponentResults() {
		return duelOpponentResults == null ? null : new Results(duelOpponentResults);
	}
	
	public int getPlace() {
		return place;
	}
	
	public Integer getGlobalPlace() {
		return globalPlace;
	}
	
	public Integer getPlaceAmongBuddies() {
		return placeAmongBuddies;
	}

	public Integer getGlobalPlaceAmongBuddies() {
		return globalPlaceAmongBuddies;
	}
	
}
