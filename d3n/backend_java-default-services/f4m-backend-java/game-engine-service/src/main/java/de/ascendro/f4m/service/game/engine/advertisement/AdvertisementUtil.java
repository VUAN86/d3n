package de.ascendro.f4m.service.game.engine.advertisement;

import static de.ascendro.f4m.service.game.selection.model.game.AdvertisementFrequency.AFTER_EACH_QUESTION;
import static de.ascendro.f4m.service.game.selection.model.game.AdvertisementFrequency.AFTER_EVERY_X_QUESTION;
import static de.ascendro.f4m.service.game.selection.model.game.AdvertisementFrequency.AFTER_GAME;
import static de.ascendro.f4m.service.game.selection.model.game.AdvertisementFrequency.BEFORE_GAME;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.QuestionStep;
import de.ascendro.f4m.service.game.selection.model.game.AdvertisementFrequency;
import de.ascendro.f4m.service.game.selection.model.game.Game;

public class AdvertisementUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AdvertisementUtil.class);
	
	private AdvertisementUtil(){
		//Static until with no instances
	}
	
	public static int getAdvertisementCountWithinGame(AdvertisementFrequency advertisementFrequency,
			int numberOfQuestions, Integer advFreqXQuestionDefinition) {
		int advertisementCount = 0;

		if (advertisementFrequency == BEFORE_GAME || advertisementFrequency == AFTER_GAME) {
			advertisementCount = 1;
		} else if (advertisementFrequency == AFTER_EACH_QUESTION) {
			advertisementCount = numberOfQuestions;
		} else if (advertisementFrequency == AFTER_EVERY_X_QUESTION && advFreqXQuestionDefinition != null
				&& advFreqXQuestionDefinition > 0) {
			advertisementCount = numberOfQuestions / advFreqXQuestionDefinition;
		}

		return advertisementCount;
	}
	
	public static boolean hasAwaitingAdvertisement(GameInstance gameInstance, GameStatus gameStatus) {
		final boolean hasAdvertisementAwaiting;
		
		final Game game = gameInstance.getGame();
		if (gameInstance.hasAdvertisements()) {
			final AdvertisementFrequency advertisementFrequency = game.getAdvertisementFrequency();
			final GameState gameState = gameInstance.getGameState();
			
			final QuestionStep currentQuestionStep = gameState.getCurrentQuestionStep();
			final Integer currentQuestion = currentQuestionStep != null ? currentQuestionStep.getQuestion() - gameInstance.getSkipsUsed() : null; 
			
			final Integer advFreqXQuestionDefinition = game.getAdvertisementFrequencyXQuestionDefinition();
			
			hasAdvertisementAwaiting = hasAwaitingAdvertisement(gameStatus, advertisementFrequency,
					currentQuestion, advFreqXQuestionDefinition);
		} else {
			hasAdvertisementAwaiting = false;
		}
		return hasAdvertisementAwaiting;
	}

	public static boolean hasAwaitingAdvertisement(GameStatus gameStatus,
			AdvertisementFrequency advertisementFrequency, Integer currentQuestion, Integer advFreqXQuestionDefinition) {
		boolean hasAdvertisementAwaiting = false;
		if (gameStatus == GameStatus.READY_TO_PLAY || gameStatus == GameStatus.PREPARED) {
			hasAdvertisementAwaiting = advertisementFrequency == AdvertisementFrequency.BEFORE_GAME;
		} else if (gameStatus == GameStatus.IN_PROGRESS) {
			if (advertisementFrequency == AdvertisementFrequency.AFTER_EACH_QUESTION) {
				hasAdvertisementAwaiting = true;
			} else if(advertisementFrequency == AdvertisementFrequency.AFTER_EVERY_X_QUESTION){
				hasAdvertisementAwaiting = ((currentQuestion + 1) % advFreqXQuestionDefinition) == 0;
			}
		} else if (gameStatus == GameStatus.COMPLETED) {
			hasAdvertisementAwaiting = advertisementFrequency == AdvertisementFrequency.AFTER_GAME;
		}
		return hasAdvertisementAwaiting;
	}
	
	
	public static String getAwaitingAdvertisement(GameInstance gameInstance) {
		String advertisementBlobKey = null;

		final String[] advertisementBlobKeys = gameInstance.getAdvertisementBlobKeys();
		if (!ArrayUtils.isEmpty(advertisementBlobKeys)) {
			final GameState gameState = gameInstance.getGameState();
			final Integer currentAdvertisementIndex = gameState.getCurrentAdvertisementIndex();
			if (currentAdvertisementIndex != null) {
				if (currentAdvertisementIndex < advertisementBlobKeys.length) {
					advertisementBlobKey = advertisementBlobKeys[currentAdvertisementIndex];
				} else {
					LOGGER.error("Showed all expected advertisements[{}], but another requested",
							String.valueOf(advertisementBlobKeys));
					advertisementBlobKey = advertisementBlobKeys[advertisementBlobKeys.length - 1];
				}
			}
		}

		return advertisementBlobKey;
	}

}
