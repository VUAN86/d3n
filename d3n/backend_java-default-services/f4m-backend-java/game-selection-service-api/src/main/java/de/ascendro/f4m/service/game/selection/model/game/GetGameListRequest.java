package de.ascendro.f4m.service.game.selection.model.game;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Content of getGameList request
 * 
 */
public class GetGameListRequest implements JsonMessageContent {

	@SerializedName(value = GameFilter.TYPE)
	private String type;

	@SerializedName(value = GameFilter.POOL)
	private String pool;

	@SerializedName(value = GameFilter.HANDICAP_FROM)
	private double handicapFrom = 0.0d;

	@SerializedName(value = GameFilter.HANDICAP_TO)
	private double handicapTo = 0.0d;

	@SerializedName(value = GameFilter.IS_FREE)
	private Boolean isFree;

	@SerializedName(value = GameFilter.IS_OFFLINE)
	private Boolean isOfflineGame;

	@SerializedName(value = GameFilter.BY_INVITATION)
	private boolean byInvitation = false;

	@SerializedName(value = GameFilter.BY_FRIEND_CREATED)
	private boolean byFriendCreated = false;

	@SerializedName(value = GameFilter.BY_FRIEND_PLAYED)
	private boolean byFriendPlayed = false;

	@SerializedName(value = GameFilter.NMBR_OF_QUESTIONS_FROM)
	private int numberOfQuestionsFrom = 0;

	@SerializedName(value = GameFilter.NMBR_OF_QUESTIONS_TO)
	private int numberOfQuestionsTo = 0;

	@SerializedName(value = GameFilter.HAS_SPECIAL_PRIZE)
	private Boolean hasSpecialPrize;

	@SerializedName(value = GameFilter.IS_SPECIAL_GAME)
	private Boolean isSpecialGame;

	@SerializedName(value = GameFilter.FULL_TEXT)
	private String fullText;

	public GetGameListRequest() {
		// Empty constructor of getGameList request
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPool() {
		return pool;
	}

	public void setPool(String pool) {
		this.pool = pool;
	}

	public double getHandicapFrom() {
		return handicapFrom;
	}

	public void setHandicapFrom(double handicapFrom) {
		this.handicapFrom = handicapFrom;
	}

	public double getHandicapTo() {
		return handicapTo;
	}

	public void setHandicapTo(double handicapTo) {
		this.handicapTo = handicapTo;
	}

	public Boolean getIsFree() {
		return isFree;
	}

	public void setIsFree(Boolean isFree) {
		this.isFree = isFree;
	}

	public Boolean getIsOfflineGame() {
		return isOfflineGame;
	}

	public void setIsOfflineGame(Boolean isOfflineGame) {
		this.isOfflineGame = isOfflineGame;
	}

	public boolean isByInvitation() {
		return byInvitation;
	}

	public void setByInvitation(boolean byInvitation) {
		this.byInvitation = byInvitation;
	}

	public boolean isByFriendCreated() {
		return byFriendCreated;
	}

	public void setByFriendCreated(boolean byFriendCreated) {
		this.byFriendCreated = byFriendCreated;
	}

	public boolean isByFriendPlayed() {
		return byFriendPlayed;
	}

	public void setByFriendPlayed(boolean byFriendPlayed) {
		this.byFriendPlayed = byFriendPlayed;
	}

	public int getNumberOfQuestionsFrom() {
		return numberOfQuestionsFrom;
	}

	public void setNumberOfQuestionsFrom(int numberOfQuestionsFrom) {
		this.numberOfQuestionsFrom = numberOfQuestionsFrom;
	}

	public int getNumberOfQuestionsTo() {
		return numberOfQuestionsTo;
	}

	public void setNumberOfQuestionsTo(int numberOfQuestionsTo) {
		this.numberOfQuestionsTo = numberOfQuestionsTo;
	}

	public Boolean getHasSpecialPrize() {
		return hasSpecialPrize;
	}

	public void setHasSpecialPrize(Boolean hasSpecialPrize) {
		this.hasSpecialPrize = hasSpecialPrize;
	}

	public Boolean getIsSpecialGame() {
		return isSpecialGame;
	}

	public void setIsSpecialGame(Boolean isSpecialGame) {
		this.isSpecialGame = isSpecialGame;
	}

	public String getFullText() {
		return fullText;
	}

	public void setFullText(String fullText) {
		this.fullText = fullText;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetGameListRequest");
		builder.append("[ type=").append(type);
		builder.append(", pool=").append(pool);
		builder.append(", handicapFrom=").append(handicapFrom);
		builder.append(", handicapTo=").append(handicapTo);
		builder.append(", isFree=").append(isFree);
		builder.append(", isOffileGame=").append(isOfflineGame);
		builder.append(", byInvitation=").append(byInvitation);
		builder.append(", byFriendCreated=").append(byFriendCreated);
		builder.append(", byFriendPlayed=").append(byFriendPlayed);
		builder.append(", numberOfQuestionsFrom=").append(numberOfQuestionsFrom);
		builder.append(", numberOfQuestionsTo=").append(numberOfQuestionsTo);
		builder.append(", hasSpecialPrize=").append(hasSpecialPrize);
		builder.append(", isSpecialGame=").append(isSpecialGame);
		builder.append(", fullText=").append(fullText);
		builder.append("]");

		return builder.toString();
	}

}
