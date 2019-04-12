package de.ascendro.f4m.service.game.selection.client;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

import de.ascendro.f4m.service.game.selection.client.communicator.UserMessageServiceCommunicator;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.notification.GamePlayerReadinessNotification;
import de.ascendro.f4m.service.game.selection.model.notification.GameStartingSoonNotification;
import de.ascendro.f4m.service.game.selection.model.notification.InvitationExpirationNotification;
import de.ascendro.f4m.service.game.selection.model.notification.InvitationNotification;
import de.ascendro.f4m.service.game.selection.model.notification.InvitationRespondNotification;
import de.ascendro.f4m.service.game.selection.model.notification.LiveTournamentStartingSoonNotification;
import de.ascendro.f4m.service.game.selection.request.UserMessageRequestInfo;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManager;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.usermessage.translation.Messages;

public class NotificationMessagePreparer {

	private final UserMessageServiceCommunicator userMessageServiceCommunicator;
	private final MultiplayerGameInstanceManager mgiManager;

	@Inject
	public NotificationMessagePreparer(UserMessageServiceCommunicator userMessageServiceCommunicator,
			MultiplayerGameInstanceManager mgiManager) {
		this.userMessageServiceCommunicator = userMessageServiceCommunicator;
		this.mgiManager = mgiManager;
	}

	/**
	 * Send invitation notification to specified user id
	 * @param userId - recipient id
	 * @param mgiId - multiplayer game instance id
	 * @param inviteeClientInfo 
	 */
	public void sendInviteNotification(String inviterId, String inviteeId, String mgiId, ClientInfo inviteeClientInfo) {
		final InvitationNotification invitationNotification = new InvitationNotification(mgiId, inviterId);
		userMessageServiceCommunicator.pushMessageToUser(inviteeId, invitationNotification,
				inviteeClientInfo, Messages.GAME_INVITATION_PUSH);
	}

	/**
	 * Schedules invitation expiration notification 1h before duel start to specified user
	 * @param inviterId invitation owner
	 * @param inviteeId user who received the invitation
	 * @param mgiId multi-player game instance id
	 * @param inviteeClientInfo 
	 */
	public void sendInvitationExpirationNotification(String inviteeId, String inviterId, String mgiId, ClientInfo inviteeClientInfo) {
		CustomGameConfig multiplayerGameConfig = mgiManager.getMultiplayerGameConfig(mgiId);
		String appId = multiplayerGameConfig.getAppId();
		ZonedDateTime playDateTime = multiplayerGameConfig.getExpiryDateTime();
		ZonedDateTime scheduledNotificationDateTime = playDateTime.minus(1, ChronoUnit.HOURS);
		long expirationTimeMillis = playDateTime.toInstant().toEpochMilli();
		final InvitationExpirationNotification notification = new InvitationExpirationNotification(mgiId,
				inviterId, expirationTimeMillis);
		UserMessageRequestInfo requestInfo = new UserMessageRequestInfo(inviteeId, mgiId);
		
		String messageTemplate = null;
		if (multiplayerGameConfig.getGameType().isTournament()) {
			messageTemplate = Messages.GAME_TOURNAMENT_ONEHOUREXPIRE_INVITATION_PUSH;
		} else if (multiplayerGameConfig.getGameType().isDuel()) {
			messageTemplate = Messages.GAME_DUEL_ONEHOUREXPIRE_INVITATION_PUSH;
		}
		
		if (messageTemplate != null) {
			userMessageServiceCommunicator.pushNotificationToUser(inviteeId, appId,
					scheduledNotificationDateTime, notification,
					requestInfo, messageTemplate, multiplayerGameConfig.getGameTitle());
		}
	}
	
	/**
	 * Cancels invitation expiration notification
	 * @param notificationId the id of the notification to be cancelled
	 * @param inviteeId user who received the invitation
	 * @param mgiId multi-player game instance id
	 * @param clientInfo 
	 */
	public void cancelInvitationExpirationNotification(String notificationId, String inviteeId, String mgiId, ClientInfo clientInfo) {
		UserMessageRequestInfo requestInfo = new UserMessageRequestInfo(inviteeId, mgiId);
		userMessageServiceCommunicator.cancelNotification(notificationId, requestInfo, clientInfo);
	}

