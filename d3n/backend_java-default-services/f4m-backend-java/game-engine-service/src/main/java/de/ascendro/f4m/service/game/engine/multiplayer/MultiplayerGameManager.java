package de.ascendro.f4m.service.game.engine.multiplayer;

import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;

import java.util.List;

public interface MultiplayerGameManager {

	void joinGame(String mgiId, String gameInstanceId, String userId, Double userHandicap);

	void registerForGame(String mgiId, ClientInfo clientInfo, String gameInstanceId, CustomGameConfig customGameConfig);

	void markUserResultsAsCalculated(String mgiId, String gameInstanceId, String userId);

	void requestCalculateMultiplayerResultsIfPossible(ClientInfo clientInfo, String mgiId, Game game,
			SessionWrapper sourceSession);

	void cancelGame(String userId, String mgiId, String gameInstanceId);
	
	CustomGameConfig getMultiplayerGameConfig(String mgiId);
	
	void markAsExpired(String mgiId);

	List<MultiplayerUserGameInstance> getMultiplayerGameInstances(String mgiId);

	void markTournamentAsEnded(String mgiId);

	void cleanUpPublicGameList();
	
	boolean hasEnoughPlayersToPlay(String mgiId);
	
	void cancelLiveTournament(String mgiId);
}