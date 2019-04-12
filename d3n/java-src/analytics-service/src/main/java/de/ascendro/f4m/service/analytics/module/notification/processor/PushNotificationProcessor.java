package de.ascendro.f4m.service.analytics.module.notification.processor;

import javax.inject.Inject;

import org.slf4j.Logger;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.TombolaEndAnnouncementEvent;
import de.ascendro.f4m.server.analytics.model.TombolaEndEvent;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.notification.processor.base.INotificationProcessor;
import de.ascendro.f4m.service.analytics.module.notification.util.NotificationUtil;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.analytics.util.AnalyticServiceUtil;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.usermessage.translation.Messages;

public class PushNotificationProcessor implements INotificationProcessor {
    @InjectLogger
    private static Logger LOGGER;
    protected final NotificationCommon notificationCommon;
    protected final NotificationUtil notificationUtil;
    protected final AnalyticServiceUtil analyticServiceUtil;

    @Inject
    public PushNotificationProcessor(NotificationCommon notificationCommon,
                                     NotificationUtil notificationUtil,
                                     AnalyticServiceUtil analyticServiceUtil) {
        this.notificationCommon = notificationCommon;
        this.notificationUtil = notificationUtil;
        this.analyticServiceUtil = analyticServiceUtil;
    }

    @Override
    public void handleEvent(EventContent content) {
        try {
            processEvent(content);
        } catch (Exception e) {
            LOGGER.error("Error on event handle", e);
        }
    }

    @Override
    public void processEvent(EventContent content) throws Exception {
        if (content.isOfType(MultiplayerGameEndEvent.class)) {
            sendMultiplayerGameEndNotification((MultiplayerGameEndEvent) content.getEventData(), content);
        } else if (content.isOfType(TombolaEndEvent.class)) {
            sendTombolaEndNotification((TombolaEndEvent) content.getEventData(), content);
        } else if (content.isOfType(TombolaEndAnnouncementEvent.class)) {
            sendTombolaEndAnnouncementNotification((TombolaEndAnnouncementEvent) content.getEventData(), content);
        }
    }

    private void sendMultiplayerGameEndNotification(MultiplayerGameEndEvent event, EventContent content) {
        GameInstance gameInstance = analyticServiceUtil.getGameInstance(event.getGameInstanceId());
        GameType gameType = gameInstance.getGame().getType();

        if (gameType.isDuel() && event.getOpponentId() != null) {
            sendDuelGameEndNotification(event, content);
        } else if (gameType.isTournament()) {
            sendTournamentGameEndNotification(event, content);
        }
    }

    private void sendTournamentGameEndNotification(MultiplayerGameEndEvent event, EventContent content) {
        ClientInfo clientInfo = buildClientInfo(content);
        if (event.getJackpot() != null && event.getJackpot().signum() > 0) {
            String[] params = new String[2];
            params[0] = event.getJackpot().setScale(2).toPlainString();
            if (event.hasCurrency(Currency.MONEY)) {
                params[1] = analyticServiceUtil.getCurrency(content);
            } else if (event.hasCurrency(Currency.BONUS)) {
                params[1] = Messages.CURRENCY_POINTS;
            } else if (event.hasCurrency(Currency.CREDIT)) {
                params[1] = Messages.CURRENCY_CREDITS;
            }
            notificationCommon.pushMessageToUser(content.getUserId(), Messages.GAME_TOURNAMENT_WON_PUSH,
                    params, clientInfo);
        } else {
            notificationCommon.pushMessageToUser(content.getUserId(), Messages.GAME_TOURNAMENT_LOST_PUSH,
                    null, clientInfo);
        }
    }

    private ClientInfo buildClientInfo(EventContent content) {
		ClientInfo clientInfo = new ClientInfo();
		clientInfo.setTenantId(content.getTenantId());
		clientInfo.setAppId(content.getAppId());
		clientInfo.setUserId(content.getUserId());
		clientInfo.setIp(content.getSessionIp());
		return clientInfo;
	}

	private void sendDuelGameEndNotification(MultiplayerGameEndEvent event, EventContent content) {
        Profile profile = analyticServiceUtil.getProfile(event.getOpponentId());
        boolean isBuddyGame = notificationUtil.isBuddyGame(event, content);
        ClientInfo clientInfo = buildClientInfo(content);

        if (isBuddyGame) {
            if (Boolean.TRUE.equals(event.isGameWon())) {
                notificationCommon.pushMessageToUser(content.getUserId(), Messages.BUDDY_GAME_DUEL_WON_PUSH, null, clientInfo);
            } else if (Boolean.TRUE.equals(event.isGameLost())) {
                notificationCommon.pushMessageToUser(content.getUserId(), Messages.BUDDY_GAME_DUEL_LOST_PUSH, null, clientInfo);
            }
        } else {
            if (Boolean.TRUE.equals(event.isGameWon())) {
                notificationCommon.pushMessageToUser(content.getUserId(), Messages.GAME_DUEL_WON_PUSH,
                        new String[]{profile.getFullNameOrNickname()}, clientInfo);
            } else if (Boolean.TRUE.equals(event.isGameLost())) {
                notificationCommon.pushMessageToUser(content.getUserId(), Messages.GAME_DUEL_LOST_PUSH,
                        new String[]{profile.getFullNameOrNickname()}, clientInfo);
            }

        }

    }

    private void sendTombolaEndNotification(TombolaEndEvent event, EventContent content) {
    	ClientInfo clientInfo = buildClientInfo(content);
        notificationUtil.getTombolaResults(event.getTombolaId()).forEach(tombolaResult -> {
            if (tombolaResult.isWinner()) {
                tombolaResult.getPrizeNames().forEach(prizeName ->
                        notificationCommon.pushMessageToUser(tombolaResult.getUserId(), Messages.TOMBOLA_DRAW_WIN_PUSH,
                                new String[]{prizeName}, clientInfo));
            } else {
                notificationCommon.pushMessageToUser(tombolaResult.getUserId(), Messages.TOMBOLA_DRAW_LOSE_PUSH, null, clientInfo);
            }
        });
    }

    private void sendTombolaEndAnnouncementNotification(TombolaEndAnnouncementEvent event, EventContent content) {
    	ClientInfo clientInfo = buildClientInfo(content);
        notificationUtil.getTombolaParticipants(event.getTombolaId()).stream().forEach(userId ->
                notificationCommon.pushMessageToUser(userId, Messages.TOMBOLA_DRAW_ANNOUNCEMENT_PUSH,
                        new String[]{String.valueOf(event.getMinutesToEnd())}, clientInfo)
        );
    }


}
