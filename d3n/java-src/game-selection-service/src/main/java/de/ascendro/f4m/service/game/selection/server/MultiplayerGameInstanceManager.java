package de.ascendro.f4m.service.game.selection.server;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CreatedBy;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.InvitedUser;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.game.selection.model.multiplayer.PublicGameFilter;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.api.ApiProfileExtendedBasicInfo;
import de.ascendro.f4m.service.session.SessionWrapper;

public interface MultiplayerGameInstanceManager {

	CustomGameConfig createMultiplayerGameInstance(
			MultiplayerGameParameters multiplayerGameParameters,
			ClientInfo clientInfo,
			JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper sourceSession,
			boolean rematch, boolean isPublic);

	void setMultiplayerGameInstanceAsExpired(String mgiId);

	void addPublicGameToElastic(String mgiId);

	String createPublicGame(MultiplayerGameParameters params, ClientInfo clientInfo,
			JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper sourceSession);

	CustomGameConfig getMultiplayerGameConfig(String instanceId);

	List<String> addUsers(List<String> inviteesIds, String instanceId, String inviterId);

	boolean addUser(String inviteeId, String instanceId, String inviterId);
	
	void activateInvitationsAndSendInviteNotifications(String mgiId, ClientInfo clientInfo);
	
	void inviteUserIfNotExist(String userId, String instanceId);

	String getInviterId(String userId, String mgiId);

	void declineInvitation(String userId, String mgiId, ClientInfo clientInfo);

	void deleteDuelPrivate(String mgiId);

	void rejectInvitation(String userId, String mgiId, ClientInfo clientInfo);

	void acceptedInvitation(String userId, String mgiId, ClientInfo clientInfo);

	List<Invitation> getInvitationList(String tenantId, String appId, String userId, List<String> states,
			CreatedBy createdBy, FilterCriteria filterCriteria);

	List<Invitation> filterInvitationsByResults(List<Invitation> invitations, boolean isPending);

	void addInvitationOpponents(List<Invitation> invitations, String inviterId);

	void addGameInfo(List<Invitation> invitations);

	void addGameInstanceInfo(List<Invitation> invitations, String userId);

	void addInvitationUserInfo(List<Invitation> invitations, Function<Invitation, String> getUserId,
			BiConsumer<Invitation, ApiProfileExtendedBasicInfo> setUserInfo);

	List<InvitedUser> getInvitedList(String mgiId, int limit, long offset, List<OrderBy> orderBy,
			Map<String, String> searchBy);

	Set<String> getGamesUserHasBeenInvitedTo(String tenantId, String appId, String userId);

	boolean validateParticipantCount(String mgiId, String userId);

	boolean validateConfirmInvitation(String mgiId, String userId);

	boolean validateInvitation(MultiplayerGameParameters multiplayerGameParameters, String mgiId, List<?> invitees);

	GameType getGameType(MultiplayerGameParameters multiplayerGameParameters, String mgiId);

	boolean validateUserState(String userId, String mgiId);

	void validateUserPermissionsForEntryFee(String messageName, ClientInfo clientInfo, String mgiId);

	void validateUserPermissionsForEntryFee(String messageName, ClientInfo clientInfo, EntryFee entryFee);

	Invitation getInvitation(String tenantId, String appId, String userId, GameType gameType);

	void updateLastInvitedAndPausedDuelOpponents(String mgiId, String inviterId, List<String> invitedUsers, List<String> pausedUsers);

	void mapTournament(String gameId, String mgiId);

	String getTournamentInstanceId(String gameId);

	List<MultiplayerUserGameInstance> getMultiplayerUserGameInstances(String mgiId,
			MultiplayerGameInstanceState... states);

	List<Invitation> listPublicGames(PublicGameFilter filter, int limit);

	long getNumberOfPublicGames(PublicGameFilter publicGameFilter);

	Invitation getNextPublicGame(PublicGameFilter publicGameFilter);

	void deletePublicGameSilently(String mgiId);

	Invitation getPublicGame(String appId, String mgiId);

	void moveMultiplayerGameInstanceData(String tenantId, String sourceUserId, String targetUserId);

	void movePublicGameData(String sourceUserId, String targetUserId);

	long getMillisToPlayDateTime(String mgiId);

	ZonedDateTime getPlayDateTime(String mgiId);

	void setScheduledNotification(String mgiId, String userId, String notificationId);
	
	void cancelScheduledNotification(String mgiId, String userId, ClientInfo clientInfo);
	
	void resetScheduledNotification(String mgiId, String userId);

	List<String> ensureHasNoBlocking(String userId, List<String> usersIds, boolean throwException);

}
