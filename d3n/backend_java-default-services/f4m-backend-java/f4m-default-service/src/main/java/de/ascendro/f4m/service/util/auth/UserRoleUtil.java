package de.ascendro.f4m.service.util.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.ascendro.f4m.service.auth.model.register.SetUserRoleRequest;
import de.ascendro.f4m.service.json.model.user.UserRole;

public class UserRoleUtil {

	private UserRoleUtil() {
	}

	public static SetUserRoleRequest createSetUserRoleRequest(String userId, String[] userRoles, UserRole roleToAdd,
			UserRole roleToDelete) {
		SetUserRoleRequest setUserRoleRequest = null;
		if (roleToAdd != null || roleToDelete != null) {
			List<String> useRoleList = userRoles != null ? Arrays.asList(userRoles) : new ArrayList<>();
			if (roleToAdd != null && !useRoleList.contains(roleToAdd.toString())) {
				setUserRoleRequest = new SetUserRoleRequest(userId, Arrays.asList(roleToAdd.toString()), null);
			}
			if (roleToDelete != null && useRoleList.contains(roleToDelete.toString())) {
				setUserRoleRequest = setUserRoleRequest == null ? new SetUserRoleRequest() : setUserRoleRequest;
				setUserRoleRequest.setRolesToRemove(roleToDelete.toString());
			}
		}
		return setUserRoleRequest;
	}
}
