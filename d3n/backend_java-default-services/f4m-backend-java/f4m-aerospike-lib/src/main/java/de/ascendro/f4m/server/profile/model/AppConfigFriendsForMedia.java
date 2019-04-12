package de.ascendro.f4m.server.profile.model;

/**
 * Partial model of AppConfiguration.friendsForMedia object returned by getAppConfiguration (not all attributes)
 */
public class AppConfigFriendsForMedia {
	private String name;
	private String url;
	private String email;
	private String logoUrl;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AppConfigFriendsForMedia [name=");
		builder.append(name);
		builder.append(", url=");
		builder.append(url);
		builder.append(", email=");
		builder.append(email);
		builder.append(", logoUrl=");
		builder.append(logoUrl);
		builder.append("]");
		return builder.toString();
	}

}