package de.ascendro.f4m.server.friend;

import java.util.List;

import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;

public interface CommonBuddyElasticDao {
	
    boolean isMyBuddy(String ownerId, String buddyId);
    
    boolean isBlockingMe(String ownerId, String blockerId);

	List<String> getAllBuddyIds(String userId, String appId, String tenantId,
			BuddyRelationType[] includedRelationTypes, BuddyRelationType[] excludedRelationTypes, String buddyUserId);

}
