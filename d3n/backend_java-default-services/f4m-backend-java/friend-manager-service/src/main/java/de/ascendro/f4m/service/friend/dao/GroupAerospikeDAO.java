package de.ascendro.f4m.service.friend.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.ascendro.f4m.service.friend.model.Group;
import de.ascendro.f4m.service.json.model.ListResult;

public interface GroupAerospikeDAO {

    String GROUPS_LIST_BIN_NAME = "groupsList";
    String GROUP_BIN_NAME = "group";
    String GROUP_ID_BIN_NAME = "groupId";

	/** Create a new group. */
    Group createGroup(String userId, String tenantId, String name, String type, String image, Map<String, String> initialUserIds);

	/** Update an existing group. */
    Group updateGroup(String userId, String tenantId, String groupId, boolean updateOnlyUsers, String name, String type, String image, Map<String, String> userIdsToAdd, String... userIdsToRemove);

	/** Get the group. */
	Group getGroup(String userId, String tenantId, String groupId, boolean checkOwner);

	/** Delete the group. */
	void deleteGroup(String userId, String tenantId, String groupId);

	/** Get the list of users groups. */
	ListResult<Group> getGroupsList(String userId, String tenantId, int limit, long offset);

	List<Group> getGroupsList(String userId, String tenantId);

	/** Get the list of groups user is member of. */
	ListResult<Group> getGroupsListMemberOf(String userId, String tenantId, Boolean blocked, int limit, long offset);

	/** List group users. */
	ListResult<String> listGroupUsers(String userId, String tenantId, String groupId, String ownerUsername,
			boolean includeOwner, int limit, long offset);

	/** Move groups from source user to target user. */
	void moveGroups(String sourceUserId, String targetUserId, Collection<String> sourceTenantIds, String targetSearchName);

	/** Is player in group? */
	boolean isPlayerInGroup(String userId, String tenantId, String groupId);

	/** Add blocked groups. */
	void addBlockedGroups(String userId, String tenantId, String[] groupIds, String sortName);

	/** Remove blocked groups. */
	void removeBlockedGroups(String userId, String tenantId, String[] groupIds);

}
