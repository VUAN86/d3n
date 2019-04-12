package de.ascendro.f4m.server.analytics.model.base;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class BaseEvent extends JsonObjectWrapper {

	public BaseEvent() {
		super();
	}

	public BaseEvent(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	protected boolean isUserInfoRequired() {
		return true;
	}
	
	public boolean isUserIdRequired() {
		return isUserInfoRequired();
	}
	
	public boolean isAppIdRequired() {
		return isUserInfoRequired();
	}
	
	public boolean isSessionIpRequired() {
		return isUserInfoRequired();
	}

	public boolean isTenantIdRequired() {
		return false;
	}

	public Integer getAchievementIncrementingCounter(AchievementRule rule) {
		throw new UnsupportedOperationException();
	}
	
}
