package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ResyncResponse implements JsonMessageContent {
	
	private int resyncedContactCount;
	private int newContactMatchesCount;
	private int lostContactMatchesCount;
	private int resyncedBuddyCount;
	private int lostBuddyCount;

	public ResyncResponse(int resyncedContactCount, int newContactMatchesCount, int lostContactMatchesCount,
			int resyncedBuddyCount, int lostBuddyCount) {
		this.resyncedContactCount = resyncedContactCount;
		this.newContactMatchesCount = newContactMatchesCount;
		this.lostContactMatchesCount = lostContactMatchesCount;
		this.resyncedBuddyCount = resyncedBuddyCount;
		this.lostBuddyCount = lostBuddyCount;
	}

	public int getResyncedContactCount() {
		return resyncedContactCount;
	}
	
	public int getNewContactMatchesCount() {
		return newContactMatchesCount;
	}

	public int getLostContactMatchesCount() {
		return lostContactMatchesCount;
	}

	public int getResyncedBuddyCount() {
		return resyncedBuddyCount;
	}

	public int getLostBuddyCount() {
		return lostBuddyCount;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("resyncedContactCount=").append(resyncedContactCount);
		builder.append(", newContactMatchesCount=").append(newContactMatchesCount);
		builder.append(", lostContactMatchesCount=").append(lostContactMatchesCount);
		builder.append(", resyncedBuddyCount=").append(resyncedBuddyCount);
		builder.append(", lostBuddyCount=").append(lostBuddyCount);
		builder.append("]");
		return builder.toString();
	}

}
