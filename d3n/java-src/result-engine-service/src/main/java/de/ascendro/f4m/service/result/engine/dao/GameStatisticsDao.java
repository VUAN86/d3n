package de.ascendro.f4m.service.result.engine.dao;

import de.ascendro.f4m.service.result.engine.model.GameStatistics;

public interface GameStatisticsDao {

	public static final String BIN_NAME_PLAYED_COUNT = "played";
	public static final String BIN_NAME_SPECIAL_PRIZE_AVAILABLE_COUNT = "spPrizeAvail";
	public static final String BIN_NAME_SPECIAL_PRIZE_WON_COUNT = "spPrizeWon";

	/**
	 * Update the game statistics.
	 * @param incrementPlayedCountBy By how much playedCount should be increased
	 * @param incrementSpecialPrizeAvailableCountBy By how much specialPrizeAvailableCount should be increased 
	 */
	GameStatistics updateGameStatistics(String gameId, long incrementPlayedCountBy, long incrementSpecialPrizeAvailableCountBy, long incrementSpecialPrizeWonCountBy);

}
