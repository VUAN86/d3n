package de.ascendro.f4m.service.game.engine.client.results;

import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;

public interface ResultEngineCommunicator {

	/**
	 * Request to calculate individual player results
	 * @param clientInfo - player info
	 * @param gameInstance - game instance to calculate results for
	 * @param sourceSession 
	 */
	void requestCalculateResults(ClientInfo clientInfo, GameInstance gameInstance,
			SessionWrapper sourceSession);

	void requestCalculateMultiplayerResults(ClientInfo clientInfo, String mgiId, GameType gameType,
			SessionWrapper sourceSession);

}