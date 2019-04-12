package de.ascendro.f4m.service.friend.dao;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.friend.config.FriendManagerConfig;
import de.ascendro.f4m.service.friend.exception.F4MBlockedException;
import de.ascendro.f4m.service.friend.exception.F4MCannotReferenceSelfAsBuddyException;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.util.BuddyPrimaryKeyUtil;
import de.ascendro.f4m.service.profile.model.Profile;

public class BuddyAerospikeDaoImpl extends AerospikeOperateDaoImpl<BuddyPrimaryKeyUtil> implements BuddyAerospikeDao {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BuddyAerospikeDaoImpl.class);
	
	private static final String VALUE_BIN_NAME = "value";
	
	private BuddyElasticDao buddyElasticDao;
	private CommonProfileAerospikeDao profileDao;
	
	@Inject
	public BuddyAerospikeDaoImpl(Config config, BuddyPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider, BuddyElasticDao buddyElasticDao,
			CommonProfileAerospikeDao profileDao) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
		this.buddyElasticDao = buddyElasticDao;
		this.profileDao = profileDao;
	}

	@Override
	public void createOrUpdateBuddy(String ownerId, String tenantId, Buddy buddy) {
		String buddyId = buddy.getUserId();
		if (StringUtils.equals(ownerId, buddyId)) {
			throw new F4MCannotReferenceSelfAsBuddyException("Trying to add self as a buddy: " + ownerId);
		}
		String buddyKey = primaryKeyUtil.createBuddyPrimaryKey(ownerId, buddyId);
		String updated = createOrUpdateJson(getSet(), buddyKey, VALUE_BIN_NAME, (existing, wp) -> {
			Buddy existingBuddy = existing == null ? buddy : new Buddy(jsonUtil.fromJson(existing, JsonObject.class));
			if (existing != null) {
				assureCorrectBuddyOwner(existingBuddy, ownerId, tenantId);
				// It's okay if we loose some info on parallel updates - this is more statistical information
				existingBuddy.update(buddy);
			}
			return existingBuddy.getAsString();
		});
		updateElasticBuddy(updated);
	}
	
	@Override
	public boolean createBuddyRelation(String ownerId, String buddyId, String tenantId, BuddyRelationType relationType, Boolean favorite) {
		if (StringUtils.equals(ownerId, buddyId)) {
			throw new F4MCannotReferenceSelfAsBuddyException("Trying to reference self as a buddy: " + ownerId);
		}
		final AtomicBoolean relationAdded = new AtomicBoolean(false);
		if (!StringUtils.equals(ownerId, buddyId)) {
			String buddyKey = primaryKeyUtil.createBuddyPrimaryKey(ownerId, buddyId);
			String updated = createOrUpdateJson(getSet(), buddyKey, VALUE_BIN_NAME, (existing, wp) -> {
				Buddy existingBuddy = existing == null ? null : new Buddy(jsonUtil.fromJson(existing, JsonObject.class));
				if (existingBuddy == null) {
					existingBuddy = new Buddy(ownerId, relationType, buddyId, tenantId, favorite == Boolean.TRUE);
					relationAdded.set(true);
				} else {
					relationAdded.set(existingBuddy.addRelationTypes(relationType)==1);
					existingBuddy.addTenantIds(tenantId);
					if (favorite != null) {
						existingBuddy.setFavorite(favorite);
					}
				}
				return existingBuddy.getAsString();
			});
			updateElasticBuddy(updated);
			if (relationType == BuddyRelationType.BLOCKED) {
				createBuddyRelation(buddyId, ownerId, tenantId, BuddyRelationType.BLOCKED_BY, null);
			}
		} else {
			LOGGER.warn("User [{}] tried to create buddy relation with him(her)self", ownerId);
		}
		return relationAdded.get();
	}

	/** 
	 * Delete buddy relation. 
	 * For BLOCKED relation will also remove the reverse relation BLOCKED_BY (also set to BUDDY if any additional info exists). 
	 */
	@Override
	public boolean deleteBuddyRelation(String ownerId, String buddyId, String tenantId, BuddyRelationType relationType) {
		if (relationType == BuddyRelationType.BLOCKED_BY) {
			throw new F4MBlockedException("May not remove " + relationType + " relation for userId " + ownerId + ", buddyId " + buddyId);
		}
		boolean relationDeleted = doDeleteBuddyRelation(ownerId, buddyId, tenantId, relationType);
		if (relationType == BuddyRelationType.BLOCKED) {
			doDeleteBuddyRelation(buddyId, ownerId, tenantId, BuddyRelationType.BLOCKED_BY);
		}

		return relationDeleted;
	}

	private boolean doDeleteBuddyRelation(String ownerId, String buddyId, String tenantId, BuddyRelationType relationType) {
		final AtomicBoolean relationDeleted = new AtomicBoolean(false);
		String buddyKey = primaryKeyUtil.createBuddyPrimaryKey(ownerId, buddyId);
		if (exists(getSet(), buddyKey)) {
			String updated = createOrUpdateJson(getSet(), buddyKey, VALUE_BIN_NAME, (existing, wp) -> {
				if (existing == null) {
					return null;
				} else {
					Buddy existingBuddy = jsonUtil.toJsonObjectWrapper(existing, Buddy::new);
					assureCorrectBuddyOwner(existingBuddy, ownerId, tenantId);
					relationDeleted.set(existingBuddy.deleteRelationTypes(relationType)==1);
					return existingBuddy.getAsString();
				}
			});
			updateElasticBuddy(updated);
		}

		return relationDeleted.get();
	}

	@Override
	public List<Buddy> getBuddies(String ownerId, List<String> buddiesIds) {
		return getJsonObjects(getSet(), VALUE_BIN_NAME, buddiesIds,
				id -> primaryKeyUtil.createBuddyPrimaryKey(ownerId, id),
				s -> jsonUtil.toJsonObjectWrapper(s, Buddy::new), true);
	}
	
	@Override
	public void deleteBuddy(String ownerId, String buddyId) {
		String buddyKey = primaryKeyUtil.createBuddyPrimaryKey(ownerId, buddyId);
		delete(getSet(), buddyKey);
		buddyElasticDao.deleteBuddy(ownerId, buddyId, true, true);
	}

	@Override
	public void moveBuddy(String sourceBuddyOwnerId, String sourceBuddyUserId, String targetBuddyOwnerId, String targetBuddyUserId) {
		String buddyKey = primaryKeyUtil.createBuddyPrimaryKey(sourceBuddyOwnerId, sourceBuddyUserId);
		Buddy buddy = jsonUtil.toJsonObjectWrapper(readJson(getSet(), buddyKey, VALUE_BIN_NAME), Buddy::new);
		buddy.setOwnerId(targetBuddyOwnerId);
		buddy.setUserId(targetBuddyUserId);
		String targetBuddyKey = primaryKeyUtil.createBuddyPrimaryKey(targetBuddyOwnerId, targetBuddyUserId);
		String updated = createOrUpdateJson(getSet(), targetBuddyKey, VALUE_BIN_NAME, (existing, wp) -> {
			Buddy existingBuddy = existing == null ? buddy : new Buddy(jsonUtil.fromJson(existing, JsonObject.class));
			if (existing != null) {
				// It's okay if we loose some info on parallel updates - this is more statistical information
				existingBuddy.update(buddy);
			}
			return existingBuddy.getAsString();
		});
		updateElasticBuddy(updated);
		deleteBuddy(sourceBuddyOwnerId, sourceBuddyUserId);
	}

	@Override
	public int resyncIndex() {
		AtomicInteger scanned = new AtomicInteger();
		getAerospikeClient().scanAll(null, getNamespace(), getSet(), (key, record) -> {
			String buddyJson = readJson(VALUE_BIN_NAME, record);
			try {
				updateElasticBuddy(buddyJson);
			} catch (Exception e) {
				LOGGER.error("Could not sync buddy data", e);
				LOGGER.error("Buddy data: " + buddyJson);
			}
			scanned.incrementAndGet();
		}, VALUE_BIN_NAME);
		return scanned.get();
	}
	
	private String getSet() {
		return config.getProperty(FriendManagerConfig.AEROSPIKE_BUDDY_SET);
	}

	private void assureCorrectBuddyOwner(Buddy buddy, String userId, String tenantId) {
		if (! StringUtils.equals(buddy.getOwnerId(), userId)) {
			throw new F4MInsufficientRightsException("Owner ID of buddy (" + buddy.getUserId() + 
					") does not match the user ID of requestor (" + userId + ")");
		}
		if (! ArrayUtils.contains(buddy.getTenantIds(), tenantId)) {
			throw new F4MInsufficientRightsException("Tenant IDs of buddy (" + buddy.getTenantIds() + 
					") does not contain the tenant ID of requestor (" + tenantId + ")");
		}
	}
	
	private void updateElasticBuddy(String buddyJson) {
		if (buddyJson != null) {
			Buddy buddy = new Buddy(jsonUtil.fromJson(buddyJson, JsonObject.class));
			Profile profile = profileDao.getProfile(buddy.getUserId());
			buddyElasticDao.createOrUpdate(profile == null ? null : profile.getSearchName(), buddy);
		}
	}

}
