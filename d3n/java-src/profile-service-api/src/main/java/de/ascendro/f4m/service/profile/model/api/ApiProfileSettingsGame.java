package de.ascendro.f4m.service.profile.model.api;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileSettingsGame;

public class ApiProfileSettingsGame {
	private Integer minimumGameLevel;
	private Currency currency;
	private BigDecimal entryFee;
	private Boolean internationalOnly;
	private String[] categories;
	private Integer numberOfQuestions;

	public ApiProfileSettingsGame() {
		// Initialize empty object
	}

	public ApiProfileSettingsGame(Profile profile) {
		ProfileSettingsGame profileSettings = profile.getSettingsGameWrapper();
		if (profileSettings != null) {
			minimumGameLevel = profileSettings.getMinimumGameLevel();
			currency = profileSettings.getCurrency();
			entryFee = profileSettings.getEntryFee();
			internationalOnly = profileSettings.isInternationalOnly();
			categories = profileSettings.getCategories();
			numberOfQuestions = profileSettings.getNumberOfQuestions();
		}
	}
	
	public void fillProfile(Profile profile) {
		ProfileSettingsGame profileSettings = profile.getSettingsGameWrapper();
		if (profileSettings == null) {
			profileSettings = new ProfileSettingsGame();
			profile.setSettingsGameWrapper(profileSettings);
		}
		if (minimumGameLevel != null) {
			profileSettings.setMinimumGameLevel(minimumGameLevel);
		}
		if (currency != null) {
			profileSettings.setCurrency(currency);
		}
		if (entryFee != null) {
			profileSettings.setEntryFee(entryFee);
		}
		if (internationalOnly != null) {
			profileSettings.setInternationalOnly(internationalOnly);
		}
		if (categories != null) {
			profileSettings.setCategories(categories);
		}
		if (numberOfQuestions != null) {
			profileSettings.setNumberOfQuestions(numberOfQuestions);
		}
	}

	public Integer getMinimumGameLevel() {
		return minimumGameLevel;
	}

	public Currency getCurrency() {
		return currency;
	}

	public BigDecimal getEntryFee() {
		return entryFee;
	}

	public boolean isInternationalOnly() {
		return internationalOnly;
	}

	public String[] getCategories() {
		return categories;
	}

	public Integer getNumberOfQuestions() {
		return numberOfQuestions;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ApiProfileSettingsGame [minimumGameLevel=");
		builder.append(minimumGameLevel);
		builder.append(", currency=");
		builder.append(currency);
		builder.append(", entryFee=");
		builder.append(entryFee);
		builder.append(", internationalOnly=");
		builder.append(internationalOnly);
		builder.append(", categories=");
		builder.append(categories);
		builder.append(", numberOfQuestions=");
		builder.append(numberOfQuestions);
		builder.append("]");
		return builder.toString();
	}
}
