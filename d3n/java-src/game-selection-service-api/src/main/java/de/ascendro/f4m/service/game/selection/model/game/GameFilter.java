package de.ascendro.f4m.service.game.selection.model.game;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Class containing all possible filter fields and some helper methods
 */
public class GameFilter extends JsonObjectWrapper {

	// filtering fields
	public static final String TENANT_ID = "tenantId";
	public static final String APP_ID = "appId";
	public static final String TYPE = "type";
	public static final String POOL = "pool";
	public static final String HANDICAP_FROM = "handicapFrom";
	public static final String HANDICAP_TO = "handicapTo";
	public static final String IS_FREE = "isFree";
	public static final String IS_OFFLINE = "isOfflineGame";
	public static final String BY_INVITATION = "byInvitation";
	public static final String BY_FRIEND_CREATED = "byFriendCreated";
	public static final String BY_FRIEND_PLAYED = "byFriendPlayed";
	public static final String NMBR_OF_QUESTIONS_FROM = "numberOfQuestionsFrom";
	public static final String NMBR_OF_QUESTIONS_TO = "numberOfQuestionsTo";
	public static final String HAS_SPECIAL_PRIZE = "hasSpecialPrize";
	public static final String FULL_TEXT = "fullText";
	public static final String PLAYING_REGIONS = "playingRegions";
	public static final String IS_SPECIAL_GAME = "isSpecialGame";

	// additional data for filtering
	public static final String FRIENDS = "friendsIdList";
	public static final String GAMES = "gamesIdList";

	public GameFilter(String tenantId, String appId, String countryCode, JsonElement jsonElement) {
		if (jsonElement != null && jsonElement.isJsonObject()) {
			this.jsonObject = jsonElement.getAsJsonObject();
		} else {
			this.jsonObject = new JsonObject();
		}
		setProperty(TENANT_ID, tenantId);
		setProperty(APP_ID, appId);
		setProperty(PLAYING_REGIONS, countryCode);
	}

	/**
	 * Get map of search fields for searching by primary key
	 * 
	 * @return map in form [search_field]:[search_value]
	 */
	public Map<String, String> getSearchFields() {
		final Map<String, String> searchFields = new HashMap<>();

		putSearchField(searchFields, Game.SEARCH_FIELD_TYPE, getType());
		putSearchField(searchFields, Game.SEARCH_FIELD_POOL, getPool());
		putSearchField(searchFields, Game.SEARCH_FIELD_FREE,
				getIsFree() == null ? null : Boolean.toString(getIsFree()));
		putSearchField(searchFields, Game.SEARCH_FIELD_OFFLINE,
				getIsOffline() == null ? null : Boolean.toString(getIsOffline()));
		putSearchField(searchFields, Game.SEARCH_FIELD_SPECIAL_PRIZE,
				getHasSpecialPrize() == null ? null : Boolean.toString(getHasSpecialPrize()));

		return searchFields;
	}

	private void putSearchField(Map<String, String> searchFields, String fieldName, String fieldValue) {
		if (fieldValue != null) {
			searchFields.put(fieldName, fieldValue);
		}
	}

	/**
	 * Check if at least one additional filtering field is present
	 * 
	 * @return true if request has at least one additional filtering field
	 */
	public boolean hasAdditionalFilteringFields() {
		return getHandicapTo() > 0 || getNumberOfQuestionsTo() > 0;
	}

	public String getTenantId() {
		return getPropertyAsString(TENANT_ID);
	}

	public String getAppId() {
		return getPropertyAsString(APP_ID);
	}

	public String getCountryCode() {
		return getPropertyAsString(PLAYING_REGIONS);
	}

	public String getType() {
		return getPropertyAsString(TYPE);
	}

	public String getPool() {
		return getPropertyAsString(POOL);
	}

	public double getHandicapFrom() {
		return getPropertyAsDouble(HANDICAP_FROM);
	}

	public double getHandicapTo() {
		return getPropertyAsDouble(HANDICAP_TO);
	}

	public Boolean getIsFree() {
		return getPropertyAsBoolean(IS_FREE);
	}

	public Boolean getIsOffline() {
		return getPropertyAsBoolean(IS_OFFLINE);
	}

	public boolean byInvitation() {
		return getPropertyAsBoolean(BY_INVITATION);
	}

	public boolean byFriendCreated() {
		return getPropertyAsBoolean(BY_FRIEND_CREATED);
	}

	public boolean byFriendPlayed() {
		return getPropertyAsBoolean(BY_FRIEND_PLAYED);
	}

	public int getNumberOfQuestionsFrom() {
		return getPropertyAsInt(NMBR_OF_QUESTIONS_FROM);
	}

	public int getNumberOfQuestionsTo() {
		return getPropertyAsInt(NMBR_OF_QUESTIONS_TO);
	}

	public Boolean getHasSpecialPrize() {
		return getPropertyAsBoolean(HAS_SPECIAL_PRIZE);
	}

	public Boolean getIsSpecialGame() {
		return getPropertyAsBoolean(IS_SPECIAL_GAME);
	}
	
	public String getFullText() {
		return getPropertyAsString(FULL_TEXT);
	}

	public void setFriends(String[] friends) {
		setArray(FRIENDS, friends);
	}

	public String[] getFriends() {
		return getPropertyAsStringArray(FRIENDS);
	}

	public void setGames(String[] games) {
		setArray(GAMES, games);
	}

	public String[] getGames() {
		return getPropertyAsStringArray(GAMES);
	}

}
