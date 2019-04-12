package de.ascendro.f4m.service.game.usergameaccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.aerospike.client.Operation;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.selection.model.game.Game;

public class UserGameAccessDaoImpl extends AerospikeOperateDaoImpl<GameEnginePrimaryKeyUtil> implements UserGameAccessDao {
	
    public static final String USER_GAME_ACCESS_BIN_NAME = "usrGameAccess";

	@Inject
	public UserGameAccessDaoImpl(Config config, GameEnginePrimaryKeyUtil primaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}
	
	@Override
	public void create(Game game, String userId) {
		final String gameCountKey = primaryKeyUtil.createUserGameAccessId(game.getGameId(), userId);
		Operation readPlays = Operation.get(USER_GAME_ACCESS_BIN_NAME);
		operate(getSet(), gameCountKey, new Operation[] { readPlays }, (readResult, wp) -> Arrays
				.asList(Operation.add(getLongBin(USER_GAME_ACCESS_BIN_NAME, game.getEntryFeeBatchSize().longValue()))));
	}
	
	@Override
	public Long getUserGameCountLeft(Game game, String userId) {
		final String gameInstanceKey = primaryKeyUtil.createUserGameAccessId(game.getGameId(), userId);
		return readLong(getSet(),gameInstanceKey,USER_GAME_ACCESS_BIN_NAME );
	}	

	@Override
	public void decrementGamePlayedCount(Game game, String userId) {
		final String gameCountKey = primaryKeyUtil.createUserGameAccessId(game.getGameId(), userId);
		Operation readPlays = Operation.get(USER_GAME_ACCESS_BIN_NAME);
		operate(getSet(), gameCountKey, new Operation[] { readPlays }, (readResult, wp) -> {
			List<Operation> result = new ArrayList<>(1);
			Long gameCount = readLong(getSet(), gameCountKey, USER_GAME_ACCESS_BIN_NAME);
			if (gameCount == null)  {
				//do nothing
			} else  if (readResult != null && gameCount > 0) {
				result.add(Operation.add(getLongBin(USER_GAME_ACCESS_BIN_NAME, -1L)));
			} else {
				throw new F4MFatalErrorException(
						String.format("Failed to decrement user game  access plays left for game %s, user %s, game count is %s ",
								game.getGameId(), userId, gameCount));
			}
			return result;
		});
	}
	
	private String getSet(){
		return config.getProperty(GameConfigImpl.AEROSPIKE_USER_GAME_ACCESS_SET);
	}
}
