package de.ascendro.f4m.service.game.engine.dao.history;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.service.game.engine.model.GameHistory;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;

public interface GameHistoryDao extends AerospikeDao {

	/**
	 * Create game history entry
	 * @param userId - Player id
	 * @param gameInstanceId -  Played game instance id
	 * @param gameHistoryEntry - new game history entry value
	 * @return result game history value 
	 */
	GameHistory createUserGameHistoryEntry(String userId, String gameInstanceId, GameHistory gameHistoryEntry);
	
	/**
	 * Update existing game history entry status
	 * @param userId - player id
	 * @param gameInstanceId - game instance id
	 * @param status - new status of game or null if not to be changed
	 * @param endStatus - new end game status or null if not to be changed
	 * @return 
	 */
	void updateGameHistoryStatus(String userId, String gameInstanceId, GameStatus status, GameEndStatus endStatus);
	
	/**
	 * Register player question to be played by user
	 * @param userId - Player id
	 * @param questionid - Question id
	 */
	void addPlayedQuestions(String userId, String questionid);
	
	/**
	 * Check if question is played by user 
	 * @param userId - Player id
	 * @param questionId - Question id
	 * @return true if played, false - otherwise
	 */
	boolean isQuestionPlayed(String userId, String questionId);

	/**
	 * Select game history entry
	 * @param userId - player id
	 * @param gameInstanceId - game instance history entry to be selected
	 * @return game history entry
	 */
	GameHistory getGameHistory(String userId, String gameInstanceId);
	
}
