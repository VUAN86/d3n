package de.ascendro.f4m.service.friend.model.api;

import java.time.ZonedDateTime;
import java.util.List;

import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class ApiBuddy {

	private String userId;
	private BuddyRelationType[] relationTypes;
	private int interactionCount;
	private String lastPlayedGameInstanceId;
	private String lastInteractionTimestamp;
	private boolean favorite;

	public ApiBuddy(Buddy buddy) {
		userId = buddy.getUserId();
		List<BuddyRelationType> relationTypes = buddy.getRelationTypes();
		this.relationTypes = relationTypes.toArray(new BuddyRelationType[relationTypes.size()]);
		interactionCount = buddy.getInteractionCount() == null ? 0 : buddy.getInteractionCount();
		lastPlayedGameInstanceId = buddy.getLastPlayedGameInstanceId();
		lastInteractionTimestamp = DateTimeUtil.formatISODateTime(buddy.getLastInteractionTimestamp());
		favorite = buddy.isFavorite() != null ? buddy.isFavorite() : false;
	}

	public String getUserId() {
		return userId;
	}

	public BuddyRelationType[] getRelationTypes() {
		return relationTypes;
	}

	public int getInteractionCount() {
		return interactionCount;
	}

	public String getLastPlayedGameInstanceId() {
		return lastPlayedGameInstanceId;
	}

	public ZonedDateTime getLastInteractionTimestamp() {
		return lastInteractionTimestamp == null ? null : DateTimeUtil.parseISODateTimeString(lastInteractionTimestamp);
	}

	public boolean isFavorite() {
		return favorite;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("userId=").append(userId);
		builder.append(", relationTypes=").append(relationTypes);
		builder.append(", interactionCount=").append(interactionCount);
		builder.append(", lastPlayedGameInstanceId=").append(lastPlayedGameInstanceId);
		builder.append(", lastInteractionTimestamp=").append(lastInteractionTimestamp);
		builder.append(", favorite=").append(favorite);
		builder.append("]");
		return builder.toString();
	}

}
