package de.ascendro.f4m.service.game.selection.builder;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameEntryFeeSettings;
import de.ascendro.f4m.service.game.selection.model.game.GameEntryFeeValues;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.JsonLoader;

public class GameBuilder {

	private String gameId;
	private String title;
	private String type;
	private Integer playerReadinessTournament;
	private Integer playerReadinessDuel;
	
	private String[] applications;

	private Boolean hideCategories;
	private String[] assignedPools = {};
	private String[] assignedPoolsColors = { "green", null };
	private String[] assignedPoolsIcons = null;
	private boolean userCanOverridePools;

	private Integer numberOfQuestions;
	private String startDateTime = DateTimeUtil.formatISODateTime(DateTimeUtil.getCurrentDateTime().with(LocalTime.MIN));
	private String endDateTime = DateTimeUtil.formatISODateTime(DateTimeUtil.getCurrentDateTime().with(LocalTime.MAX));

	private boolean isFree;
	private boolean entryFeeDecidedByPlayer;
	private BigDecimal entryFeeAmount;
	private String entryFeeCurrency;
	private Integer entryFeeBatchSize;
	private boolean multiplePurchaseAllowed = true;
	private String entryFeeType = Game.ENTRY_FEE_TYPE_DEFAULT;
	private GameEntryFeeSettings entryFeeSettings = new GameEntryFeeSettings();
	private GameEntryFeeValues entryFeeValues = new GameEntryFeeValues();
	private int timeToAcceptInvites = 500;

	private final JsonUtil jsonUtil = new JsonUtil();

	private GameBuilder(String gameId, GameType type) {
		withGameId(gameId).withType(type).withFree(true);
	}

	public static GameBuilder createGame(String gameId, GameType type) {
		return new GameBuilder(gameId, type);
	}

	public Game build() throws Exception {
		String result = JsonLoader.getTextFromResources("gameConfiguration.json", this.getClass());

		result = replace(result, "gameId", gameId);
		result = replace(result, "type", type);
		result = replace(result, "title", title);
		result = replace(result, "applications", applications);
		result = replace(result, "assignedPools", assignedPools);
		result = replace(result, "assignedPoolsColors", assignedPoolsColors);
		result = replace(result, "assignedPoolsIcons", assignedPoolsIcons);
		result = replace(result, "userCanOverridePools", userCanOverridePools);
		result = replace(result, "numberOfQuestions", numberOfQuestions);
		result = replace(result, "startDateTime", startDateTime);
		result = replace(result, "endDateTime", endDateTime);
		result = replace(result, "isFree", isFree);
		result = replace(result, "entryFeeDecidedByPlayer", entryFeeDecidedByPlayer);
		result = replace(result, "entryFeeAmount", entryFeeAmount);
		result = replace(result, "entryFeeCurrency", entryFeeCurrency);
		result = replace(result, "entryFeeBatchSize", entryFeeBatchSize);
		result = replace(result, "multiplePurchaseAllowed", multiplePurchaseAllowed);
		result = replace(result, "entryFeeType", entryFeeType);
		result = replace(result, "entryFeeSettings", entryFeeSettings);
		result = replace(result, "entryFeeValues", entryFeeValues);
		result = replace(result, "timeToAcceptInvites", timeToAcceptInvites);
		result = replace(result, "playerReadinessTournament", playerReadinessTournament);
		result = replace(result, "playerReadinessDuel", playerReadinessDuel);

		Game game = jsonUtil.fromJson(result, Game.class);
		game.setHideCategories(hideCategories);
		return game;
	}

	private String replace(String result, String property, Object value) {
		if (value != null) {
			return result.replaceFirst("\"<<" + property + ">>\"", jsonUtil.toJson(value));
		} else {
			return result.replaceFirst("\"" + property + "\": \"<<" + property + ">>\",", "");
		}
	}

	public GameBuilder withGameId(String gameId) {
		this.gameId = gameId;
		return this;
	}

	public GameBuilder withTitle(String title) {
		this.title = title;
		return this;
	}

	public GameBuilder withType(GameType type) {
		this.type = type.name();
		return this;
	}
	
	public GameBuilder withApplications(String... applications) {
		this.applications = applications;
		return this;
	}

	public GameBuilder withHideCategories(Boolean hideCategories) {
		this.hideCategories = hideCategories;
		return this;
	}
	
	public GameBuilder withAssignedPools(String... assignedPools) {
		this.assignedPools = assignedPools;
		return this;
	}
	
	public GameBuilder withAssignedPoolsColors(String... assignedPoolsColors) {
		this.assignedPoolsColors = assignedPoolsColors;
		return this;
	}
	
	public GameBuilder withAssignedPoolsIcons(String... assignedPoolsIcons) {
		this.assignedPoolsIcons = assignedPoolsIcons;
		return this;
	}

	public GameBuilder withUserCanOverridePools(boolean userCanOverridePools) {
		this.userCanOverridePools = userCanOverridePools;
		return this;
	}

	public GameBuilder withNumberOfQuestions(Integer numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
		return this;
	}

	public GameBuilder withStartDateTime(ZonedDateTime startDateTime) {
		this.startDateTime = DateTimeUtil.formatISODateTime(startDateTime);
		return this;
	}

	public GameBuilder withEndDateTime(ZonedDateTime endDateTime) {
		this.endDateTime = DateTimeUtil.formatISODateTime(endDateTime);
		return this;
	}

	public GameBuilder withFree(boolean isFree) {
		this.isFree = isFree;
		return this;
	}

	public GameBuilder withPlayerReadiness(Integer playerReadinessTournament, Integer playerReadinessDuel) {
		this.playerReadinessTournament = playerReadinessTournament;
		this.playerReadinessDuel = playerReadinessDuel;
		return this;
	}

	public GameBuilder withEntryFeeDecidedByPlayer(boolean entryFeeDecidedByPlayer) {
		this.entryFeeDecidedByPlayer = entryFeeDecidedByPlayer;
		return this;
	}
	
	public GameBuilder withTimeToAcceptInvites(int minutes) {
		this.timeToAcceptInvites = minutes;
		return this;
	}

	public GameBuilder withEntryFee(String amount, String min, String max, String step, Currency currency) {
		this.entryFeeAmount = new BigDecimal(amount);
		this.entryFeeCurrency = currency.name();
		switch (currency) {
		case MONEY:
			this.entryFeeSettings.setMoneyMin(new BigDecimal(min));
			this.entryFeeSettings.setMoneyMax(new BigDecimal(max));
			this.entryFeeSettings.setMoneyIncrement(new BigDecimal(step));
			break;
		case BONUS:
			this.entryFeeSettings.setBonusMin(new BigDecimal(min));
			this.entryFeeSettings.setBonusMax(new BigDecimal(max));
			this.entryFeeSettings.setBonusIncrement(new BigDecimal(step));
			break;
		case CREDIT:
			this.entryFeeSettings.setCreditsMin(new BigDecimal(min));
			this.entryFeeSettings.setCreditsMax(new BigDecimal(max));
			this.entryFeeSettings.setCreditsIncrement(new BigDecimal(step));
			break;
		default:
			break;
		}
		return this;
	}
	
	public GameBuilder withBatchAndMultiplePurchase(int batchSize, boolean multiplePurchaseAllowed) {
		this.entryFeeBatchSize = batchSize;
		this.multiplePurchaseAllowed = multiplePurchaseAllowed;
		return this;
	}

}
