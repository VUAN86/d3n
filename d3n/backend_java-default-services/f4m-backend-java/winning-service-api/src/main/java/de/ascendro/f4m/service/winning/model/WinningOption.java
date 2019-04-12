package de.ascendro.f4m.service.winning.model;

import com.google.gson.JsonObject;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.util.F4MEnumUtils;

import java.math.BigDecimal;

public class WinningOption extends JsonObjectWrapper {

	public static final String PROPERTY_WINNING_OPTION_ID = "winningOptionId";
	public static final String PROPERTY_PRIZE_ID = "prizeId";
	public static final String PROPERTY_PRIZE_INDEX = "prizeIndex";
	public static final String PROPERTY_TYPE = "type";
	public static final String PROPERTY_AMOUNT = "amount";
	public static final String PROPERTY_PROBABILITY = "probability";
	public static final String PROPERTY_IMAGE_ID = "imageId";
	public static final String PROPERTY_TITLE = "title";

	public WinningOption() {
		// Initialize empty object
	}
	
	public WinningOption(JsonObject configuration) {
		super(configuration);
	}

	public String getWinningOptionId() {
		return getPropertyAsString(PROPERTY_WINNING_OPTION_ID);
	}

	public String getPrizeId() {
		return getPropertyAsString(PROPERTY_PRIZE_ID);
	}

	public Integer getPrizeIndex() {
		return getPropertyAsInteger(PROPERTY_PRIZE_INDEX);
	}

	public WinningOptionType getType() {
		return F4MEnumUtils.getEnum(WinningOptionType.class, getPropertyAsString(PROPERTY_TYPE));
	}

	public BigDecimal getAmount() {
		return getPropertyAsBigDecimal(PROPERTY_AMOUNT);
	}

	public BigDecimal getProbability() {
		return getPropertyAsBigDecimal(PROPERTY_PROBABILITY);
	}
	
	public String getImageId() {
		return getPropertyAsString(PROPERTY_IMAGE_ID);
	}

	public String getTitle() {
		return getPropertyAsString(PROPERTY_TITLE);
	}

	public boolean isSuperPrize() {
		return getType() == WinningOptionType.SUPER;
	}

	@Override
	public String toString() {
		return "WinningOption[ jsonObject=" + jsonObject +
				"] " + super.toString();
	}
}
