package de.ascendro.f4m.server.multiplayer.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CreatedBy;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.InvitedUser;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.json.model.user.ClientInfo;

public interface CommonMultiplayerGameInstanceDao {

	String create(String userId, CustomGameConfig customGameConfig);

    /**
     *
     * @param userId
     * @param customGameConfig
     * @param mgiId - multiplayer game instance id
     */
	void createDuel(String userId, CustomGameConfig customGameConfig, String mgiId);

    /**
     *
     * @param mgiId - multiplayer game instance id
     */
	void deleteDuel(String mgiId);

    /**
     *
     * @param userId
     * @param customGameConfig
     * @param mgiId - multiplayer game instance id
     * @param decrement - decrement entryCount
     */
	void updateDuel(String userId, CustomGameConfig customGameConfig, String mgiId, long decrement);

	void addUser(String mgiId, String userId, String inviterId, MultiplayerGameInstanceState state);
	
	CustomGameConfig getConfig(String mgiId);

	CustomGameConfig getConfigForFinish(String mgiId);

	/**
	 * Change users' state from PENDING to INVITED
	 * 
	 * @return list of invited users
	 */
	List<String> activateInvitations(String mgiId);

	/**
	 * Change state from INVITED to REGISTER.
	 */
	void registerForGame(String mgiId, ClientInfo clientInfo, String gameInstanceId);
	
	/**
	 * Change state from REGISTER to STARTED.
	 * @param mgiId - multiplayer game instance id
	 * @param gameInstanceId - user game instance id
	 * @param userId - player id
	 * @param userHandicap - user handicap at game start
	 */
	void joinGame(String mgiId, String gameInstanceId, String userId, Double userHandicap);

	/**
	 * Mark game instance as results as calculated
	 * @param mgiId - multiplayer game instance id
	 * @param gameInstanceId - Game instance id
	 */
	void markGameInstanceAsCalculated(String mgiId, String gameInstanceId, String userId);
	
	/**
	 * Checks if all game instances' results are calculated
	 * @param mgiId - multiplayer game instance id
	 * @return true is all game instances' results are calculated
	 */
	boolean hasNoRemainingGameInstances(String mgiId);
	
	/**
	 * Change game instance state to CANCELLED.
	 * @param mgiId - multiplayer game instance id
	 * @param gameInstanceId - 
	 * @return initial state of multiplayer game instance
	 */
	MultiplayerGameInstanceState cancelGameInstance(String mgiId, String gameInstanceId, String userId);
	
	/**
	 * Add amount to not yet calculated counter
	 * @param mgiId - multiplayer game instance id
	 * @param amount - amount to be added, e.g. +1 or -1
	 */
	void addToNotYetCalculatedCounter(String mgiId, int amount);

	/**
	 * Add amount to game instances counter
	 * @param mgiId  - multiplayer game instance id
	 * @param amount - amount to be added, e.g. +1 or -1
	 */
	void addToGameInstancesCounter(String mgiId, int amount);
	
	/**
	 * Get total count of game instances within particular 
	 * @param mgiId - multiplayer game instance id
	 * @return total count of game instances within particular 
	 */
	int getGameInstancesCount(String mgiId);
	
	/**
	 * Count of created, but not yet calculated game instances.
	 * Canceled game instances are counted as calculated
	 * @param mgiId - multiplayer game instance id
	 * @retur nnot yet calculated game instance count
	 */
	int getNotYetCalculatedGameInstancesCount(String mgiId);
	
	boolean isMultiplayerInstanceAvailable(String mgiId);

	boolean hasValidParticipantCount(String mgiId);

	void declineInvitation(String userId, String mgiId);
	
	void rejectInvitation(String userId, String mgiId);

	String getInviterId(String userId, String mgiId, MultiplayerGameInstanceState status);
	
	String getInviterId(String userId, String mgiId);

	void deleteNotAcceptedInvitations(String mgiId);
	
	void markAsExpired(String mgiId);

	/** Get map (userId -> state) of all invited users to multiplayer game instance */
	Map<String, String> getAllUsersOfMgi(String mgiId);
	
	/**
	 * Get invitation list for specified user within particular tenant id, filtered by {@link CreatedBy} and {@link FilterCriteria}
	 * @param tenantId
	 * @param appId TODO
	 * @param userId
	 * @param states
	 * @param createdBy
	 * @param filterCriteria
	 * @return list of {@link Invitation}s
	 */
	List<Invitation> getInvitationList(String tenantId, String appId, String userId,
			List<MultiplayerGameInstanceState> states, CreatedBy createdBy, FilterCriteria filterCriteria);
	
	/**
	 * Get list of games user is invited for within particular tenant
	 * @param tenantId - Tenant id
	 * @param appId TODO
	 * @param userId - User id
	 * @return list of game ids
	 */
	Set<String> getGamesInvitedTo(String tenantId, String appId, String userId);

	List<InvitedUser> getInvitedList(String mgiId, int limit, long offset, List<OrderBy> orderBy,
			Map<String, String> searchBy);
	
	/**
	 * Select MultiplayerUserGameInstance for all states with MultiplayerUserGameInstance
	 * @param mgiId - multiplayer game instance id
	 * @return list of all MultiplayerUserGameInstance
	 */
	List<MultiplayerUserGameInstance> getGameInstances(String mgiId);

	/**
	 * Select MultiplayerUserGameInstance by state
	 * @param mgiId - multiplayer game instance id
	 * @param states - states to be selected
	 * @throws IllegalArgumentException - if invalid stated used (INVIATED, DELETED, DECLINED)
	 * @return list of all MultiplayerUserGameInstance filtered by states
	 */
	List<MultiplayerUserGameInstance> getGameInstances(String mgiId, MultiplayerGameInstanceState... states) throws IllegalArgumentException;
	
	/**
	 * Select user's game instance ID for particular multiplayer game instance
	 * @param mgiId
	 * @param userId
	 * @return game instance ID
	 */
	String getGameInstanceId(String mgiId, String userId);

	/**
	 * Determine state of user's multiplayer game instance
	 * @param multiplayerGameInstanceId - multiplayer game instance id
	 * @param userId - user id
	 * @return state of user's multiplayer game instance
	 */
	MultiplayerGameInstanceState getUserState(String multiplayerGameInstanceId, String userId);

	/** Map live tournament game ID with multiplayer game instance ID */
	void mapTournament(String gameId, String mgiId);

	void moveTournamentToFinished(String gameId, String mgiId);
	
	String getTournamentInstanceId(String gameId);

	/**
	 * Set multiplayer game instance end date to now
	 * NOTE: meant to be used only for games with repetitions via events, e.g. live and normal tournaments
	 * @param mgiId
	 */
	void markTournamentAsEnded(String mgiId);

	List<String> getExtendedMgiDuel();

    List<String> getExtendedMgiTournament();

    void markGameAsExpired(String mgiId);

	boolean hasAnyCalculated(String mgiId);

	void setInvitationExpirationNotificationId(String mgiId, String userId, String notificationId);

	void resetInvitationExpirationNotificationId(String mgiId, String userId);
	
	String getInvitationExpirationNotificationId(String mgiId, String userId);

    /**
     *
     * @param mgiId - multiplayer game instance id
     * @return the number of invited players
     */
	long getTotalRecordCountDuel(String mgiId);
}
