package de.ascendro.f4m.service.result.engine.util;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import de.ascendro.f4m.server.result.MultiplayerResults;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.server.result.UserInteractionType;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.result.engine.model.ApiProfileWithResults;
import de.ascendro.f4m.service.result.engine.model.GameHistoryUpdateKind;
import de.ascendro.f4m.service.result.engine.model.get.CompletedGameListResponse;
import de.ascendro.f4m.service.result.engine.model.multiplayerGameResult.MultiplayerResultsStatus;

public interface ResultEngineUtil {

	/**
	 * Get the results from aerospike, by given game instance id.
	 * @return Results or <code>null</code> if no results found
	 */
	Results getResults(String gameInstanceId) throws F4MEntryNotFoundException;

	/**
	 * Store the results in aerospike.
	 */
	void storeResults(String gameInstanceId, Results results, boolean overwriteExisting);

	/**
	 * Store the game history.
	 */
	void saveGameHistoryForUser(GameInstance gameInstance, Results results, GameHistoryUpdateKind updateKind);

	/**
	 * Store the user winning component in results.
	 */
	void storeUserWinningComponent(ClientInfo clientInfo, Results results, String userWinningComponentId, boolean paid);

	/**
	 * Calculate the game results.
	 */
	Results calculateResults(GameInstance gameInstance, ClientInfo clientInfo);

	/**
	 * Remove user interaction.
	 */
	void removeUserInteraction(String gameInstanceId, UserInteractionType type);

	/**
	 * Send game end notifications to all involved users.
	 */
	void sendGameEndNotificationsToInvolvedUsers(UserResultsByHandicapRange results);

	/**
	 * Get the multiplayer game results from aerospike, by given multiplayer game instance ID.
	 * @return ResultsByHandicapRange or <code>null</code> if no results found
	 */
	MultiplayerResults getMultiplayerResults(String multiplayerGameInstanceId) throws F4MEntryNotFoundException;

		/**
         * Verifies, if multiplayer results have already been calculated
         * @param multiplayerGameInstanceId
         * @return
         */
	boolean hasMultiplayerResults(String multiplayerGameInstanceId);

	/**
	 * Store the game results in aerospike.
	 */
	void storeMultiplayerResults(MultiplayerResults results, boolean overwriteExisting);

	/**
	 * Calculate outcome for multi-user games. Includes calculation of placements for the given list of game instances,
	 * sending "results calculated" events to clients, advancement of playoff brackets and payout calculation.
	 */
	UserResultsByHandicapRange calculateMultiplayerGameOutcome(String multiplayerGameInstanceId);

	/**
	 * Update individual game results to include multiplayer game results
	 */
	void updateIndividualResults(UserResultsByHandicapRange results);

	/**
	 * Update game results to include total bonus points.
	 */
	void storeTotalBonusPoints(String gameInstanceId, BigDecimal bonusPoints);

	/**
	 * Store User results of payout.
	 */
	void storeUserPayout(UserResultsByHandicapRange results);


	/**
	 * Initiate the payout.
	 */
	void initiatePayout(UserResultsByHandicapRange results, boolean isTournament);

	/**
	 * Calculate and update average answering time for each question.
	 */
	void updateAverageAnswerTimeAndWriteWarningsOnTooBigDeviations(Results results, GameInstance gameInstance);

	/**
	 * Returns the list of completed games
	 */
    CompletedGameListResponse getCompletedGameListResponse(String userId, String tenantId, List<GameType> gameTypes, 
    		long offset, int limit, ZonedDateTime dateFrom, ZonedDateTime dateTo,  String isMultiplayerGameFinished,
														   String gameOutcome);

    /**
     * Move all results from source user to target user.
     */
	void moveResults(String sourceUserId, String targetUserId);

	/**
	 * Determine if multiplayer game instance is available.
	 */
	boolean isMultiplayerInstanceAvailable(String multiplayerGameInstanceId);

	/**
	 * Save player game end event (to be issued when single game ends - doesn't matter 
	 * if it's within a multi-player game, or it's a single-player game).
	 */
	void savePlayerGameEndEvent(GameInstance gameInstance, Results results, ClientInfo clientInfo);

	/**
	 * Save game end event (to be issued when whole game finishes - in single-player game case immediately when game ends; 
	 * in multi-player game case - when all players have played and whole game ends).
	 */
	void saveGameEndEvent(Game game, ClientInfo clientInfo);

	/**
	 * Get multiplayer results list. 
	 * If handicapRangeId is not specified, results will be retrieved among all handicap ranges and sorted by results instead of actual place.
	 * If buddy ID-s are specified, results will be retrieved only among buddies and sorted by results instead of actual place.
	 */
	ListResult<ApiProfileWithResults> listMultiplayerResults(String mgiId, String userId, Integer handicapRangeId,
			List<String> buddyIds, int limit, long offset, MultiplayerResultsStatus status);

	/**
	 * Get multiplayer game result rank.
	 */
	int getMultiplayerGameRankByResults(String multiplayerGameInstanceId, String userId, int correctAnswerCount, double gamePointsWithBonus, 
			Integer handicapRangeId, List<String> buddyIds);

	/**
	 * Resynchronize partially calculated multiplayer game results with elastic only for one player.
	 * 
	 * @param result
	 */
	void resyncSinglePlayerResults(Results result);
	
	/**
	 * Resynchronize multiplayer game results with elastic.
	 */
	void resyncMultiplayerResults(String mgiId);

	/**
	 * Resynchronize multiplayer game results with elastic.
	 */
	void resyncMultiplayerResults(UserResultsByHandicapRange results);

}
