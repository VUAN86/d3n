package de.ascendro.f4m.service.friend.dao;

import java.util.List;

import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;

public interface BuddyAerospikeDao {

	/** 
	 * Update / create the core information of buddy (relation type will be set only if creating or 
	 * if updating and no relations exist, otherwise it will remain to original value). 
	 */
	void createOrUpdateBuddy(String ownerId, String tenantId, Buddy buddy);

	/** 
	 * Add buddy relation. If any information exists for userId-buddyId pair => only update relations.
	 * For BLOCKED relation will also set up the reverse relation BLOCKED_BY.
	 */
	boolean createBuddyRelation(String ownerId, String buddyId, String tenantId, BuddyRelationType relationType, Boolean favorite);

	/** 
	 * Delete buddy relation. If any information exists for userId-buddyId pair => only update relations.
	 * For BLOCKED relation will also remove the reverse relation BLOCKED_BY. 
	 * BLOCKED_BY relation cannot be removed. Will result in ERR_BLOCKED.
	 */
	boolean deleteBuddyRelation(String ownerId, String buddyId, String tenantId, BuddyRelationType relationType);

	/** 
	 * Get buddies. 
	 */
	List<Buddy> getBuddies(String ownerId, List<String> buddiesIds);

	/**
	 * Delete buddy.
	 */
	void deleteBuddy(String ownerId, String buddyId);

	/**
	 * Resync elastic index.
	 */
	int resyncIndex();

	/**
	 * Move buddy to target user.
	 */
	void moveBuddy(String sourceBuddyOwnerId, String sourceBuddyUserId, String targetBuddyOwnerId, String targetBuddyUserId);

}
