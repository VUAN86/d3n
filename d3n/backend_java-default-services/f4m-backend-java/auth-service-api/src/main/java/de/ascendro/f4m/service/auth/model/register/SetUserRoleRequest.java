package de.ascendro.f4m.service.auth.model.register;

import java.util.Arrays;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class SetUserRoleRequest implements JsonMessageContent {

	private String userId;
	private String[] rolesToAdd;
	private String[] rolesToRemove;

	public SetUserRoleRequest() {
	}

	public SetUserRoleRequest(String userId, List<String> roleListToAdd, List<String> roleListToRemove) {
		this.userId = userId;
		if (roleListToAdd != null && !roleListToAdd.isEmpty()) {
			this.rolesToAdd = roleListToAdd.toArray(new String[0]);
		}
		if (roleListToRemove != null && !roleListToRemove.isEmpty()) {
			this.rolesToRemove = roleListToRemove.toArray(new String[0]);
		}
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String[] getRolesToAdd() {
		return rolesToAdd;
	}

	public void setRolesToAdd(String... rolesToAdd) {
		this.rolesToAdd = rolesToAdd;
	}

	public String[] getRolesToRemove() {
		return rolesToRemove;
	}

	public void setRolesToRemove(String... rolesToRemove) {
		this.rolesToRemove = rolesToRemove;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SetUserRoleRequest");
		builder.append(" [userId=").append(userId);
		builder.append(", rolesToAdd=[").append(Arrays.toString(rolesToAdd)).append("]");
		builder.append(", rolesToRemove=[").append(Arrays.toString(rolesToRemove)).append("]");
		builder.append("]");

		return builder.toString();
	}

}
