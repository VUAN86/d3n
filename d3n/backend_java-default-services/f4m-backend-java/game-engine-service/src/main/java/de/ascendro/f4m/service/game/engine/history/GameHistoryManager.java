package de.ascendro.f4m.service.game.engine.history;

import de.ascendro.f4m.server.result.RefundReason;
import de.ascendro.f4m.service.game.engine.model.CloseUpReason;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.json.model.user.ClientInfo;

public interface GameHistoryManager {

	void recordGameRegistration(String mgiId, String userId, String gameInstanceId, String gameId);

	void recordGamePrepared(String userId, String gameInstanceId);

	void recordGameReadyToPlay(String userId, String gameInstanceId);

	void recordStartPlaying(String userId, String gameInstanceId);

	void recordGameCompleted(String userId, String gameInstanceId);

	void recordMultiplayerResultsCalculated(String userId, String gameInstanceId);

	void recordGameCancellation(String userId, String gameInstanceId);

	boolean isQuestionPlayed(String userId, String questionId);

	void recordQuestionsPlayed(String userId, String gameId, String... additionalPlayedQuestions);

	void recordMultiplayerResultsNotCalculated(String userId, String gameInstanceId);

	void processPerformGameEndTippTournament(String mgiId);

	void cleanUpActiveGameInstances();

	void terminatedIfNotSet(String gameInstanceId, CloseUpReason closeUpReason, String errorMessage,
			RefundReason refundReason);

	void calculateIfUnfinished(String gameInstanceId, CloseUpReason closeUpReason, String errorMessage,
			RefundReason refundReason);
	
	void calculateIfUnfinished(ClientInfo clientInfo, String gameInstanceId, CloseUpReason closeUpReason, String errorMessage,
			RefundReason refundReason);

	boolean isOnlyInviterRegistered(String mgiId);

	ClientInfo buildClientInfo(GameInstance gameInstance);

}