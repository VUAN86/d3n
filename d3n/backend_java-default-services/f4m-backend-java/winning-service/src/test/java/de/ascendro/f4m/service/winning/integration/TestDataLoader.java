package de.ascendro.f4m.service.winning.integration;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.winning.model.SuperPrize;
import de.ascendro.f4m.service.winning.model.UserWinning;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.UserWinningComponentStatus;
import de.ascendro.f4m.service.winning.model.UserWinningType;
import de.ascendro.f4m.service.winning.model.WinningComponent;
import de.ascendro.f4m.service.winning.model.WinningComponentType;
import de.ascendro.f4m.service.winning.model.WinningOption;
import de.ascendro.f4m.service.winning.model.WinningOptionType;

public class TestDataLoader extends JsonLoader {

	private final JsonUtil jsonUtil;

	public TestDataLoader(JsonUtil jsonUtil) {
		super(new DefaultServiceStartupTest());

		this.jsonUtil = jsonUtil;
	}

	public WinningComponent getWinningComponent(String id) throws Exception {
		String componentString = getPlainTextJsonFromResources("winningComponent.json")
				.replace("<<winningComponentId>>", id);

		JsonObject componentJson = jsonUtil.fromJson(componentString, JsonObject.class);
		return new WinningComponent(componentJson);
	}

	public UserWinningComponent getUserWinningComponent(String id, String gameInstanceId, String gameId, 
			UserWinningComponentStatus status, GameType sourceGameType, String winningComponentId, 
			WinningComponentType type, BigDecimal paymentAmount, Currency paymentCurrency) throws Exception {
		String componentString = getPlainTextJsonFromResources("userWinningComponent.json")
				.replace("<<userWinningComponentId>>", id)
				.replace("<<gameInstanceId>>", gameInstanceId)
				.replace("<<gameId>>", gameId)
				.replace("<<status>>", status == null ? "null" : status.name())
				.replace("<<sourceGameType>>", sourceGameType == null ? "null" : sourceGameType.name())
				.replace("<<winningComponentId>>", winningComponentId)
				.replace("<<type>>", type.toString())
				.replace("\"<<amount>>\"", paymentAmount.toString())
				.replace("<<currency>>", paymentCurrency == null ? "null" : paymentCurrency.name());

		JsonObject componentJson = jsonUtil.fromJson(componentString, JsonObject.class);
		return new UserWinningComponent(componentJson);
	}

	public SuperPrize getSuperPrize(String id, int winning, String insuranceUrl, 
			int randomRangeFrom, int randomRangeTo) throws Exception {
		String componentString = getPlainTextJsonFromResources("superPrize.json")
				.replace("<<superPrizeId>>", id)
				.replace("<<winning>>", String.valueOf(winning))
				.replace("<<insuranceUrl>>", insuranceUrl)
				.replace("<<randomRangeFrom>>", String.valueOf(randomRangeFrom))
				.replace("<<randomRangeTo>>", String.valueOf(randomRangeTo));

		JsonObject componentJson = jsonUtil.fromJson(componentString, JsonObject.class);
		return new SuperPrize(componentJson);
	}

	public WinningOption getWinning(String prizeId, int prizeNumber, WinningOptionType type, BigDecimal amount,
			BigDecimal possibility, String title, String description, String image) throws Exception {
		String configurationString = getPlainTextJsonFromResources("winningConfiguration.json")
				.replace("<<prizeId>>", prizeId)
				.replace("\"<<prizeNumber>>\"", Integer.toString(prizeNumber))
				.replace("<<type>>", type.toString())
				.replace("\"<<amount>>\"", amount.toString())
				.replace("\"<<possibility>>\"", possibility.toString())
				.replace("<<title>>", title)
				.replace("<<description>>", description)
				.replace("<<image>>", image);

		JsonObject configurationJson = jsonUtil.fromJson(configurationString, JsonObject.class);
		return new WinningOption(configurationJson);
	}

	public Game getGame(String id, GameType type, GameWinningComponentListItem... winningComponents) throws Exception {
		String gameString = getPlainTextJsonFromResources("gameConfiguration.json")
				.replace("<<gameId>>", id)
				.replace("\"<<type>>\"", type.toString());
		
		Game game = jsonUtil.fromJson(gameString, Game.class);
		game.setWinningComponents(winningComponents);
		return game;
	}

	public GameInstance getGameInstace(String id, Game game) {
		GameInstance gameInstance = new GameInstance(game);
		gameInstance.setId(id);
		return gameInstance;
	}
	
	public UserWinning getUserWinning(String id, String title, UserWinningType type, BigDecimal amount, Currency currency,
			ZonedDateTime obtainDate, String details, String imageId) throws Exception {
		String userWinningString = getPlainTextJsonFromResources("userWinning.json")
				.replace("<<userWinningId>>", id)
				.replace("<<title>>", title)
				.replace("<<type>>", type.toString())
				.replace("<<currency>>", currency.toString())
				.replace("\"<<amount>>\"", amount.toString())
				.replace("<<obtainDate>>", obtainDate.toString())
				.replace("<<details>>", details)
				.replace("<<imageId>>", imageId);
		
		JsonObject userWinningJson = jsonUtil.fromJson(userWinningString, JsonObject.class);
		return new UserWinning(userWinningJson);
	}

}
