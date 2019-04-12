package de.ascendro.f4m.service.profile.model.api;

import de.ascendro.f4m.service.profile.model.Profile;

public class ApiProfileSettingsPush {

	private boolean pushNotificationsFriendActivity;
	private boolean pushNotificationsTournament;
	private boolean pushNotificationsDuel;
	private boolean pushNotificationsTombola;
	private boolean pushNotificationsNews;

	public ApiProfileSettingsPush() {
		// Initialize empty object
	}
	
	public ApiProfileSettingsPush(Profile profile) {
		pushNotificationsFriendActivity = profile.isPushNotificationsFriendActivity();
		pushNotificationsTournament = profile.isPushNotificationsTournament();
		pushNotificationsDuel = profile.isPushNotificationsDuel();
		pushNotificationsTombola = profile.isPushNotificationsTombola();
		pushNotificationsNews = profile.isPushNotificationsNews();
	}

	public void fillProfile(Profile profile) {
		profile.setPushNotificationsDuel(pushNotificationsDuel);
		profile.setPushNotificationsFriendActivity(pushNotificationsFriendActivity);
		profile.setPushNotificationsTombola(pushNotificationsTombola);
		profile.setPushNotificationsTournament(pushNotificationsTournament);
		profile.setPushNotificationsNews(pushNotificationsNews);
	}

	public boolean isPushNotificationsFriendActivity() {
		return pushNotificationsFriendActivity;
	}

	public boolean isPushNotificationsTournament() {
		return pushNotificationsTournament;
	}

	public boolean isPushNotificationsDuel() {
		return pushNotificationsDuel;
	}

	public boolean isPushNotificationsTombola() {
		return pushNotificationsTombola;
	}

	public boolean isPushNotificationsNews() {
		return pushNotificationsNews;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ApiProfileSettingsPush [");
		builder.append("pushNotificationsFriendActivity=").append(pushNotificationsFriendActivity);
		builder.append(", pushNotificationsTournament=").append(pushNotificationsTournament);
		builder.append(", pushNotificationsDuel=").append(pushNotificationsDuel);
		builder.append(", pushNotificationsTombola=").append(pushNotificationsTombola);
		builder.append(", pushNotificationsNews=").append(pushNotificationsNews);
		builder.append("]");
		return builder.toString();
	}
}
