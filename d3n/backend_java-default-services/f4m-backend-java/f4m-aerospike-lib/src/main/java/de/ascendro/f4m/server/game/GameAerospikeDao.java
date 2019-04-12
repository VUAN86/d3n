package de.ascendro.f4m.server.game;

import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.game.selection.model.game.Game;

public interface GameAerospikeDao {

	public static final String BLOB_BIN_NAME = "value";

	/**
	 * Retrieve game info by game ID
	 * 
	 * @param gameId
	 *            {@link String}
	 * @return game info as {@link Game}
	 * @throws F4MEntryNotFoundException - if game not found by id
	 */
	Game getGame(String gameId) throws F4MEntryNotFoundException;

	void createARecordMgiId(String gameId, String mgiId);

	String getMgiId(String gameId) throws F4MEntryNotFoundException;
}
