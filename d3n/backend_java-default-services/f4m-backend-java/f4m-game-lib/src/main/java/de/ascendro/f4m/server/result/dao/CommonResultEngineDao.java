package de.ascendro.f4m.server.result.dao;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.result.MultiplayerResults;
import de.ascendro.f4m.server.result.Results;

public interface CommonResultEngineDao extends AerospikeDao {

	/**
	 * Get stored results list given the game instance ID.
	 */
	Results getResults(String gameInstanceId);

	/**
	 * Determine if there are results.
	 */
	public boolean exists(String gameInstanceId);

	/**
	 * Get stored multiplayer game results.
	 */
	MultiplayerResults getMultiplayerResults(String multiplayerGameInstanceId);
	
	/**
	 * Determine if there are multiplayer game results.
	 */
	public boolean hasMultiplayerResults(String multiplayerGameInstanceId);

}
