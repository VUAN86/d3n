package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import java.util.List;

/**
 * Content of inviteGroupToGame response
 * 
 */
public class InviteGroupToGameResponse extends InviteResponse {

	private List<String> usersIds;

	public InviteGroupToGameResponse(String gameInstanceId, String multiplayerGameInstanceId, List<String> usersIds) {
		super(gameInstanceId, multiplayerGameInstanceId);
		this.usersIds = usersIds;
	}

	public List<String> getUsersIds() {
		return usersIds;
	}

	public void setUsersIds(List<String> usersIds) {
		this.usersIds = usersIds;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InviteGroupToGameResponse");
		builder.append(" [usersIds=").append(usersIds);
		builder.append(", ").append(super.toString());
		builder.append("]");

		return builder.toString();
	}

}
