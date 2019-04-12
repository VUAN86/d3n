package de.ascendro.f4m.service.winning.model;

import static de.ascendro.f4m.service.util.F4MEnumUtils.getEnum;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class UserWinningComponent extends JsonObjectWrapper {

	public static final String PROPERTY_USER_WINNING_COMPONENT_ID = "userWinningComponentId";
	public static final String PROPERTY_GAME_INSTANCE_ID = "gameInstanceId";
	public static final String PROPERTY_GAME_ID = "gameId";
	public static final String PROPERTY_STATUS = "status";
	public static final String PROPERTY_SOURCE_GAME_TYPE = "sourceGameType";
	public static final String PROPERTY_ASSIGN_TIMESTAMP = "assignTimestamp";
	public static final String PROPERTY_USE_TIMESTAMP = "useTimestamp";
	public static final String PROPERTY_FILE_TIMESTAMP = "fileTimestamp";

	// To be copied from winning component
	public static final String PROPERTY_WINNING_COMPONENT_ID = "winningComponentId";
	public static final String PROPERTY_GAME_TYPE = "gameType";
	public static final String PROPERTY_CASINO_COMPONENT = "casinoComponent";
	public static final String PROPERTY_TYPE = "type";
	public static final String PROPERTY_AMOUNT = "amount";
	public static final String PROPERTY_CURRENCY = "currency";
	public static final String PROPERTY_IMAGE_ID = "imageId";
	public static final String PROPERTY_TITLE = "title";
	public static final String PROPERTY_RULES = "rules";
	public static final String PROPERTY_DESCRIPTION = "description";
	public static final String PROPERTY_INFO = "info";

	// To be assigned when something has been won
	public static final String PROPERTY_WINNING = "winning";
	
	public UserWinningComponent(String userWinningComponentId, WinningComponent winningComponent, GameWinningComponentListItem winningComponentListItem, String gameInstanceId,
			String gameId, GameType sourceGameType) {
		setProperty(PROPERTY_USER_WINNING_COMPONENT_ID, userWinningComponentId);
		setProperty(PROPERTY_GAME_INSTANCE_ID, gameInstanceId);
		setProperty(PROPERTY_GAME_ID, gameId);
		setProperty(PROPERTY_SOURCE_GAME_TYPE, sourceGameType == null ? null : sourceGameType.name());
		setStatus(UserWinningComponentStatus.NEW);
		if (winningComponent != null) {
			setProperty(PROPERTY_WINNING_COMPONENT_ID, winningComponent.getWinningComponentId());
			setProperty(PROPERTY_GAME_TYPE, winningComponent.getGameType() == null ? null : winningComponent.getGameType().name());
			setProperty(PROPERTY_CASINO_COMPONENT, winningComponent.getCasinoComponent() == null ? null : winningComponent.getCasinoComponent().name());
			setProperty(PROPERTY_TYPE, winningComponentListItem.isPaid() ? WinningComponentType.PAID.name() : WinningComponentType.FREE.name());
			setProperty(PROPERTY_AMOUNT, winningComponentListItem.getAmount());
			setProperty(PROPERTY_CURRENCY, winningComponentListItem.getCurrency() == null ? null : winningComponentListItem.getCurrency().name());
			setProperty(PROPERTY_IMAGE_ID, winningComponent.getImageId());
			setProperty(PROPERTY_TITLE, winningComponent.getTitle());
			setProperty(PROPERTY_RULES, winningComponent.getRules());
			setProperty(PROPERTY_DESCRIPTION, winningComponent.getDescription());
			setProperty(PROPERTY_INFO, winningComponent.getInfo());
		}
		setProperty(PROPERTY_ASSIGN_TIMESTAMP, DateTimeUtil.getCurrentDateTime());
	}

	public UserWinningComponent(JsonObject componentJsonObject) {
		super(componentJsonObject);
	}

	public String getUserWinningComponentId() {
		return getPropertyAsString(PROPERTY_USER_WINNING_COMPONENT_ID);
	}
	
	public String getWinningComponentId() {
		return getPropertyAsString(PROPERTY_WINNING_COMPONENT_ID);
	}

	public String getGameInstanceId() {
		return getPropertyAsString(PROPERTY_GAME_INSTANCE_ID);
	}

	public String getGameId() {
		return getPropertyAsString(PROPERTY_GAME_ID);
	}

	public UserWinningComponentStatus getStatus() {
		return getEnum(UserWinningComponentStatus.class, getPropertyAsString(PROPERTY_STATUS));
	}
	
	public void setStatus(UserWinningComponentStatus status) {
		setProperty(PROPERTY_STATUS, status == null ? null : status.name());
		if (status == UserWinningComponentStatus.USED) {
			setProperty(PROPERTY_USE_TIMESTAMP, DateTimeUtil.getCurrentDateTime());
		} else if (status == UserWinningComponentStatus.FILED) {
			setProperty(PROPERTY_FILE_TIMESTAMP, DateTimeUtil.getCurrentDateTime());
		}
	}
	
	public GameType getSourceGameType() {
		return getEnum(GameType.class, getPropertyAsString(PROPERTY_SOURCE_GAME_TYPE));
	}
	
	public ZonedDateTime getAssignTimestamp() {
		return getPropertyAsZonedDateTime(PROPERTY_ASSIGN_TIMESTAMP);
	}
	
	public ZonedDateTime getUseTimestamp() {
		return getPropertyAsZonedDateTime(PROPERTY_USE_TIMESTAMP);
	}
	
	public ZonedDateTime getFileTimestamp() {
		return getPropertyAsZonedDateTime(PROPERTY_FILE_TIMESTAMP);
	}
	
	public WinningComponentType getType() {
		return getEnum(WinningComponentType.class, getPropertyAsString(PROPERTY_TYPE));
	}
	
	public WinningComponentGameType getGameType() {
		return getEnum(WinningComponentGameType.class, getPropertyAsString(PROPERTY_GAME_TYPE));
	}

	public CasinoComponentType getCasinoComponent() {
		return getEnum(CasinoComponentType.class, getPropertyAsString(PROPERTY_CASINO_COMPONENT));
	}
	
	public BigDecimal getAmount() {
		return getPropertyAsBigDecimal(PROPERTY_AMOUNT);
	}

	public Currency getCurrency() {
		return getEnum(Currency.class, getPropertyAsString(PROPERTY_CURRENCY));
	}

	public String getImageId() {
		return getPropertyAsString(PROPERTY_IMAGE_ID);
	}

	public String getTitle() {
		return getPropertyAsString(PROPERTY_TITLE);
	}

	public String getRules() {
		return getPropertyAsString(PROPERTY_RULES);
	}

	public String getDescription() {
		return getPropertyAsString(PROPERTY_DESCRIPTION);
	}
	
	public String getInfo() {
		return getPropertyAsString(PROPERTY_INFO);
	}

	public WinningOption getWinning() {
		JsonObject winning = getPropertyAsJsonObject(PROPERTY_WINNING);
		return winning == null ? null : new WinningOption(winning);
	}
	
	public void setWinning(WinningOption winning) {
		setProperty(PROPERTY_WINNING, winning == null ? null : winning.getJsonObject());
	}

}
