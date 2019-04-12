package de.ascendro.f4m.service.friend.model.api.group;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class PlayerIsGroupMemberResponse implements JsonMessageContent {
	
	private boolean userIsMember;
	
	public PlayerIsGroupMemberResponse(boolean userIsMember) {
		this.userIsMember = userIsMember;
	}
	
	public boolean isUserIsMember() {
		return userIsMember;
	}

}
