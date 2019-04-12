package de.ascendro.f4m.server.multiplayer.dao;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.service.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiplayerGameInstancePrimaryKeyUtil extends PrimaryKeyUtil<String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(MultiplayerGameInstancePrimaryKeyUtil.class);

	private static final String KEY_PART_META = "meta";
	private static final String KEY_PART_USER = "user";
	private static final String KEY_PART_TENANT = "tenant";
	private static final String KEY_PART_APP = "app";
	private static final String KEY_GAME_INSTANCE_ID = "gameInstanceId";
	private static final String KEY_GAME_INSTANCES = "gameInstances";
	private static final String KEY_TOURNAMENT = "tournament";
	private static final String KEY_MGI_PREFIX = "mgi";
	private static final String NUMBER_OF_ATTEMPTS = "count";

	@Inject
	public MultiplayerGameInstancePrimaryKeyUtil(Config config) {
		super(config);
	}

	/**
	 * 
	 * @param mgiId
	 * @param recordNumber
	 * @return instance record key in form of mgi:[mgiId]:[recordNumber]
	 */
	public String createInstanceRecordKey(String mgiId, long recordNumber) {
		return getServiceName() + KEY_ITEM_SEPARATOR + mgiId + KEY_ITEM_SEPARATOR + Long.toString(recordNumber);
	}

	/**
	 * 
	 * @param mgiId
	 * @param userId
	 * @return index key to MGI record in form of mgi:[mgiId]:user:[userId]
	 */
	public String createIndexKeyToInstanceRecord(String mgiId, String userId) {
		return getServiceName() + KEY_ITEM_SEPARATOR + mgiId + KEY_ITEM_SEPARATOR + KEY_PART_USER
				+ KEY_ITEM_SEPARATOR + userId;
	}

    public String createIndexKeyToInstance(String mgiId, String userId, Long numberOfAttempts) {
        return getServiceName() + KEY_ITEM_SEPARATOR + mgiId + KEY_ITEM_SEPARATOR + KEY_PART_USER
                + KEY_ITEM_SEPARATOR + userId + KEY_ITEM_SEPARATOR + NUMBER_OF_ATTEMPTS + KEY_ITEM_SEPARATOR +numberOfAttempts;
    }

	/**
	 * 
	 * @param mgiId
	 * @return meta key in form of mgi:[mgiId]:meta
	 */
	public String createMetaKey(String mgiId) {
		return getServiceName() + KEY_ITEM_SEPARATOR + mgiId + KEY_ITEM_SEPARATOR + KEY_PART_META;
	}

	/**
	 *
	 * @param mgiId
	 * @return meta key in form of mgi:[mgiId]:meta
	 */
	public String createMetaMgiKey(String mgiId) {
		return KEY_MGI_PREFIX + KEY_ITEM_SEPARATOR + mgiId + KEY_ITEM_SEPARATOR + KEY_PART_META;
	}

	/**
	 * 
	 * @param tenantId
	 * @param appId TODO
	 * @param userId
	 * @return invitation list key in form of mgi:tenant:[tenantId]:user:[userId]
	 */
	public String createInvitationListKey(String tenantId, String appId, String userId) {
		return getServiceName() 
				+ KEY_ITEM_SEPARATOR + KEY_PART_TENANT + KEY_ITEM_SEPARATOR + tenantId
				+ KEY_ITEM_SEPARATOR + KEY_PART_APP + KEY_ITEM_SEPARATOR + appId
				+ KEY_ITEM_SEPARATOR + KEY_PART_USER + KEY_ITEM_SEPARATOR + userId;
	}
	
	/**
	 * 
	 * @param mgiId
	 * @param userId
	 * @return key for game instance reference in form of mgi:[mgiId]:user:[userId]:gameInstanceId
	 */
	public String createGameInstanceIdKey(String mgiId, String userId) {
		return getServiceName() + KEY_ITEM_SEPARATOR + mgiId + KEY_ITEM_SEPARATOR + KEY_PART_USER + KEY_ITEM_SEPARATOR
				+ userId + KEY_ITEM_SEPARATOR + KEY_GAME_INSTANCE_ID;
	}
	
	/**
	 * 
	 * @param mgiId
	 * @return game instance counter key in form of mgi:[mgiId]:gameInstances
	 */
	public String createGameInstanceCounterKey(String mgiId) {
		return getServiceName() + KEY_ITEM_SEPARATOR + mgiId + KEY_ITEM_SEPARATOR + KEY_GAME_INSTANCES;
	}
	
	/**
	 * 
	 * @param gameId
	 * @return live tournament mapping key in form of mgi:tournament:[gameId]
	 */
	public String createTournamentMappingKey(String gameId) {
		return getServiceName() + KEY_ITEM_SEPARATOR + KEY_TOURNAMENT + KEY_ITEM_SEPARATOR + gameId;
	}

	/**
	 *
	 * @param gameId
	 * @return live tournament mapping key in form of mgi:tournament:[gameId]
	 */
	public String createTournamentMappingMgiKey(String gameId) {
		return KEY_MGI_PREFIX + KEY_ITEM_SEPARATOR + KEY_TOURNAMENT + KEY_ITEM_SEPARATOR + gameId;
	}

	@Override
	protected String getServiceName() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_MULTIPLAYER_KEY_PREFIX);
	}

}
