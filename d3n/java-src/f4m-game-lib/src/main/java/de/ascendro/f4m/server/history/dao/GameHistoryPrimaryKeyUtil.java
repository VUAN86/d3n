package de.ascendro.f4m.server.history.dao;

import com.google.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.selection.model.game.GameType;

public class GameHistoryPrimaryKeyUtil extends PrimaryKeyUtil<String> {
	private static final String HISTORY_KEY_FORMAT = "profile:%s";
	
	private static final String PLAYED_QUESTIONS_KEY_FORMAT = "profile:%s:question:%s";
	private static final String PLAYED_OPPONENTS_KEY_FORMAT = "profile:%s";
	
	private static final String COMPLETED_HISTORY_KEY_FORMAT = "history:profile:%s:tenant:%s:gameType:%s:completedAt:%s";
	private static final String COMPLETED_HISTORY_INDEX_KEY_FORMAT = "historyIndex:profile:%s:tenant:%s";

	@Inject
	public GameHistoryPrimaryKeyUtil(Config config) {
		super(config);
	}

	@Override
	public String createPrimaryKey(String userId) {
		return String.format(HISTORY_KEY_FORMAT, userId);
	}
	
	public String createPlayedQuestionPrimaryKey(String userId, String questionId) {
		return String.format(PLAYED_QUESTIONS_KEY_FORMAT, userId, questionId);
	}
	
	public String createPlayedOpponentsPrimaryKey(String userId) {
		return String.format(PLAYED_OPPONENTS_KEY_FORMAT, userId);
	}
	
	/**
     * Creates primary key for results history list
     * @return primary key as profile:[userId]:[gameType]:completed:[date]
     */
    public String createCompletedGameHistoryPrimaryKey(String userId, String tenantId, GameType gameType, String date) {
		return String.format(COMPLETED_HISTORY_KEY_FORMAT, userId, tenantId, gameType.toString(), date);
    }

	/**
     * Creates primary key for results history list index
     * @return primary key as profile:[userId]
     */
	public String createCompletedGameHistoryIndexPrimaryKey(String userId, String tenantId) {
		return String.format(COMPLETED_HISTORY_INDEX_KEY_FORMAT, userId, tenantId);
	}
	
}
