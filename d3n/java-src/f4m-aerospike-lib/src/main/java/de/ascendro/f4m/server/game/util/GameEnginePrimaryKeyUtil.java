package de.ascendro.f4m.server.game.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;

public class GameEnginePrimaryKeyUtil extends PrimaryKeyUtil<String> {
	@Inject
	public GameEnginePrimaryKeyUtil(Config config) {
		super(config);
	}

	@Override
	protected String getServiceName() {
		return GameEngineMessageTypes.SERVICE_NAME;
	}
	
	public String createUserGameAccessId(String gameId, String userId) {
		return getServiceName()+ KEY_ITEM_SEPARATOR + gameId + KEY_ITEM_SEPARATOR + userId;
	}


}
