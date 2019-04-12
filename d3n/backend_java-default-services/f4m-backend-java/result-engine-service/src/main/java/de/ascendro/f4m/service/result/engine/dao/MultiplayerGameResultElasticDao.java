package de.ascendro.f4m.service.result.engine.dao;

import java.util.List;

import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.result.engine.model.MultiplayerGameResult;

public interface MultiplayerGameResultElasticDao {

	void createOrUpdate(MultiplayerGameResult multiplayerGameResult);

	/**
	 * Get multiplayer results list. 
	 * If handicapRangeId is not specified, results will be retrieved among all handicap ranges and sorted by results instead of actual place.
	 * If buddy ID-s are specified, results will be retrieved only among buddies and sorted by results instead of actual place.
	 * If place is not yet calculated, results are sorted always by results instead of actual place.
	 */
	ListResult<MultiplayerGameResult> listResults(String mgiId, Integer handicapRangeId, List<String> includedUserIds,
			int limit, long offset, boolean placeCalculated);

	/**
	 * Get rank.
	 */
	int getMultiplayerGameRankByResults(String multiplayerGameInstanceId, String userId, int correctAnswerCount,
			double gamePointsWithBonus, Integer handicapRangeId, List<String> buddyIds);

}
