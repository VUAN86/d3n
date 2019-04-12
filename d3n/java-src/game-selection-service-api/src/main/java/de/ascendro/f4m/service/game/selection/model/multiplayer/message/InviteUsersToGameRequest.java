package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import java.util.List;

/**
 * Content of InviteFriendsToGame request
 * 
 */
public class InviteUsersToGameRequest extends InviteRequest {

	private List<String> usersIds;
	private List<String> pausedUsersIds;

	private boolean rematch;

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

	public boolean isRematch() {
		return rematch;
	}
	
	public void setRematch(boolean rematch) {
		this.rematch = rematch;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InviteUsersToGameRequest");
		builder.append(" [usersIds=").append(usersIds);
		builder.append(", pausedUsersIds=").append(pausedUsersIds);
		builder.append(", rematch=").append(rematch);
		builder.append(", ").append(super.toString());
		builder.append("]");

		return builder.toString();
	}

}
