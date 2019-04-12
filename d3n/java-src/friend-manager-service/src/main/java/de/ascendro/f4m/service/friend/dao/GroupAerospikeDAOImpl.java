package de.ascendro.f4m.service.friend.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryAlreadyExistsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.friend.config.FriendManagerConfig;
import de.ascendro.f4m.service.friend.model.Group;
import de.ascendro.f4m.service.friend.util.FriendPrimaryKeyUtil;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.json.model.OrderBy;

public class GroupAerospikeDAOImpl extends AerospikeOperateDaoImpl<FriendPrimaryKeyUtil> implements GroupAerospikeDAO {

    @Inject
    public GroupAerospikeDAOImpl(Config config, FriendPrimaryKeyUtil friendPrimaryKeyUtil, JsonUtil jsonUtil,
                                  AerospikeClientProvider aerospikeClientProvider) {
        super(config, friendPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
    }

    @Override
    public Group createGroup(String userId, String tenantId, String name, String type, String image, Map<String, String> initialUserIds) {
    	String groupId = UUID.randomUUID().toString();
    	
    	// Create group unique key
    	addUniqueGroupKeyEntry(groupId, tenantId, userId, name);
    	
    	// Create group
        Group group = new Group(groupId, tenantId, userId, name, type);
        group.setImage(image);
        group.addMemberUserIds(initialUserIds);
        createJson(getGroupSet(), groupId, GroupAerospikeDAO.GROUP_BIN_NAME, group.getAsString());
        
        // Add group to user owned groups
        addOrUpdateUserOwnedGroup(userId, tenantId, groupId, group);
		
		// Add group to user membership groups
		if (initialUserIds != null) {
			initialUserIds.keySet().forEach(uId -> addOrUpdateUserGroupMembership(uId, tenantId, groupId, group, false));
		}
		
        return group;
    }

	@Override
    public Group updateGroup(String userId, String tenantId, String groupId, boolean updateOnlyUsers, String name, String type, String image, Map<String, String> userIdsToAdd, String... userIdsToRemove) {
		boolean groupNameChanged = false;
		if (! updateOnlyUsers) {
			Group oldGroup = getGroup(userId, tenantId, groupId, false);
			if (! StringUtils.equals(oldGroup.getName(), name)) {
				groupNameChanged = true;
			}
		}
		
		if (groupNameChanged) {
			// Create unique key
			addUniqueGroupKeyEntry(groupId, tenantId, userId, name);
		}
		
    	// Update group
		StringBuilder oldName = new StringBuilder();
        String updatedGroupJson = updateJson(getGroupSet(), groupId, GROUP_BIN_NAME, (readGroup, writePolicy) -> {
			if (readGroup == null) {
	            throw new F4MEntryNotFoundException();
	        }
			Group group = new Group(jsonUtil.fromJson(readGroup, JsonObject.class));
			if (updateOnlyUsers || userIdsToAdd != null || userIdsToRemove != null || ! StringUtils.equals(group.getName(), name)) {
				// Image may be changed by anyone, otherwise assure that the owner is changing it
				assureCorrectGroupOwner(group, userId, tenantId);
			}
			if (! updateOnlyUsers) {
				oldName.append(group.getName());
				group.setName(name);
				group.setType(type);
				group.setImage(image);
			}
			if (userIdsToAdd != null) {
				group.addMemberUserIds(userIdsToAdd);
			}
			if (userIdsToRemove != null) {
				group.removeMemberUserIds(userIdsToRemove);
			}
			return group.getAsString();
		});
        Group group = new Group(jsonUtil.fromJson(updatedGroupJson, JsonObject.class));

        if (groupNameChanged && ! StringUtils.equals(group.getName(), oldName.toString())) {
            // Delete old unique key
        	removeUniqueGroupKeyEntry(tenantId, userId, oldName.toString());
        }

		// Update user owned groups
        addOrUpdateUserOwnedGroup(userId, tenantId, groupId, group);
        
        // Update removed group memberships
		if (userIdsToRemove != null) {
			Arrays.stream(userIdsToRemove).forEach(uId -> removeUserGroupMembership(uId, tenantId, groupId));
		}

		// Update all user membership groups
        group.getMemberUserIds().keySet().forEach(uId -> addOrUpdateUserGroupMembership(uId, tenantId, groupId, group, false));
        group.getBlockedUserIds().keySet().forEach(uId -> addOrUpdateUserGroupMembership(uId, tenantId, groupId, group, true));

		return group;
	}

	@Override
    public Group getGroup(String userId, String tenantId, String groupId, boolean checkOwner) {
		String jsonString = readJson(getGroupSet(), groupId, GROUP_BIN_NAME);
        if (jsonString == null) {
            throw new F4MEntryNotFoundException("Cannot find group by ID " + groupId);
        }
        Group group = new Group(jsonUtil.fromJson(jsonString, JsonObject.class));
        if (checkOwner) {
        	assureCorrectGroupOwner(group, userId, tenantId);
        }
		return group;
    }

    private void assureCorrectGroupOwner(Group group, String userId, String tenantId) {
		if (! StringUtils.equals(group.getUserId(), userId)) {
			throw new F4MInsufficientRightsException("User ID of group (" + group.getUserId() + 
					") does not match the user ID of requestor (" + userId + ")");
		}
		if (! Objects.equals(group.getTenantId(), tenantId)) {
			throw new F4MInsufficientRightsException("Tenant ID of group (" + group.getTenantId() + 
					") does not match the tenant ID of requestor (" + tenantId + ")");
		}
	}
	
	@Override
    public void deleteGroup(String userId, String tenantId, String groupId) {
    	Group group = getGroup(userId, tenantId, groupId, true);

		// Delete group
        delete(getGroupSet(), groupId);
        
        // Delete group from user owned groups
        removeUserOwnedGroup(userId, tenantId, groupId);
        deleteByKeyFromMap(getOwnedGroupSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, userId), GROUPS_LIST_BIN_NAME, groupId);

        // Delete user group memberships
		group.getMemberUserIds().keySet().forEach(uId -> removeUserGroupMembership(uId, tenantId, groupId));
		group.getBlockedUserIds().keySet().forEach(uId -> removeUserGroupMembership(uId, tenantId, groupId));
    }