	public void sendLiveTournamentScheduledNotifications(String userId, String gameInstanceId, String mgiId,
			String gameId) {
		CustomGameConfig multiplayerGameConfig = mgiManager.getMultiplayerGameConfig(mgiId);
		String appId = multiplayerGameConfig.getAppId();
		ZonedDateTime playDateTime = multiplayerGameConfig.getStartDateTime();
		LiveTournamentStartingSoonNotification notification = new LiveTournamentStartingSoonNotification(gameInstanceId,
				mgiId, gameId);

		UserMessageRequestInfo requestInfo = new UserMessageRequestInfo(userId, mgiId);
		
		userMessageServiceCommunicator.pushNotificationToUser(userId, appId, playDateTime.minus(60, ChronoUnit.MINUTES),
				notification, requestInfo, Messages.GAME_TOURNAMENT_ONE_HOUR_PUSH,
				multiplayerGameConfig.getGameTitle());
		// push the 5 minutes before notification
		userMessageServiceCommunicator.pushNotificationToUser(userId, appId, playDateTime.minus(5, ChronoUnit.MINUTES),
				notification, requestInfo, Messages.GAME_TOURNAMENT_FIVE_MINUTES_PUSH,
				multiplayerGameConfig.getGameTitle());
	}

	/**
	 * Send invitation approve or decline notification to specified user
	 * @param inviteeId - user who approved/declined invitation
	 * @param accept - accepted(true)/declined(false)
	 * @param inviterId - recipient id/invitation owner
	 * @param mgiId - multiplayer game instance id
	 * @param clientInfo 
	 */
	public void sendInvitationResponseNotification(String inviteeId, boolean accept, String inviterId, String mgiId, ClientInfo clientInfo) {
		final InvitationRespondNotification invitationRespondNotification = new InvitationRespondNotification(mgiId,
				inviteeId, accept, inviterId);
		userMessageServiceCommunicator.pushMessageToUser(inviterId, invitationRespondNotification,
				clientInfo, accept ? Messages.GAME_INVITATION_RESPONSE_ACCEPTED_PUSH : Messages.GAME_INVITATION_RESPONSE_REJECTED_PUSH);
	}

	/**
	 * Send notification that game (live or normal tournament) is starting soon
	 * @param userId
	 * @param gameInstanceId
	 * @param mgiId
	 * @param gameId
	 * @param clientInfo 
	 */
	public void sendGameStartingSoonNotification(String userId, String gameInstanceId, String mgiId, String gameId, ClientInfo clientInfo) {
		GameStartingSoonNotification notification = new GameStartingSoonNotification(gameInstanceId, mgiId, gameId);
		CustomGameConfig multiplayerGameConfig = mgiManager.getMultiplayerGameConfig(mgiId);
		notification.setMillisToPlayDateTime(mgiManager.getMillisToPlayDateTime(mgiId));
		userMessageServiceCommunicator.pushMessageToUser(userId, notification, clientInfo, Messages.GAME_STARTING_SOON_PUSH,
				multiplayerGameConfig.getGameTitle());
	}
	
	/**
	 * Send player readiness notification
	 * @param userId
	 * @param gameInstanceId
	 * @param mgiId
	 * @param gameId
	 * @param clientInfo 
	 */
	public void sendGamePlayerReadinessNotification(String userId, String gameInstanceId, String mgiId, String gameId, ClientInfo clientInfo) {
		GamePlayerReadinessNotification notification = new GamePlayerReadinessNotification(gameInstanceId, mgiId, gameId);
		notification.setMillisToPlayDateTime(mgiManager.getMillisToPlayDateTime(mgiId));
		CustomGameConfig multiplayerGameConfig = mgiManager.getMultiplayerGameConfig(mgiId);
		// minutes to play time is the readiness /60, since readiness is in seconds :
		int secondsToPlayTime = multiplayerGameConfig.getPlayerGameReadiness();
		userMessageServiceCommunicator.pushMessageToUser(userId, notification, clientInfo, Messages.GAME_PLAYER_READINESS_PUSH,
				String.valueOf(secondsToPlayTime), multiplayerGameConfig.getGameTitle());
	}

}
