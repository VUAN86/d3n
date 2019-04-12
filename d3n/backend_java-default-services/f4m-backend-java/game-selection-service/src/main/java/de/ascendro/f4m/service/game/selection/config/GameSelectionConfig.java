package de.ascendro.f4m.service.game.selection.config;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;

/**
 * Game Selection Service Configuration store
 * 
 */
public class GameSelectionConfig extends F4MConfigImpl {
	
	public static final String GAME_NOTIFICATION_THREAD_POOL_SIZE = "game.notification.thread.pool.size";
	public static final String GAME_COUNT_DOWN_NOTIFICATION_COUNT = "game.count.down.notification.count";
	public static final String GAME_COUNT_DOWN_NOTIFICATION_FREQUENCY = "game.count.down.notification.frequency";
	public static final String GAME_START_NOTIFICATION_ADVANCE = "game.start.notification.advance";
	public static final String GAME_PLAYER_READINESS_DEFAULT = "game.player.readiness.default";
	public static final String JACKPOT_PAYOUT_PERCENT = "result.jackpot.payout.percent";

	public static final String DUEL_INVITEE_MAX_COUNT = "duel.invitee.max.count";

	public GameSelectionConfig() {
		super(new AerospikeConfigImpl(), new ElasticConfigImpl(), new GameConfigImpl());
		setProperty(JACKPOT_PAYOUT_PERCENT, 0.90);
		setProperty(USERS_CACHE_TIME_TO_LIVE, 60 * 60 * 1000); //cache client ids received within session for 1h

		setProperty(GAME_NOTIFICATION_THREAD_POOL_SIZE, 2);

		setProperty(GAME_COUNT_DOWN_NOTIFICATION_COUNT, 3);
		//EVENT -> count-down..1m..count-down..1m..count-down..45s...start..15s..PLAY
		setProperty(GAME_COUNT_DOWN_NOTIFICATION_FREQUENCY, 60_000); // 1 minute
		setProperty(GAME_START_NOTIFICATION_ADVANCE, 15_000); // 15 seconds
		setProperty(GAME_PLAYER_READINESS_DEFAULT, 180_000); // 180 seconds

		setProperty(DUEL_INVITEE_MAX_COUNT, 4);
		
		loadProperties();
	}

}
