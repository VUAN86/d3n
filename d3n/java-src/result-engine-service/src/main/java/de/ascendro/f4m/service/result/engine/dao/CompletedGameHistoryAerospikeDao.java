package de.ascendro.f4m.service.result.engine.dao;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.result.engine.model.CompletedGameHistoryInfo;
import de.ascendro.f4m.service.result.engine.model.CompletedGameHistoryList;
import de.ascendro.f4m.service.result.engine.model.GameHistoryUpdateKind;

public interface CompletedGameHistoryAerospikeDao extends AerospikeDao {

	/**
	 * Store/update results for game history
	 */
	void saveResultsForHistory(String userId, String tenantId, GameType gameType, LocalDate endDate,
			CompletedGameHistoryInfo completedGameHistoryInfo, GameHistoryUpdateKind updateKind);

	/**
	 * Move game history entries from one user to another.
	 */
	void moveCompletedGameHistory(String sourceUserId, String targetUserId, String tenantId);

	/**
	 * Returns the list of completed games, with pagination
	 */
	CompletedGameHistoryList getCompletedGamesList(String userId, String tenantId, GameType gameType, long offset, 
			int limit, ZonedDateTime dateTimeFrom, ZonedDateTime dateTimeTo,
												   String isMultiplayerGameFinished,
												   String gameOutcome);

	/**
	 * This method can be used to retrieve the number of completed games for the defined date
	 *
	 * @param userId - the user id
	 * @param tenantId - the tenant id
	 * @param gameType - the type of the game
	 * @param endDate - the end date of the game
	 * @return - number of completed games for date
	 */
	int getNumberOfCompletedGamesForDate(String userId, String tenantId, GameType gameType, LocalDate endDate);

}
