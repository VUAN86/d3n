package de.ascendro.f4m.service.profile.model;

import java.math.BigDecimal;
import java.util.Objects;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.F4MEnumUtils;

public class ProfileSettingsGame extends JsonObjectWrapper {
	public static final String MINIMUM_GAME_LEVEL_PROPERTY = "minimumGameLevel";
	public static final String CURRENCY_PROPERTY = "currency";
	public static final String ENTRY_FEE_PROPERTY = "entryFee";
	public static final String INTERNATIONAL_ONLY_PROPERTY = "internationalOnly";
	public static final String CATEGORIES_PROPERTY = "categories";
	public static final String NUMBER_OF_QUESTIONS_PROPERTY = "numberOfQuestions";

	public ProfileSettingsGame() {
		// Initialize empty object
	}
	
	public ProfileSettingsGame(JsonObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	public Integer getMinimumGameLevel() {
		return getPropertyAsInteger(MINIMUM_GAME_LEVEL_PROPERTY);
	}

	public void setMinimumGameLevel(Integer minimumGameLevel) {
		setProperty(MINIMUM_GAME_LEVEL_PROPERTY, minimumGameLevel);
	}

	public Currency getCurrency() {
		return F4MEnumUtils.getEnum(Currency.class, getPropertyAsString(CURRENCY_PROPERTY));
	}

	public void setCurrency(Currency currency) {
		setProperty(CURRENCY_PROPERTY, Objects.toString(currency, null));
	}

	public BigDecimal getEntryFee() {
		return getPropertyAsBigDecimal(ENTRY_FEE_PROPERTY);
	}

	public void setEntryFee(BigDecimal entryFee) {
		setProperty(ENTRY_FEE_PROPERTY, entryFee);
	}

	public Boolean isInternationalOnly() {
		return getPropertyAsBoolean(INTERNATIONAL_ONLY_PROPERTY);
	}

	public void setInternationalOnly(Boolean internationalOnly) {
		setProperty(INTERNATIONAL_ONLY_PROPERTY, internationalOnly);
	}

	public String[] getCategories() {
		return getPropertyAsStringArray(CATEGORIES_PROPERTY);
	}

	public void setCategories(String[] categories) {
		setArray(CATEGORIES_PROPERTY, categories);
	}

	public Integer getNumberOfQuestions() {
		return getPropertyAsInteger(NUMBER_OF_QUESTIONS_PROPERTY);
	}

	public void setNumberOfQuestions(Integer numberOfQuestions) {
		setProperty(NUMBER_OF_QUESTIONS_PROPERTY, numberOfQuestions);
	}

}
