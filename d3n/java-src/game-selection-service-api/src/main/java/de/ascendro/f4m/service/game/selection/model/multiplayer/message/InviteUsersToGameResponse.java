package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import java.util.List;

/**
 * Content of inviteFriendsToGame response
 * 
 */
public class InviteUsersToGameResponse extends InviteResponse {

	private List<String> usersIds;
	private List<String> pausedUsersIds;

	public InviteUsersToGameResponse() {
		// Empty constructor of inviteFriendsToGame response
	}

	public List<String> getUsersIds() {
		return usersIds;
	}

	public void setUsersIds(List<String> usersIds) {
		this.usersIds = usersIds;
	}

	public List<String> getPausedUsersIds() {
		return pausedUsersIds;
	}

	public void setPausedUsersIds(List<String> pausedUsersIds) {
		this.pausedUsersIds = pausedUsersIds;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InviteUsersToGameResponse");
		builder.append(" [usersIds=").append(usersIds);
		builder.append(", pausedUsersIds=").append(pausedUsersIds);
		builder.append(", ").append(super.toString());
		builder.append("]");

		return builder.toString();
	}

}
