package de.ascendro.f4m.service.friend.model.api;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class LeaderboardPositionResponse implements JsonMessageContent {
	
	private long globalRank;
	private long globalTotal;
	private long buddyRank;
	private long buddyTotal;

	public LeaderboardPositionResponse(long globalRank, long globalTotal, long buddyRank, long buddyTotal) {
		this.globalRank = globalRank;
		this.globalTotal = globalTotal;
		this.buddyRank = buddyRank;
		this.buddyTotal = buddyTotal;
	}

	public long getGlobalRank() {
		return globalRank;
	}
	
	public long getGlobalTotal() {
		return globalTotal;
	}
	
	public long getBuddyRank() {
		return buddyRank;
	}
	
	public long getBuddyTotal() {
		return buddyTotal;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("globalRank=").append(globalRank);
		builder.append(", globalTotal=").append(globalTotal);
		builder.append(", buddyRank=").append(buddyRank);
		builder.append(", buddyTotal=").append(buddyTotal);
		builder.append("]");
		return builder.toString();
	}

}
