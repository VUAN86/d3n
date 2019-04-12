package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

/**
 * Content of InviteGroupToGame request
 * 
 */
public class InviteGroupToGameRequest extends InviteRequest {

	private String groupId;

	public InviteGroupToGameRequest() {
		// Empty constructor of inviteGroupToGame request
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InviteGroupToGameRequest [groupId=");
		builder.append(groupId);
		builder.append("]");
		return builder.toString();
	}

}
