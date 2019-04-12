package de.ascendro.f4m.service.game.engine.server;

import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.GAME_ID;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.MGI_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.QuestionStep;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.singleplayer.SinglePlayerGameParameters;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.model.Currency;

public class GamePlayer {

	private static final Logger LOGGER = LoggerFactory.getLogger(GamePlayer.class);

	private final List<Exception> errors = new ArrayList<>();

	private final GameEngine gameEngine;

	private GameInstance gameInstance;
	private ClientInfo clientInfo = ClientInfo.cloneOf(ANONYMOUS_CLIENT_INFO);

	private final String userLanguage;

	private final GameType gameType;
	private final boolean free;

	private final SinglePlayerGameParameters singlePlayerGameConfig;

	private final String entryFeeTransactionId;
	private final BigDecimal entryFeeAmount;
	private final Currency entryFeeCurrency;
	
	private int numberOfQuestions = 3;
	
	public GamePlayer(GameEngine gameEngine, GameType gameType, String userLanguage, boolean free,
			boolean trainingMode, String entryFeeTransactionId, BigDecimal entryFeeAmount, Currency entryFeeCurrency) {
		this.gameEngine = gameEngine;
		this.gameType = gameType;
		this.userLanguage = userLanguage;
		this.free = free;
		this.singlePlayerGameConfig = new SinglePlayerGameParameters(trainingMode);
		this.entryFeeTransactionId = entryFeeTransactionId;
		this.entryFeeAmount = entryFeeAmount;
		this.entryFeeCurrency = entryFeeCurrency;
	}

	public GamePlayer(GameEngine gameEngine, GameType gameType, String userLanguage) {
		this(gameEngine, gameType, userLanguage, true, false, null, null, null);
	}
	
	private void play(PlayUntil playUntil){
		try {
			gameInstance = register();
			if (playUntil == PlayUntil.REGISTER) {
				return;
			}

			gameInstance = gameEngine.startGame(clientInfo.getUserId(), gameInstance.getId(), userLanguage);
			if (playUntil == PlayUntil.START) {
				return;
			}

			gameInstance = gameEngine.readyToPlay(gameInstance.getId(), System.currentTimeMillis(), true);
			if (playUntil == PlayUntil.READY_TO_PLAY) {
				return;
			}

			for (int i = 0; i < numberOfQuestions; i++) {
				do {
					replyToStartStep(gameInstance.getGameState().getCurrentQuestionStep(), gameInstance.getId());
					gameInstance = performNextAction(gameInstance);
				} while (gameInstance.getGameState().getCurrentQuestionStep().getQuestion() == i
						&& gameInstance.getGameState().getGameStatus() != GameStatus.COMPLETED);
				if (playUntil == PlayUntil.ANSWER_FIRST && i == 0) {
					return;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to start a game with user[{}], game[{}], mgiId[{}], language[{}]",
					clientInfo.getUserId(), GAME_ID, MGI_ID, userLanguage, e);
			errors.add(e);
		}
	}

	private void replyToStartStep(QuestionStep currentQuestionStep, String gameInstanceId) {
		if (currentQuestionStep.getStep() == currentQuestionStep.getStepCount() - 1) {
			gameEngine.answerQuestion(clientInfo.getClientId(), gameInstanceId, currentQuestionStep.getQuestion(), 100L,
					DateTimeUtils.currentTimeMillis() - 100, new String[] { "abc" });
		} else {
			gameEngine.registerStepSwitch(clientInfo.getClientId(), gameInstanceId, 90L,
					DateTimeUtils.currentTimeMillis() - 100);
		}

	}

	private GameInstance register() {
		final String gameInstanceId;
		if (gameType == GameType.QUIZ24) {
			if (free) {
				gameInstanceId = gameEngine.registerForFreeQuiz24(clientInfo, GAME_ID,
						singlePlayerGameConfig);
			} else {
				gameInstanceId = gameEngine.registerForPaidQuiz24(clientInfo, GAME_ID,
						entryFeeTransactionId, entryFeeAmount, entryFeeCurrency, singlePlayerGameConfig);
			}
		} else if (gameType.isMultiUser()) {
			gameInstanceId = gameEngine.registerForMultiplayerGame(clientInfo, MGI_ID, entryFeeTransactionId,
					entryFeeAmount, entryFeeCurrency);
		} else {
			throw new IllegalStateException("Unsupported type");
		}
		return gameEngine.validateIfGameNotCancelled(gameInstanceId);
	}

	public void setClientInfo(String userId, Double handicap, UserRole... roles) {
		this.clientInfo.setUserId(userId);
		this.clientInfo.setHandicap(handicap);
		this.clientInfo.setRoles(Arrays.stream(roles)
				.map(r -> r.name())
				.toArray(String[]::new)
			);
	}

	public GameInstance getGameInstance() {
		return gameInstance;
	}

	public void addError(Exception e) {
		errors.add(e);
	}

	public List<Exception> getErrors() {
		return errors;
	}

	public String getErrorMessages() {
		final StringBuffer messages = new StringBuffer();

		errors.forEach(e -> messages.append("[").append(e.getMessage()).append("]"));

		return messages.toString();
	}
	
	public ClientInfo getClientInfo() {
		return clientInfo;
	}
	
	public GameInstance registerAndStart(){
		return playUntil(PlayUntil.START);
	}
	
	public GameInstance playUntil(PlayUntil playUntil){
		return exeucte(playUntil);
	}
	
	private GameInstance exeucte(PlayUntil playUntil) {
		GameInstance gameInstance = null;
		try {
			final Thread gameThread = new Thread(() -> play(playUntil));

			gameThread.start();
			gameThread.join();

			gameInstance = this.getGameInstance();
		} catch (InterruptedException e) {
			LOGGER.error("Failed to join game starter thread", e);
			this.addError(e);
		}
		return gameInstance;
	}
	
	private GameInstance performNextAction(GameInstance gameInstance) {
		GameInstance newGameInstance;
		if (gameInstance.hasAnyStepOrQuestion()) {
			newGameInstance = gameEngine.performNextGameStepOrQuestion(gameInstance.getId(), System.currentTimeMillis(), false);
		} else {
			newGameInstance = gameEngine.performEndGame(gameInstance.getId());
		}
		return newGameInstance;
	}
	
	public static GameInstance[] registerAndStart(GamePlayer... starters) {
		return Arrays.stream(starters)
				.parallel()
				.map(s -> s.registerAndStart())
				.toArray(size -> new GameInstance[size]);
	}
	
	public enum PlayUntil{
		REGISTER,
		START,
		READY_TO_PLAY,
		ANSWER_FIRST,
		ANSWER_ALL
	}
}
