package de.ascendro.f4m.server.multiplayer.dao;

import java.util.List;

import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.PublicGameFilter;

public interface PublicGameElasticDao {

	void createOrUpdate(List<String> appIds, Invitation publicGame);

	Invitation getPublicGame(String appId, String mgiId);

	long getNumberOfPublicGames(PublicGameFilter publicGameFilter);

	Invitation getNextPublicGame(PublicGameFilter publicGameFilter);

	List<Invitation> searchPublicGames(PublicGameFilter filter, int limit);

	void delete(String mgiId, boolean wait, boolean silent);

	void changeUserOfPublicGame(String sourceUserId, String targetUserId);

	void removeLiveTournamentsWithExpiredPlayDateTime();

}
