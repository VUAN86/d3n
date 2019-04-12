package de.ascendro.f4m.service.usermessage.onesignal;

import static de.ascendro.f4m.service.usermessage.translation.Messages.*;

import java.util.HashMap;
import java.util.Map;

import de.ascendro.f4m.service.profile.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushNotificationTypeMessageMapper {
	/**
	 * Maps a message to a profile attribute which specifies, if user has allowed specific type of push notification.
	 */
	private Map<String, String> messagesMap = null;
	private static final Logger LOGGER = LoggerFactory.getLogger(PushNotificationTypeMessageMapper.class);

	public boolean isMessageEnabled(String message, Profile profile) {
		init();
		String profileAttribute = messagesMap.get(message);
		Boolean isEnabled = Boolean.TRUE;
		LOGGER.debug("isMessageEnabled {} ", profile.getPropertyAsBoolean(profileAttribute));
		if (profileAttribute != null) {
			isEnabled = profile.getPropertyAsBoolean(profileAttribute);
		}
		return isEnabled == null || isEnabled;
	}

	private synchronized void init() {
		if (messagesMap == null) {
			messagesMap = new HashMap<>();
			putProfilePropertyMessages(Profile.PUSH_NOTIFICATIONS_FRIEND_ACTIVITY_PROPERTY, //
					GAME_INVITATION_RESPONSE_ACCEPTED_PUSH, GAME_INVITATION_RESPONSE_REJECTED_PUSH, GAME_INVITATION_PUSH);

			putProfilePropertyMessages(Profile.PUSH_NOTIFICATIONS_TOURNAMENT_PROPERTY, //
					GAME_TOURNAMENT_ONEHOUREXPIRE_INVITATION_PUSH, GAME_PLAYER_READINESS_PUSH,
					GAME_TOURNAMENT_FIVE_MINUTES_PUSH, GAME_TOURNAMENT_ONE_HOUR_PUSH, GAME_TOURNAMENT_WON_PUSH,
					GAME_TOURNAMENT_LOST_PUSH, GAME_STARTING_SOON_PUSH, GAME_ENDED_PUSH);

			putProfilePropertyMessages(Profile.PUSH_NOTIFICATIONS_DUEL_PROPERTY, //
					GAME_DUEL_ONEHOUREXPIRE_INVITATION_PUSH, GAME_DUEL_WON_PUSH, GAME_DUEL_LOST_PUSH,
					BUDDY_GAME_DUEL_WON_PUSH, BUDDY_GAME_DUEL_LOST_PUSH);

			putProfilePropertyMessages(Profile.PUSH_NOTIFICATIONS_TOMBOLA_PROPERTY, //
					TOMBOLA_DRAW_WIN_PUSH, TOMBOLA_DRAW_LOSE_PUSH, TOMBOLA_DRAW_ANNOUNCEMENT_PUSH,
					TOMBOLA_OPEN_ANNOUNCEMENT_PUSH);

			putProfilePropertyMessages(Profile.PUSH_NOTIFICATIONS_NEWS_PROPERTY); //no 'predefined' messages for news
		}
	}

	private void putProfilePropertyMessages(String profileProperty, String... messages) {
		for (String message : messages) {
			messagesMap.put(message, profileProperty);
		}
	}
}
