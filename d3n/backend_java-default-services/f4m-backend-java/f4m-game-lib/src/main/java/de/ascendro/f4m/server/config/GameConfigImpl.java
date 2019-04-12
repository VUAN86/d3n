package de.ascendro.f4m.server.config;

import com.google.common.io.Resources;

import de.ascendro.f4m.server.multiplayer.dao.PublicGameElasticDao;
import de.ascendro.f4m.service.config.F4MConfig;

public class GameConfigImpl extends F4MConfig {
	
	public static final long AEROSPIKE_MULTIPLAYER_RECORD_CAPACITY_DEFAULT = 50L;
	
	public static final String AEROSPIKE_MULTIPLAYER_RECORD_CAPACITY = "aerospike.set.multiplayer.record.capacity";
	public static final String AEROSPIKE_MULTIPLAYER_SET = "aerospike.set.multiplayer";
	public static final String AEROSPIKE_MULTIPLAYER_INDEX_SET = "aerospike.set.multiplayer.index";
	public static final String AEROSPIKE_MULTIPLAYER_KEY_PREFIX = "aerospike.multiplayer.key.prefix";

	public static final String AEROSPIKE_GAME_HISTORY_SET = "aerospike.set.gameHistory";
	public static final String AEROSPIKE_DASHBOARD_SET = "aerospike.set.dashboard";
	
	public static final String AEROSPIKE_RESULT_SET = "result.aerospike.set";
	
	public static final String MINUTES_TO_ACCEPT_INVITE_DEFAULT = "time.to.accept.invite.default";
	
	public static final String ELASTIC_INDEX_PUBLIC_GAME = "elastic.index.public.game";
	public static final String ELASTIC_TYPE_PUBLIC_GAME = "elastic.type.public.game";
	public static final String ELASTIC_MAPPING_INDEX_PUBLIC_GAME = "elastic.mapping.index.public.game";
	public static final String ELASTIC_MAPPING_TYPE_PUBLIC_GAME = "elastic.mapping.type.public.game";
	
	public static final String EXPIRED_PUBLIC_GAME_LIST_SIZE = "elastic.expired.public.game.list.size";

	public static final String GAME_CLEAN_UP_UTC_TIME = "cleanUp.game.utcTime";
	public static final String GAME_CLEAN_UP_UTC_TIME_DEFAULT = "16:00";
	
	public static final String AEROSPIKE_USER_GAME_ACCESS_SET = "aerospike.set.userGameAccess";
	public static final String AEROSPIKE_USER_GAME_ACCESS_NAMESPACE = "userGameAccess";
	
	
	
	/**
	 * Game max play time. 
	 * Used as max period for opened game, so after that time game is closed up.
	 * Also used as default normal tournament end date/
	 */
	public static final String GAME_MAX_PLAY_TIME_IN_HOURS = "cleanUp.game.maxActiveTimeInHours";
	public static final int GAME_MAX_PLAY_TIME_IN_HOURS_DEFAULT = 24;

	public GameConfigImpl() {
		setProperty(AEROSPIKE_MULTIPLAYER_RECORD_CAPACITY, AEROSPIKE_MULTIPLAYER_RECORD_CAPACITY_DEFAULT);
		setProperty(AEROSPIKE_MULTIPLAYER_SET, "mgi");
		setProperty(AEROSPIKE_MULTIPLAYER_INDEX_SET, "mgiIndex");
		setProperty(AEROSPIKE_MULTIPLAYER_KEY_PREFIX, "mgi");
		
		setProperty(AEROSPIKE_GAME_HISTORY_SET, "gameHistory");
		setProperty(AEROSPIKE_DASHBOARD_SET, "dashboard");
		
		setProperty(AEROSPIKE_RESULT_SET, "result");

		setProperty(MINUTES_TO_ACCEPT_INVITE_DEFAULT, 500); // 500 minutes
		
		setProperty(ELASTIC_INDEX_PUBLIC_GAME, "public_game");
		setProperty(ELASTIC_TYPE_PUBLIC_GAME, "public_game");
		setProperty(ELASTIC_MAPPING_INDEX_PUBLIC_GAME, Resources.getResource(PublicGameElasticDao.class, "PublicGameIndexESMapping.json"));
		setProperty(ELASTIC_MAPPING_TYPE_PUBLIC_GAME, Resources.getResource(PublicGameElasticDao.class, "PublicGameESMapping.json"));
		
		setProperty(EXPIRED_PUBLIC_GAME_LIST_SIZE, 100);
		
		setProperty(GAME_CLEAN_UP_UTC_TIME, GAME_CLEAN_UP_UTC_TIME_DEFAULT);
		setProperty(GAME_MAX_PLAY_TIME_IN_HOURS, GAME_MAX_PLAY_TIME_IN_HOURS_DEFAULT);
		
		setProperty(AEROSPIKE_USER_GAME_ACCESS_SET, "userGameAccess");
		
	}

}
