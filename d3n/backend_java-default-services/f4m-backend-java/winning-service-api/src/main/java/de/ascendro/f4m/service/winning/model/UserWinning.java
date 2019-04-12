package de.ascendro.f4m.service.winning.model;

import static de.ascendro.f4m.service.util.F4MEnumUtils.getEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class UserWinning extends JsonObjectWrapper {

	public static final String PROPERTY_USER_WINNING_ID = "userWinningId";
	public static final String PROPERTY_TITLE = "title";
	public static final String PROPERTY_TYPE = "type";
	public static final String PROPERTY_AMOUNT = "amount";
	public static final String PROPERTY_CURRENCY = "currency";
	public static final String PROPERTY_OBTAIN_DATE = "obtainDate";
	public static final String PROPERTY_DETAILS = "details";
	public static final String PROPERTY_IMAGE_ID = "imageId";
	public static final String PROPERTY_GAME_INSTANCE_ID = "gameInstanceId";
	public static final String PROPERTY_USER_WINNING_COMPONENT_ID = "userWinningComponentId";
	public static final String PROPERTY_TOMBOLA_ID = "tombolaId";
	
	
	public UserWinning(String gameInstanceId, Game game, BigDecimal amount, Currency currency) {
		this(game.getTitle(), determineWinningType(game), amount, currency, game.getGameDescription(), game.getPictureId(),
				gameInstanceId);		
	}
	
	public UserWinning(UserWinningComponent winningComponent, SuperPrize superPrize) {
		this(winningComponent.getTitle(), UserWinningType.WINNING_COMPONENT, determineAmount(winningComponent.getWinning(), superPrize),
				determineCurrency(winningComponent.getWinning().getType()), winningComponent.getInfo(), winningComponent.getImageId(),
				winningComponent.getGameInstanceId());
		setUserWinningComponentId(winningComponent.getUserWinningComponentId());
	}
	
	public UserWinning(String title, UserWinningType type, BigDecimal amount, Currency currency, String details, String imageId, String gameInstanceId) {
		setTitle(title);
		setType(type);
		setAmount(amount);
		setCurrency(currency);
		setObtainDate(DateTimeUtil.getCurrentDateTime());
		setDetails(details);
		setImageId(imageId);
		setGameInstanceId(gameInstanceId);
	}

	public UserWinning(JsonObject winningJsonObject) {
		super(winningJsonObject);
	}

	public String getUserWinningId() {
		return getPropertyAsString(PROPERTY_USER_WINNING_ID);
	}
	
	public void setUserWinningId(String id) {
		setProperty(PROPERTY_USER_WINNING_ID, id);
	}

	public String getTitle() {
		return getPropertyAsString(PROPERTY_TITLE);
	}
	
	public void setTitle(String title) {
		setProperty(PROPERTY_TITLE, title);
	}
	
	public UserWinningType getType() {
		return getEnum(UserWinningType.class, getPropertyAsString(PROPERTY_TYPE));
	}

	public void setType(UserWinningType type) {
		setProperty(PROPERTY_TYPE, type.toString());
	}

	public BigDecimal getAmount() {
		return getPropertyAsBigDecimal(PROPERTY_AMOUNT);
	}
	
	public void setAmount(BigDecimal amount) {
		setProperty(PROPERTY_AMOUNT, amount);
	}

	public Currency getCurrency() {
		return getEnum(Currency.class, getPropertyAsString(PROPERTY_CURRENCY));
	}

	public void setCurrency(Currency currency) {
		setProperty(PROPERTY_CURRENCY, currency == null ? null : currency.name());
	}
	
	public ZonedDateTime getObtainDate() {
		return getPropertyAsZonedDateTime(PROPERTY_OBTAIN_DATE);
	}

	public void setObtainDate(ZonedDateTime dateTime) {
		setProperty(PROPERTY_OBTAIN_DATE, dateTime);
	}

	public String getDetails() {
		return getPropertyAsString(PROPERTY_DETAILS);
	}

	public void setDetails(String details) {
		setProperty(PROPERTY_DETAILS, details);
	}
	
	public String getImageId() {
		return getPropertyAsString(PROPERTY_IMAGE_ID);
	}
	
	public void setImageId(String imageId) {
		setProperty(PROPERTY_IMAGE_ID, imageId);
	}

	public String getGameInstanceId() {
		return getPropertyAsString(PROPERTY_GAME_INSTANCE_ID);
	}

	public void setGameInstanceId(String gameInstanceId) {
		setProperty(PROPERTY_GAME_INSTANCE_ID, gameInstanceId);
	}
	
	public String getTombolaId() {
		return getPropertyAsString(PROPERTY_TOMBOLA_ID);
	}
	
	public void setTombolaId(String tombolaId) {
		setProperty(PROPERTY_TOMBOLA_ID, tombolaId);
	}

	public String getUserWinningComponentId() {
		return getPropertyAsString(PROPERTY_USER_WINNING_COMPONENT_ID);
	}
	
	public void setUserWinningComponentId(String userWinningComponentId) {
		setProperty(PROPERTY_USER_WINNING_COMPONENT_ID, userWinningComponentId);
	}

	private static UserWinningType determineWinningType(Game game) {
		if (game.isDuel()) {
			return UserWinningType.DUEL;
		} else if (game.isTournament()) {
			return UserWinningType.TOURNAMENT;
		} else if (game.getType() == GameType.QUIZ24) {
			return UserWinningType.QUIZ24;
		} else {
			throw new F4MValidationFailedException("Invalid game type: " + game.getType());
		}
	}

	private static Currency determineCurrency(WinningOptionType type) {
		switch (type) {
		case BONUS:
			return Currency.BONUS;
		case CREDITS:
			return Currency.CREDIT;
		case MONEY:
		case SUPER:
			return Currency.MONEY;
		default:
			throw new IllegalArgumentException("Invalid winning option type for winnings: " + type);
		}
	}

	private static BigDecimal determineAmount(WinningOption winning, SuperPrize superPrize) {
		BigDecimal result;
		switch (winning.getType()) {
		case BONUS:
		case CREDITS:
		case MONEY:
			result = winning.getAmount();
			break;
		case SUPER:
			result = BigDecimal.valueOf(superPrize.getWinning());
			break;
		default:
			throw new IllegalArgumentException("Invalid winning option type for winnings: " + winning.getType());
		}
		return result.setScale(2, RoundingMode.DOWN);
	}

}
