package de.ascendro.f4m.service.game.engine.feeder;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.util.EventServiceClient;

public class GameEndEventPublisher implements Runnable {

	private EventServiceClient eventServiceClient;
	private String gameId;
	private LoggingUtil loggingUtil;

	public GameEndEventPublisher(EventServiceClient eventServiceClient, String gameId,
			LoggingUtil loggingUtil) {
		this.eventServiceClient = eventServiceClient;
		this.gameId = gameId;
		this.loggingUtil = loggingUtil;
	}

	@Override
	public void run() {
		loggingUtil.saveBasicInformationInThreadContext();
		final String endGameEventTopic = Game.getLiveTournamentEndGameTopic(gameId);
		eventServiceClient.publish(endGameEventTopic, new JsonObject());
	}
}
