package de.ascendro.f4m.service.game.engine.history;

import java.util.TimerTask;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.logging.LoggingUtil;

public class ActiveGameTimerTask extends TimerTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(ActiveGameTimerTask.class);

	private final GameHistoryManager gameHistoryManagerImpl;
	private final LoggingUtil loggingUtil;
	private final MultiplayerGameManager multiplayerGameManager;

	@Inject
	public ActiveGameTimerTask(GameHistoryManager gameHistoryManagerImpl, LoggingUtil loggingUtil,
			MultiplayerGameManager multiplayerGameManager) {
		this.gameHistoryManagerImpl = gameHistoryManagerImpl;
		this.loggingUtil = loggingUtil;
		this.multiplayerGameManager = multiplayerGameManager;
	}

	@Override
	public void run() {
		loggingUtil.saveBasicInformationInThreadContext();
		cleanUpActiveGameInstances();
		cleanUpPublicGameList();
	}

	private void cleanUpActiveGameInstances() {
		LOGGER.info("Running active game instance clean up timer task...");
		try {
			gameHistoryManagerImpl.cleanUpActiveGameInstances();
			LOGGER.info("Completed active game instance clean up timer task...");
		} catch (Exception e) {
			LOGGER.error("Failed active game instance clean up timer task...", e);
		}
	}
	
	private void cleanUpPublicGameList() {
		LOGGER.info("Running public game list clean up timer task...");
		try {
			multiplayerGameManager.cleanUpPublicGameList();
			LOGGER.info("Completed public game list clean up timer task...");
		} catch (Exception e) {
			LOGGER.error("Failed public game list clean up timer task...", e);
		}
	}

}
