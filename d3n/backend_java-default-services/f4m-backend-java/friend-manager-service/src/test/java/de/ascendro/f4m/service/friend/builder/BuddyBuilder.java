package de.ascendro.f4m.service.friend.builder;

import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.JsonLoader;

public class BuddyBuilder {

	private String ownerId;
	private String userId;
	private BuddyRelationType[] relationTypes = new BuddyRelationType[] { BuddyRelationType.BUDDY };
	private int interactionCount;
	private String lastPlayedGameInstanceId;
	private ZonedDateTime lastInteractionTimestamp = DateTimeUtil.getCurrentDateTime();
    private String[] tenantIds;
    private boolean favorite;

	private final JsonUtil jsonUtil = new JsonUtil();
	
	public BuddyBuilder(String ownerId, String userId, String tenantId) {
		this.ownerId = ownerId;
		this.userId = userId;
		this.tenantIds = new String[] { tenantId };
	}

	public static BuddyBuilder createBuddy(String ownerId, String userId, String tenantId) {
		return new BuddyBuilder(ownerId, userId, tenantId);
	}
	
	public Buddy buildBuddy() throws Exception {
    	String buddyJson = JsonLoader.getTextFromResources("buddy.json", this.getClass())
    			.replace("\"<<buddyOwnerId>>\"", jsonUtil.toJson(ownerId))
    			.replace("\"<<buddyUserId>>\"", jsonUtil.toJson(userId))
    			.replace("\"<<relationTypes>>\"", jsonUtil.toJson(relationTypes))
    			.replace("\"<<interactionCount>>\"", jsonUtil.toJson(interactionCount))
    			.replace("\"<<lastPlayedGameInstanceId>>\"", jsonUtil.toJson(lastPlayedGameInstanceId))
    			.replace("<<lastInteractionTimestamp>>", DateTimeUtil.formatISODateTime(lastInteractionTimestamp))
    			.replace("\"<<buddyTenantIds>>\"", jsonUtil.toJson(tenantIds))
    			.replace("\"<<favorite>>\"", jsonUtil.toJson(favorite));
    	return new Buddy(jsonUtil.fromJson(buddyJson, JsonObject.class));
	}

	public BuddyBuilder withRelationTypes(BuddyRelationType... relationTypes) {
		this.relationTypes = relationTypes;
		return this;
	}

	public BuddyBuilder withFavorite(boolean favorite) {
		this.favorite = favorite;
		return this;
	}
	
	public BuddyBuilder withInteractionCount(int interactionCount) {
		this.interactionCount = interactionCount;
		return this;
	}

	public BuddyBuilder withLastPlayedGameInstanceId(String lastPlayedGameInstanceId) {
		this.lastPlayedGameInstanceId = lastPlayedGameInstanceId;
		return this;
	}

	public BuddyBuilder withLastInteractionTimestamp(ZonedDateTime lastInteractionTimestamp) {
		this.lastInteractionTimestamp = lastInteractionTimestamp;
		return this;
	}

}