	@Override
    public ListResult<Group> getGroupsList(String userId, String tenantId, int limit, long offset) {
    	List<JsonObject> groups = getAllMap(getOwnedGroupSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, userId), 
    			GROUPS_LIST_BIN_NAME).values().stream()
	    			.map(group -> jsonUtil.fromJson((String) group, JsonObject.class))
	    			.collect(Collectors.toList());
		JsonUtil.sort(groups, Arrays.asList(new OrderBy(Group.PROPERTY_NAME)));
		List<Group> results = groups.stream().skip(offset < 0 ? 0 : offset).limit(limit < 0 ? 0 : limit)
				.map(Group::new).collect(Collectors.toList());
		return new ListResult<>(limit, offset, groups.size(), results);
    }

	@Override
	public List<Group> getGroupsList(String userId, String tenantId) {
		List<JsonObject> groups = getAllMap(getOwnedGroupSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, userId),
				GROUPS_LIST_BIN_NAME).values().stream()
				.map(group -> jsonUtil.fromJson((String) group, JsonObject.class))
				.collect(Collectors.toList());
		return groups.stream().map(Group::new).collect(Collectors.toList());
	}

	@Override
    public ListResult<Group> getGroupsListMemberOf(String userId, String tenantId, Boolean blocked, int limit, long offset) {
		Stream<JsonObject> memberOfGroupStream = getAllMap(getGroupMembershipSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, userId), 
    			GROUPS_LIST_BIN_NAME).values().stream()
    			.map(group -> jsonUtil.fromJson((String) group, JsonObject.class));
		if (blocked != null) {
			memberOfGroupStream = memberOfGroupStream.filter(group -> {
				JsonElement groupBlocked = group.get(Group.PROPERTY_BLOCKED);
				return blocked == (groupBlocked != null && groupBlocked.isJsonPrimitive() && groupBlocked.getAsBoolean());
			});
		}
		List<JsonObject> groups = memberOfGroupStream.collect(Collectors.toList());
		JsonUtil.sort(groups, Arrays.asList(new OrderBy(Group.PROPERTY_NAME)));
		List<Group> results = groups.stream().skip(offset < 0 ? 0 : offset).limit(limit < 0 ? 0 : limit)
				.map(Group::new).collect(Collectors.toList());
		return new ListResult<>(limit, offset, groups.size(), results);
    }

    @Override
	public ListResult<String> listGroupUsers(String userId, String tenantId, String groupId, String ownerUsername,
			boolean includeOwner, int limit, long offset) {
    	Group group = getGroup(userId, tenantId, groupId, false);
    	assureUserIsGroupOwnerOrMember(group, userId);
		Map<String, String> memberUserIds = group.getMemberUserIds();
    	if (includeOwner && StringUtils.isNotBlank(ownerUsername)){
    		memberUserIds.put(group.getUserId(), ownerUsername);
		}

		List<Entry<String, String>> users = new ArrayList<>(memberUserIds.entrySet());
		users.sort((o1, o2) -> {
			int result = StringUtils.compare(o1.getValue(), o2.getValue());
			return result == 0 ? StringUtils.compare(o1.getKey(), o2.getKey()) : result;
		});

		List<String> results = users.stream().skip(offset < 0 ? 0 : offset).limit(limit < 0 ? 0 : limit)
				.map(user -> user.getKey()).collect(Collectors.toList());
		return new ListResult<>(limit, offset, users.size(), results);
    }

	@Override
	public boolean isPlayerInGroup(String userId, String tenantId, String groupId) {
    	Group group = getGroup(userId, tenantId, groupId, false);
		return group.getMemberUserIds().keySet().contains(userId);
	}

	@Override
	public void addBlockedGroups(String userId, String tenantId, String[] groupIds, String sortName) {
		togleGroupBlocking(userId, tenantId, groupIds, sortName, true);
	}

	@Override
	public void removeBlockedGroups(String userId, String tenantId, String[] groupIds) {
		togleGroupBlocking(userId, tenantId, groupIds, null, false);
	}

	private void togleGroupBlocking(String userId, String tenantId, String[] groupIds, String sortName, boolean block) {
		if (groupIds != null) {
			for (String groupId : groupIds) {
				String updatedGroupJson = updateJson(getGroupSet(), groupId, GROUP_BIN_NAME, (readGroup, writePolicy) -> {
					if (readGroup == null) {
			            throw new F4MEntryNotFoundException();
			        }
					Group group = new Group(jsonUtil.fromJson(readGroup, JsonObject.class));
					assureUserIsGroupOwnerOrMember(group, userId);
					if (block) {
						group.addBlockedUserIds(Collections.singletonMap(userId, sortName));
					} else {
						group.removeBlockedUserIds(userId);
					}
					return group.getAsString();
				});
		        Group group = new Group(jsonUtil.fromJson(updatedGroupJson, JsonObject.class));

				// Update user owned groups
		        addOrUpdateUserOwnedGroup(group.getUserId(), tenantId, groupId, group);
		        
				// Update all user membership groups
		        group.getMemberUserIds().keySet().forEach(uId -> addOrUpdateUserGroupMembership(uId, tenantId, groupId, group, false));
		        group.getBlockedUserIds().keySet().forEach(uId -> addOrUpdateUserGroupMembership(uId, tenantId, groupId, group, true));
			}
		}
	}
	
	private void assureUserIsGroupOwnerOrMember(Group group, String userId) {
		if (!StringUtils.equals(group.getUserId(), userId) && !group.getMemberUserIds().containsKey(userId)
				&& !group.getBlockedUserIds().containsKey(userId)) {
			throw new F4MInsufficientRightsException(
					String.format("User [%s] is neither owner nor member of a group [%s]", userId, group.getGroupId()));
		}
	}

	@Override
	public void moveGroups(String sourceUserId, String targetUserId, Collection<String> sourceTenantIds, String targetSearchName) {
		sourceTenantIds.forEach(tenantId -> {
			// Move user groups
			moveUserOwnedGroups(sourceUserId, targetUserId, tenantId);
			
			// Update user references in groups
			Map<String, String> userInGroups = getAllMap(getGroupMembershipSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, sourceUserId), GROUPS_LIST_BIN_NAME);
			if (MapUtils.isNotEmpty(userInGroups)) {
				updateGroupMembers(sourceUserId, targetUserId, targetSearchName, userInGroups);
				moveGroupMemberships(sourceUserId, targetUserId, userInGroups, tenantId);
			}
		});
	}

	private void moveUserOwnedGroups(String sourceUserId, String targetUserId, String tenantId) {
		String sourceKey = primaryKeyUtil.createGroupListPrimaryKey(tenantId, sourceUserId);
		List<Group> groups = getAllMap(getOwnedGroupSet(), sourceKey, GROUPS_LIST_BIN_NAME).values().stream()
				.map(group -> new Group(jsonUtil.fromJson((String) group, JsonObject.class)))
				.filter(group -> {
					try {
						addUniqueGroupKeyEntry(group.getGroupId(), tenantId, sourceUserId, group.getName());
						group.setUserId(targetUserId);
						return true;
					} catch (F4MEntryAlreadyExistsException e) {
						// Skip groups which already exist in the target user (simply delete them)
						deleteGroup(sourceUserId, tenantId, group.getGroupId());
						removeUniqueGroupKeyEntry(tenantId, sourceUserId, group.getName());
						return false; 
					}
				})
				.collect(Collectors.toList());
		if (! groups.isEmpty()) {
			createOrUpdateMap(getOwnedGroupSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, targetUserId), GROUPS_LIST_BIN_NAME, (readValue, writePolicy) -> {
				if (readValue == null) {
					readValue = new HashMap<>(groups.size()); 
				}
				for (Group group : groups) {
					readValue.put(group.getGroupId(), group.getAsString());
				}
				return readValue;
			});
			moveGroups(groups, targetUserId);
		}
		deleteSilently(getOwnedGroupSet(), sourceKey);
	}

	private void moveGroups(Collection<Group> groups, String targetUserId) {
		groups.forEach(group -> updateJson(getGroupSet(), group.getGroupId(), GROUP_BIN_NAME, (readGroup, writePolicy) -> {
			if (readGroup == null) {
	            throw new F4MEntryNotFoundException();
	        }
			Group g = new Group(jsonUtil.fromJson(readGroup, JsonObject.class));
			g.setUserId(targetUserId);
			return g.getAsString();
		}));
	}
	
	private void updateGroupMembers(String sourceUserId, String targetUserId, String targetSearchName, Map<String, String> userInGroups) {
        userInGroups.keySet().forEach(groupId -> updateJson(getGroupSet(), groupId, GROUP_BIN_NAME, (readGroup, writePolicy) -> {
			if (readGroup == null) {
	            throw new F4MEntryNotFoundException();
	        }
			Group group = new Group(jsonUtil.fromJson(readGroup, JsonObject.class));
			if (group.getMemberUserIds().containsKey(sourceUserId)) {
				group.removeMemberUserIds(sourceUserId);
				group.addMemberUserIds(Collections.singletonMap(targetUserId, targetSearchName));
			}
			return group.getAsString();
		}));
	}

	private void moveGroupMemberships(String sourceUserId, String targetUserId, Map<String, String> userInGroups, String tenantId) {
		createOrUpdateMap(getGroupMembershipSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, targetUserId), GROUPS_LIST_BIN_NAME, (readValue, writePolicy) -> {
			if (readValue == null) {
				readValue = new HashMap<>(userInGroups.size());
			}
			readValue.putAll(userInGroups);
			return readValue;
		});
		deleteSilently(getGroupMembershipSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, sourceUserId));
	}

    private void addOrUpdateUserGroupMembership(String userId, String tenantId, String groupId, Group group, boolean blocked) {
    	group.setBlocked(blocked);
    	createOrUpdateMapValueByKey(getGroupMembershipSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, userId), 
    			GROUPS_LIST_BIN_NAME, groupId, (readValue, writePolicy) -> group.getAsStringWithoutMembers(false));
	}

    private void removeUserGroupMembership(String userId, String tenantId, String groupId) {
    	deleteByKeyFromMapSilently(getGroupMembershipSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, userId), 
    			GROUPS_LIST_BIN_NAME, groupId);
	}

    private void addOrUpdateUserOwnedGroup(String userId, String tenantId, String groupId, Group group) {
		createOrUpdateMapValueByKey(getOwnedGroupSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, userId), 
				GROUPS_LIST_BIN_NAME, groupId, (readValue, writePolicy) -> group.getAsStringWithoutMembers(true));
    }

    private void removeUserOwnedGroup(String userId, String tenantId, String groupId) {
    	deleteByKeyFromMapSilently(getOwnedGroupSet(), primaryKeyUtil.createGroupListPrimaryKey(tenantId, userId), 
    			GROUPS_LIST_BIN_NAME, groupId);
	}

    private void addUniqueGroupKeyEntry(String groupId, String tenantId, String userId, String name) {
		String uniqueKey = primaryKeyUtil.createGroupUniqueNamePrimaryKey(tenantId, userId, name);
		String existingGroupId = readString(getGroupSet(), uniqueKey, GroupAerospikeDAO.GROUP_ID_BIN_NAME);
		if (existingGroupId != null) {
			if (! existingGroupId.equals(groupId)) {
				throw new F4MEntryAlreadyExistsException(String.format(
						"Group with given name %s already exists for tenant %s and user %s", name, tenantId, userId));
			}
		} else {
    		try {
            	createString(getGroupSet(), uniqueKey, GroupAerospikeDAO.GROUP_ID_BIN_NAME, groupId);
    		} catch (AerospikeException e) {
    			if (e.getResultCode() == ResultCode.KEY_EXISTS_ERROR) {
    				throw new F4MEntryAlreadyExistsException(String.format(
    						"Group with given name %s already exists for tenant %s and user %s", name, tenantId, userId));
    			} else {
    				throw e;
    			}
    		}
		}
    }

    private void removeUniqueGroupKeyEntry(String tenantId, String userId, String name) {
        deleteSilently(getGroupSet(), primaryKeyUtil.createGroupUniqueNamePrimaryKey(tenantId, userId, name));
    }

    private String getGroupSet() {
        return config.getProperty(FriendManagerConfig.AEROSPIKE_GROUP_SET);
    }

    private String getOwnedGroupSet() {
        return config.getProperty(FriendManagerConfig.AEROSPIKE_OWNED_GROUP_SET);
    }

    private String getGroupMembershipSet() {
    	return config.getProperty(FriendManagerConfig.AEROSPIKE_GROUP_MEMBERSHIP_SET);
    }

}
