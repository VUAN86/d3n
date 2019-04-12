package de.ascendro.f4m.service.friend.dao;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListOrderType;
import de.ascendro.f4m.service.json.model.ListResult;

public interface BuddyElasticDao {

	void createOrUpdate(String searchName, Buddy buddy);

	ListResult<Buddy> getBuddyList(String ownerId, String appId, String tenantId, String searchTerm, String email,
			BuddyRelationType[] includedRelationTypes, BuddyRelationType[] excludedRelationTypes, 
			String[] excludeUserIds, Boolean favorite, BuddyListOrderType orderType, int limit, long offset);

	List<String> getAllOwnerIds(String buddyId);

	void deleteBuddy(String ownerId, String buddyId, boolean wait, boolean silent);
	
	/** Get rank of the given user among buddies (left - rank, right - total buddy count). */
	Pair<Long, Long> getRank(String userId, String appId, String tenantId, Double handicap);

}
