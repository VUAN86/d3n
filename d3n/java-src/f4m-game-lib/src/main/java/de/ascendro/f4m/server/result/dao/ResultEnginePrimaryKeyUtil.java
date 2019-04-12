package de.ascendro.f4m.server.result.dao;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

public class ResultEnginePrimaryKeyUtil extends PrimaryKeyUtil<String> {
	
	public static final String RESULTS_PREFIX = KEY_ITEM_SEPARATOR + "results" + KEY_ITEM_SEPARATOR;
	public static final String MULTIPLAYER_RESULTS_PREFIX = KEY_ITEM_SEPARATOR + "multiplayerResults" + KEY_ITEM_SEPARATOR;

    @Inject
    public ResultEnginePrimaryKeyUtil(Config config) {
        super(config);
    }
    
	/**
	 * Creates primary key for results
	 * @return primary key as [service_name]:results:[id]
	 */
	public String createResultsPrimaryKey(String gameInstanceId) {
		return getServiceName() + RESULTS_PREFIX + gameInstanceId;
	}

	/**
	 * Creates primary key for multiplayer results
	 * @return primary key as [service_name]:multiplayerResults:[multiplayerGameInstanceId]
	 */
	public String createMultiplayerResultsPrimaryKey(String multiplayerGameInstanceId) {
		return getServiceName() + MULTIPLAYER_RESULTS_PREFIX + multiplayerGameInstanceId;
	}

}
