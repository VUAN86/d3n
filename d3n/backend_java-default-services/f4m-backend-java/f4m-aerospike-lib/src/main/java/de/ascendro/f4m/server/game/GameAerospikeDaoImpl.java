package de.ascendro.f4m.server.game;

import javax.inject.Inject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.game.util.GamePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.game.selection.model.game.Game;

public class GameAerospikeDaoImpl extends AerospikeDaoImpl<GamePrimaryKeyUtil> implements GameAerospikeDao {

	private static final String MGI_ID_BIN_NAME = "mgiId";

	@Inject
	public GameAerospikeDaoImpl(Config config, GamePrimaryKeyUtil gamePrimaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider,
			JsonUtil jsonUtil) {
		super(config, gamePrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public Game getGame(String gameId) throws F4MEntryNotFoundException {
		final String key = primaryKeyUtil.createPrimaryKey(gameId);
		final String gameAsString = readJson(getSet(), key, BLOB_BIN_NAME);
		final Game game = jsonUtil.fromJson(gameAsString, Game.class);
		if (game != null) {
			return game;
		} else {
			throw new F4MEntryNotFoundException("Game not found by id " + gameId);
		}
	}

	public void createARecordMgiId(String gameId,String mgiId) {
		final String key = primaryKeyUtil.createPrimaryKey(gameId);
		createOrUpdateString(getSet(), key, MGI_ID_BIN_NAME, mgiId);
	}

	public String getMgiId(String gameId) throws F4MEntryNotFoundException {
		final String key = primaryKeyUtil.createPrimaryKey(gameId);
		return readString(getSet(), key, MGI_ID_BIN_NAME);
	}

	protected String getSet() {
		return config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_SET);
	}

}
