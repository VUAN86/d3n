package de.ascendro.f4m.service.friend.model.api;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class LastDuelInvitationListResponse implements JsonMessageContent {

	private List<ApiProfile> invitedUsers;
	private List<ApiProfile> pausedUsers;

	public LastDuelInvitationListResponse(List<ApiProfile> items, List<ApiProfile> pausedUsers) {
		this.invitedUsers = items;
		this.pausedUsers = pausedUsers;
	}

	public List<ApiProfile> getInvitedUsers() {
		return invitedUsers;
	}

	public void setInvitedUsers(List<ApiProfile> invitedUsers) {
		this.invitedUsers = invitedUsers;
	}

	public List<ApiProfile> getPausedUsers() {
		return pausedUsers;
	}

	public void setPausedUsers(List<ApiProfile> pausedUsers) {
		this.pausedUsers = pausedUsers;
	}

	@Override
	public String toString() {
		return "LastDuelInvitationListResponse{" +
				"invitedUsers=[" + StringUtils.join(invitedUsers, ", ") + " ]" +
				"pausedUsers=[" + StringUtils.join(pausedUsers, ", ") + " ]" +
				'}';
	}
}
