package de.ascendro.f4m.service.winning.config;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class WinningConfig extends F4MConfigImpl {

	public static final String AEROSPIKE_WINNING_COMPONENT_SET = "winningComponent.aerospike.set";
	public static final String AEROSPIKE_WINNING_COMPONENT_KEY_PREFIX = "aerospike.winning.component.key.prefix";
	public static final String AEROSPIKE_USER_WINNING_COMPONENT_SET = "userWinningComponent.aerospike.set";
	public static final String AEROSPIKE_USER_WINNING_COMPONENTS_MAP_SET = "userWinningComponentMap.aerospike.set";
	public static final String AEROSPIKE_SUPER_PRIZE_SET = "superPrize.aerospike.set";


	public WinningConfig() {
		super(new AerospikeConfigImpl());
		setProperty(AEROSPIKE_WINNING_COMPONENT_SET, "winningComponent");
		setProperty(AEROSPIKE_WINNING_COMPONENT_KEY_PREFIX, "winningComponent");
		setProperty(AEROSPIKE_USER_WINNING_COMPONENT_SET, "userWinningComponent");
		setProperty(AEROSPIKE_USER_WINNING_COMPONENTS_MAP_SET, "userWinningComponentsMap");
		setProperty(AEROSPIKE_SUPER_PRIZE_SET, "superPrize");
		setProperty(ADMIN_EMAIL, ADMIN_EMAIL_DEFAULT);
		loadProperties();
	}
}
