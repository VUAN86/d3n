package de.ascendro.f4m.service.achievement.model.get.user;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserAchievementGetRequest implements JsonMessageContent {
	private String achievementId;

	public String getAchievementId() {
		return achievementId;
	}

	public void setAchievementId(String achievementId) {
		this.achievementId = achievementId;
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserAchievementGetRequest [");
        builder.append("achievementId=").append(achievementId);
        builder.append("]");
        return builder.toString();
    }
}
