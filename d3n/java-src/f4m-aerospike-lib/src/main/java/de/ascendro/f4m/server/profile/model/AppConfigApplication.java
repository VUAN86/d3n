package de.ascendro.f4m.server.profile.model;

/**
 * Partial model of AppConfiguration.application object returned by getAppConfiguration (not all attributes)
 */
public class AppConfigApplication {
	private String id;
	private String title;
	private AppConfigApplicationConfiguration configuration;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public AppConfigApplicationConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(AppConfigApplicationConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AppConfigApplication [id=");
		builder.append(id);
		builder.append(", title=");
		builder.append(title);
		builder.append(", configuration=");
		builder.append(configuration);
		builder.append("]");
		return builder.toString();
	}
}
