package de.ascendro.f4m.service.game.engine.server;

import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.game.engine.exception.F4MGameFlowViolation;
import de.ascendro.f4m.service.game.engine.exception.F4MJokerNotAvailableException;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.cancelTournament.CancelTournamentGame;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.engine.model.history.PlayerGameHistoryRequest;
import de.ascendro.f4m.service.game.engine.model.history.PlayerGameHistoryResponse;
import de.ascendro.f4m.service.game.engine.model.joker.JokerInformation;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseJokerRequest;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.JokerType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.game.selection.model.singleplayer.SinglePlayerGameParameters;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.session.SessionWrapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface GameEngine {

	String registerForFreeQuiz24(ClientInfo clientInfo, String gameId, SinglePlayerGameParameters singlePlayerGameConfig);
	
	String registerForPaidQuiz24(ClientInfo clientInfo, String gameId, String entryFeeTransactionId, BigDecimal entryFeeAmount,
			Currency entryFeeCurrency, SinglePlayerGameParameters singlePlayerGameConfig);
	
	String registerForMultiplayerGame(ClientInfo clientInfo, String mgiId, String entryFeeTransactionId, BigDecimal entryFeeAmount, Currency entryFeeCurrency);

	GameInstance startGame(String userId, String gameInstanceId, String userLanguage) throws F4MInsufficientRightsException;

	void validateMultiplayerGameForRegistration(ClientInfo clientInfo, String mgiId, CustomGameConfig customGameConfig);

	GameInstance readyToPlay(String gameInstanceId, long messageReceiveTimestamp, boolean startImmediately);

	/**
	 * Perform nextStep or answerQuestion.
	 * @param gameInstanceId - Game instance id
	 * @param messageReceiveTimestamp - nextStep/answerQuestion receive time
	 * @param skipped Whether question was skipped
	 * @return result state of game instance
	 * @throws F4MGameFlowViolation - No next questions or step to continue with
	 */
	GameInstance performNextGameStepOrQuestion(String gameInstanceId, long messageReceiveTimestamp, boolean skipped) throws F4MGameFlowViolation;
	
	/**
	 * Perform game end if all questions and steps are done
	 * @param gameInstanceId - Game instance id
	 * @return result state of game instance
	 * @throws F4MGameFlowViolation - Not all questions and steps finished to proceed with game end action
	 */
	GameInstance performEndGame(String gameInstanceId) throws F4MGameFlowViolation;

	void performRequestGetJackpotBalance(CancelTournamentGame content, SessionWrapper sessionWrapper, JsonMessage<?> sourceMessage);

	void performCancelTournamentGame(RequestContext context);

	void deleteMgiIdFromPublicGameList(String gameId);

	GameInstance answerQuestion(String clientId, String gameInstanceId, int question, Long clientMsT,
			long receiveTimeStamp, String[] answers);

	long getServerMsT(String gameInstanceId, int question);

	GameInstance registerStepSwitch(String clientId, String gameInstanceId, Long clientMsT, long receiveTimeStamp);

	void endOfTheTippTournament(String mgiId, List<MultiplayerUserGameInstance> gameInstances);

	GameEndStatus cancelGameByClient(ClientInfo clientInfo, String gameInstanceId);
	
	GameInstance validateIfGameNotCancelled(String gameInstanceId);

	boolean isGameCompleted(String gameInstanceId);

	void validateSingleGameForRegistration(ClientInfo clientInfo, Game game, SinglePlayerGameParameters singlePlayerGameConfig);
	
	void closeUpGameIntanceWithFailedResultCalculation(String userId, String gameInstanceId);
	
	void closeUpGameIntanceWithFailedProcessing(ClientInfo clientInfo, String gameInstanceId, String errorMessage);
	
	GameInstance closeUpGameIntanceWithSuccessfulResultCalculation(String gameInstanceId);

	Map<JokerType, JokerInformation> getJokerInformation(String gameInstanceId, int questionIndex);

	/**
	 * Initiate joker purchase.
	 * @param jokerType Joker type
	 * @return true if joker purchase completed already or false if a payment call has been issued
	 * @throws F4MEntryNotFoundException Thrown if game instance not found
	 * @throws F4MJokerNotAvailableException Thrown if joker not available
	 */
	boolean initiateJokerPurchase(JokerType jokerType, SessionWrapper sessionWrapper, 
			JsonMessage<? extends PurchaseJokerRequest> originalRequest) 
					throws F4MEntryNotFoundException, F4MJokerNotAvailableException;

	/**
	 * Update joker used in game instance to finalize joker purchase.
	 */
	GameInstance finalizeJokerPurchase(JokerType jokerType, String gameInstanceId, int questionIndex);

	GameInstance forceEndGame(String gameInstanceId);

	PlayerGameHistoryResponse getPlayerGameHistory(PlayerGameHistoryRequest request);

}
