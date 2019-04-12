package de.ascendro.f4m.server.profile.model;

/**
 * Model of AppConfiguration object returned by getAppConfiguration
 */
public class AppConfig {
	private AppConfigTenant tenant;
	private AppConfigApplication application;
	private AppConfigFriendsForMedia friendsForMedia;

	public AppConfigTenant getTenant() {
		return tenant;
	}

	public void setTenant(AppConfigTenant tenant) {
		this.tenant = tenant;
	}

	public AppConfigApplication getApplication() {
		return application;
	}

	public void setApplication(AppConfigApplication application) {
		this.application = application;
	}

	public AppConfigFriendsForMedia getFriendsForMedia() {
		return friendsForMedia;
	}

	public void setFriendsForMedia(AppConfigFriendsForMedia friendsForMedia) {
		this.friendsForMedia = friendsForMedia;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AppConfig [tenant=");
		builder.append(tenant);
		builder.append(", application=");
		builder.append(application);
		builder.append(", friendsForMedia=");
		builder.append(friendsForMedia);
		builder.append("]");
		return builder.toString();
	}

}
