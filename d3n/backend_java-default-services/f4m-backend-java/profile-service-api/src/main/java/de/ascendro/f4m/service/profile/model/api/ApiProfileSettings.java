package de.ascendro.f4m.service.profile.model.api;

import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.profile.model.Profile;

public class ApiProfileSettings {

	private String languageId;
	private String regionId;
	private boolean moneyGames;
	private boolean autoShare;
	private boolean showFullName;
	private ApiProfileSettingsPush push;
	private ApiProfileSettingsGame game;

	public ApiProfileSettings() {
		// Initialize empty object
		push = new ApiProfileSettingsPush();
		game = new ApiProfileSettingsGame();
	}
	
	public ApiProfileSettings(Profile profile) {
		ISOLanguage language = profile.getLanguage();
		languageId = language == null ? null : language.getValue();
		regionId = profile.getRegion();
		moneyGames = profile.isMoneyGames();
		autoShare = profile.isAutoShare();
		showFullName = profile.isShowFullName();
		push = new ApiProfileSettingsPush(profile);
		game = new ApiProfileSettingsGame(profile);
	}

	public void fillProfile(Profile profile) {
		ISOLanguage language = ISOLanguage.fromString(languageId);

		if (language != null) {
			profile.setLanguage(language);
		}
		if (regionId != null) {
			profile.setRegion(regionId);
		}
		profile.setMoneyGames(moneyGames);
		profile.setAutoShare(autoShare);
		profile.setShowFullName(showFullName);
		push.fillProfile(profile);
		game.fillProfile(profile);
	}
	
	public String getLanguageId() {
		return languageId;
	}

	public String getRegionId() {
		return regionId;
	}

	public boolean isMoneyGames() {
		return moneyGames;
	}

	public boolean isAutoShare() {
		return autoShare;
	}

	public boolean isShowFullName() {
		return showFullName;
	}

	public ApiProfileSettingsPush getPush() {
		return push;
	}
	
	public ApiProfileSettingsGame getGame() {
		return game;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ApiProfileSettings [");
		builder.append("languageId=").append(languageId);
		builder.append(", regionId=").append(regionId);
		builder.append(", moneyGames=").append(moneyGames);
		builder.append(", autoShare=").append(autoShare);
		builder.append(", showFullName=").append(showFullName);
		builder.append(", push=").append(push);
		builder.append(", game=").append(game);
		builder.append("]");
		return builder.toString();
	}

}
