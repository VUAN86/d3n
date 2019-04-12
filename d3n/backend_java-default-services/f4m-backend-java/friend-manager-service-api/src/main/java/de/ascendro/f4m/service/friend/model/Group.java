package de.ascendro.f4m.service.friend.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class Group extends JsonObjectWrapper {
	
    public static final String PROPERTY_GROUP_ID = "groupId";
    public static final String PROPERTY_USER_ID = "userId";
	public static final String PROPERTY_NAME = "name";
	public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_IMAGE = "image";
    public static final String PROPERTY_BUDDY_COUNT = "buddyCount";
    public static final String PROPERTY_MEMBER_USER_IDS = "memberUserIds";
    public static final String PROPERTY_BLOCKED_USER_IDS = "blockedUserIds";
    public static final String PROPERTY_SORT_FIELD = "sortField";
    public static final String PROPERTY_TENANT_ID = "tenantId";
    public static final String PROPERTY_BLOCKED = "blocked";

    public Group(String groupId, String tenantId, String userId, String name, String type) {
    	super();
    	setGroupId(groupId);
    	setTenantId(tenantId);
    	setUserId(userId);
    	setName(name);
		setType(type);
    }

    public Group(JsonObject group) {
    	super(group);
    }

    public String getGroupId() {
    	return getPropertyAsString(PROPERTY_GROUP_ID);
    }
    
    private void setGroupId(String groupId) {
    	setProperty(PROPERTY_GROUP_ID, groupId);
    }
    
    public String getUserId() {
    	return getPropertyAsString(PROPERTY_USER_ID);
    }
    
    public void setUserId(String userId) {
    	setProperty(PROPERTY_USER_ID, userId);
    }
    
    public String getName() {
    	return getPropertyAsString(PROPERTY_NAME);
    }
    
    public void setName(String name) {
    	setProperty(PROPERTY_NAME, name);
    }

    public String getType() {
    	return getPropertyAsString(PROPERTY_TYPE);
    }

    public void setType(String type) {
    	setProperty(PROPERTY_TYPE, type);
    }
    
    public String getImage() {
    	return getPropertyAsString(PROPERTY_IMAGE);
    }
    
    public void setImage(String image) {
    	setProperty(PROPERTY_IMAGE, image);
    }
    
    public int getBuddyCount() {
    	Integer buddyCount = getPropertyAsInteger(PROPERTY_BUDDY_COUNT);
    	return buddyCount == null ? 0 : buddyCount;
    }
    
    private void setBuddyCount(int buddyCount) {
    	setProperty(PROPERTY_BUDDY_COUNT, buddyCount);
    }

    public Map<String, String> getMemberUserIds() {
    	JsonArray memberUserIds = getArray(PROPERTY_MEMBER_USER_IDS);
    	if (memberUserIds == null) {
    		return new HashMap<>();
    	}
    	Map<String, String> result = new HashMap<>(memberUserIds.size());
    	memberUserIds.forEach(memberUserId -> {
    		JsonObject obj = memberUserId.getAsJsonObject();
    		JsonElement sortField = obj.get(PROPERTY_SORT_FIELD);
    		result.put(obj.get(PROPERTY_USER_ID).getAsString(), sortField == null || sortField.isJsonNull() ? null : sortField.getAsString());
    	});
    	return result;
    }
    
    private void setMemberUserIds(Map<String, String> memberUserIds) {
    	JsonArray array = new JsonArray();
    	if (memberUserIds != null) {
	    	memberUserIds.forEach((key, value) -> {
	    		JsonObject memberUserId = new JsonObject();
	    		memberUserId.addProperty(PROPERTY_USER_ID, key);
	    		memberUserId.addProperty(PROPERTY_SORT_FIELD, value);
	    		array.add(memberUserId);
	    	});
    	}
    	setProperty(PROPERTY_MEMBER_USER_IDS, array);
    	setBuddyCount(array.size());
    }
    
    public void removeMemberUserIds(String... memberUserIds) {
    	if (ArrayUtils.isNotEmpty(memberUserIds)) {
	    	Map<String, String> memberUserIdMap = getMemberUserIds();
	    	Arrays.stream(memberUserIds).forEach(memberUserIdMap::remove);
	    	setMemberUserIds(memberUserIdMap);
    	}
    }
    
    public void addMemberUserIds(Map<String, String> memberUserIds) {
    	if (MapUtils.isNotEmpty(memberUserIds)) {
    		Map<String, String> memberUserIdMap = getMemberUserIds();
	    	Map<String, String> blockedUserIdMap = getBlockedUserIds();
	    	memberUserIds.forEach((key, value) -> {
	    		if (! blockedUserIdMap.containsKey(key)) {
	    			memberUserIdMap.put(key, value);
	    		}
	    	});
	    	setMemberUserIds(memberUserIdMap);
    	}
    }
    
    public Map<String, String> getBlockedUserIds() {
    	JsonArray blockedUserIds = getArray(PROPERTY_BLOCKED_USER_IDS);
    	if (blockedUserIds == null) {
    		return new HashMap<>();
    	}
    	Map<String, String> result = new HashMap<>(blockedUserIds.size());
    	blockedUserIds.forEach(blockedUserId -> {
    		JsonObject obj = blockedUserId.getAsJsonObject();
    		JsonElement sortField = obj.get(PROPERTY_SORT_FIELD);
    		result.put(obj.get(PROPERTY_USER_ID).getAsString(), sortField == null || sortField.isJsonNull() ? null : sortField.getAsString());
    	});
    	return result;
    }
    
    private void setBlockedUserIds(Map<String, String> blockedUserIds) {
    	JsonArray array = new JsonArray();
    	if (blockedUserIds != null) {
	    	blockedUserIds.forEach((key, value) -> {
	    		JsonObject blockedUserId = new JsonObject();
	    		blockedUserId.addProperty(PROPERTY_USER_ID, key);
	    		blockedUserId.addProperty(PROPERTY_SORT_FIELD, value);
	    		array.add(blockedUserId);
	    	});
    	}
    	setProperty(PROPERTY_BLOCKED_USER_IDS, array);
    }
    
    public void removeBlockedUserIds(String... blockedUserIds) {
    	if (ArrayUtils.isNotEmpty(blockedUserIds)) {
	    	Map<String, String> blockedUserIdMap = getBlockedUserIds();
	    	Map<String, String> renewedUserIdMap = new HashMap<>();
	    	Arrays.stream(blockedUserIds).forEach(blockedUserId -> {
	    		String searchName = blockedUserIdMap.remove(blockedUserId);
	    		if (searchName != null) {
	    			renewedUserIdMap.put(blockedUserId, searchName);
	    		}
	    	});
	    	setBlockedUserIds(blockedUserIdMap);
	    	addMemberUserIds(renewedUserIdMap);
    	}
    }
    
    public void addBlockedUserIds(Map<String, String> blockedUserIds) {
    	if (MapUtils.isNotEmpty(blockedUserIds)) {
	    	Map<String, String> blockedUserIdMap = getBlockedUserIds();
	    	blockedUserIdMap.putAll(blockedUserIds);
	    	setBlockedUserIds(blockedUserIdMap);
	    	removeMemberUserIds(blockedUserIds.keySet().toArray(new String[blockedUserIds.keySet().size()]));
    	}
    }
    
	public String getTenantId() {
		return getPropertyAsString(PROPERTY_TENANT_ID);
	}

	private void setTenantId(String tenantId) {
		setProperty(PROPERTY_TENANT_ID, tenantId);
	}

	public boolean isBlocked() {
		Boolean blocked = getPropertyAsBoolean(PROPERTY_BLOCKED);
		return blocked != null && blocked;
	}
	
	public void setBlocked(boolean blocked) {
		setProperty(PROPERTY_BLOCKED, blocked);
	}
	
    public String getAsStringWithoutMembers(boolean excludeBlocked) {
    	JsonElement memberUserIds = jsonObject.get(PROPERTY_MEMBER_USER_IDS);
    	JsonElement blockedUserIds = jsonObject.get(PROPERTY_BLOCKED_USER_IDS);
    	JsonElement blocked = jsonObject.get(PROPERTY_BLOCKED);
    	jsonObject.remove(PROPERTY_MEMBER_USER_IDS);
    	jsonObject.remove(PROPERTY_BLOCKED_USER_IDS);
    	if (excludeBlocked) {
    		jsonObject.remove(PROPERTY_BLOCKED);
    	}
    	String result = getAsString();
    	jsonObject.add(PROPERTY_MEMBER_USER_IDS, memberUserIds);
    	jsonObject.add(PROPERTY_BLOCKED_USER_IDS, blockedUserIds);
    	if (excludeBlocked) {
    		jsonObject.add(PROPERTY_BLOCKED, blocked);
    	}
    	return result;
    }
    
}
