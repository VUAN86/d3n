package de.ascendro.f4m.service.friend.model.api;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ElasticResyncResponse implements JsonMessageContent {
	
	private int resyncedContactCount;
	private int resyncedBuddyCount;

	public ElasticResyncResponse(int resyncedContacCount, int resyncedBuddyCount) {
		this.resyncedContactCount = resyncedContacCount;
		this.resyncedBuddyCount = resyncedBuddyCount;
	}

	public int getResyncedContactCount() {
		return resyncedContactCount;
	}
	
	public int getResyncedBuddyCount() {
		return resyncedBuddyCount;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("resyncedContactCount=").append(resyncedContactCount);
		builder.append(", resyncedBuddyCount=").append(resyncedBuddyCount);
		builder.append("]");
		return builder.toString();
	}

}
