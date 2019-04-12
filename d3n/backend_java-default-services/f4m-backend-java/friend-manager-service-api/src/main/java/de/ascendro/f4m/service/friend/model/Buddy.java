package de.ascendro.f4m.service.friend.model;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.util.F4MEnumUtils;

public class Buddy extends JsonObjectWrapper {
	
    public static final String PROPERTY_OWNER_ID = "buddyOwnerId";
    public static final String PROPERTY_USER_ID = "buddyUserId";
    public static final String PROPERTY_IS_FAVORITE = "favorite";
    public static final String PROPERTY_EMAIL = "emails.email";
    public static final String PROPERTY_RELATION_TYPES = "relationTypes";
    public static final String PROPERTY_INTERACTION_COUNT = "interactionCount";
    public static final String PROPERTY_LAST_PLAYED_GAME_INSTANCE_ID = "lastPlayedGameInstanceId";
    public static final String PROPERTY_LAST_INTERACTION_TIMESTAMP = "lastInteractionTimestamp";
    public static final String PROPERTY_TENANT_IDS = "buddyTenantIds";

    public Buddy() {
    	super();
    }
    
    public Buddy(String ownerId, BuddyRelationType relationType, String userId, String tenantId, boolean favorite) {
    	setOwnerId(ownerId);
    	addRelationTypes(relationType);
    	setUserId(userId);
    	addTenantIds(tenantId);
    	setFavorite(favorite);
    }

    public Buddy(JsonObject buddy) {
    	super(buddy);
    }

	public void update(Buddy buddy) {
		if (getRelationTypes().isEmpty()) {
			List<BuddyRelationType> buddyRelationTypes = buddy.getRelationTypes();
			addRelationTypes(buddyRelationTypes.toArray(new BuddyRelationType[buddyRelationTypes.size()]));
		}
		if (buddy.getInteractionCount() != null) {
			setInteractionCount(buddy.getInteractionCount());
		}
		if (buddy.getLastPlayedGameInstanceId() != null) {
			setLastPlayedGameInstanceId(buddy.getLastPlayedGameInstanceId());
		}
		if (buddy.getLastInteractionTimestamp() != null) {
			setLastInteractionTimestamp(buddy.getLastInteractionTimestamp());
		}
		if (buddy.isFavorite() != null) {
			setFavorite(buddy.isFavorite());
		}
	}

	public String getId() {
    	return getBuddyId(getOwnerId(), getUserId());
    }

	public static String getBuddyId(String ownerId, String userId) {
		return new StringBuilder(ownerId).append("_").append(userId).toString();
	}
	
	public String getOwnerId() {
    	return getPropertyAsString(PROPERTY_OWNER_ID);
    }
    
    public void setOwnerId(String ownerId) {
    	setProperty(PROPERTY_OWNER_ID, ownerId);
    }
    
    public Boolean isFavorite() {
    	return getPropertyAsBoolean(PROPERTY_IS_FAVORITE);
    }
    
    public void setFavorite(Boolean favorite) {
    	setProperty(PROPERTY_IS_FAVORITE, favorite);
    }
    
	public String getUserId() {
    	return getPropertyAsString(PROPERTY_USER_ID);
    }
    
    public void setUserId(String userId) {
    	setProperty(PROPERTY_USER_ID, userId);
    }
    
    public List<BuddyRelationType> getRelationTypes() {
    	String[] relationTypes = getPropertyAsStringArray(PROPERTY_RELATION_TYPES);
    	return relationTypes == null ? Collections.emptyList()
    			: Arrays.stream(relationTypes).map(r -> F4MEnumUtils.getEnum(BuddyRelationType.class, r)).collect(Collectors.toList());
    }

    public long addRelationTypes(BuddyRelationType... relationTypes) {
        AtomicLong relationCount = new AtomicLong(0);
    	List<BuddyRelationType> existingRelationTypes = getRelationTypes();

    	Arrays.stream(relationTypes).forEach(relationType -> {
    	    if (!existingRelationTypes.contains(relationType)) {
                addElementToArray(PROPERTY_RELATION_TYPES, new JsonPrimitive(relationType.name()));
                relationCount.getAndIncrement();
            }
        });

    	return relationCount.get();
    }

    public long deleteRelationTypes(BuddyRelationType... types) {
    	JsonArray existingTypes = getArray(PROPERTY_RELATION_TYPES);
        return Arrays.stream(types)
                .filter(type -> existingTypes.remove(new JsonPrimitive(type.name())))
                .count();
    }
    
    public Integer getInteractionCount() {
    	return getPropertyAsInteger(PROPERTY_INTERACTION_COUNT);
    }
    
    public void setInteractionCount(int interactionCount) {
    	setProperty(PROPERTY_INTERACTION_COUNT, interactionCount);
    }

    public String getLastPlayedGameInstanceId() {
    	return getPropertyAsString(PROPERTY_LAST_PLAYED_GAME_INSTANCE_ID);
    }
    
    public void setLastPlayedGameInstanceId(String lastPlayedGameInstanceId) {
    	setProperty(PROPERTY_LAST_PLAYED_GAME_INSTANCE_ID, lastPlayedGameInstanceId);
    }
    
	public ZonedDateTime getLastInteractionTimestamp() {
		return getPropertyAsZonedDateTime(PROPERTY_LAST_INTERACTION_TIMESTAMP);
	}
	
	public void setLastInteractionTimestamp(ZonedDateTime lastInteractionTimestamp) {
		setProperty(PROPERTY_LAST_INTERACTION_TIMESTAMP, lastInteractionTimestamp);
	}
	
	public String[] getTenantIds() {
		return getPropertyAsStringArray(PROPERTY_TENANT_IDS);
	}

	public void addTenantIds(String... tenants) {
		Arrays.stream(tenants).forEach(t -> addElementToArray(PROPERTY_TENANT_IDS, new JsonPrimitive(t)));
	}

}
