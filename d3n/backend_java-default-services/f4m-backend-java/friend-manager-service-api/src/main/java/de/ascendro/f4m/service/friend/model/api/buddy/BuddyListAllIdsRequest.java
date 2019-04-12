package de.ascendro.f4m.service.friend.model.api.buddy;

import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class BuddyListAllIdsRequest implements JsonMessageContent {

	private String userId;
	private BuddyRelationType[] includedRelationTypes;
	private BuddyRelationType[] excludedRelationTypes;

	public BuddyListAllIdsRequest() {
		// Initialize empty object
	}

	public BuddyListAllIdsRequest(String userId, BuddyRelationType[] includedRelationTypes, BuddyRelationType[] excludedRelationTypes) {
		this.userId = userId;
		this.includedRelationTypes = includedRelationTypes;
		this.excludedRelationTypes = excludedRelationTypes;
	}
	
	public String getUserId() {
		return userId;
	}

	public BuddyRelationType[] getIncludedRelationTypes() {
		return includedRelationTypes == null || includedRelationTypes.length == 0 
				? new BuddyRelationType[] { BuddyRelationType.BUDDY }
				: includedRelationTypes;
	}

	public BuddyRelationType[] getExcludedRelationTypes() {
		return excludedRelationTypes == null || excludedRelationTypes.length == 0 
				? new BuddyRelationType[] { BuddyRelationType.BLOCKED }
				: excludedRelationTypes;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("userId=").append(userId);
		builder.append(", includedRelationTypes=").append(includedRelationTypes);
		builder.append(", excludedRelationTypes=").append(excludedRelationTypes);
		builder.append("]");
		return builder.toString();
	}

}
