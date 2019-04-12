package de.ascendro.f4m.service.profile.model.get.app;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import com.google.gson.JsonElement;

public class GetAppConfigurationResponse implements JsonMessageContent {
	private JsonElement appConfig;

	public GetAppConfigurationResponse() {
	}

	public GetAppConfigurationResponse(JsonElement appConfig) {
		this.appConfig = appConfig;
	}

	public JsonElement getAppConfig() {
		return appConfig;
	}

	public void setAppConfig(JsonElement appConfig) {
		this.appConfig = appConfig;
	}

}
