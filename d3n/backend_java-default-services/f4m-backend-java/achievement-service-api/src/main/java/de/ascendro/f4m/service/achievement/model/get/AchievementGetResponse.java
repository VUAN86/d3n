package de.ascendro.f4m.service.achievement.model.get;

import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class AchievementGetResponse implements JsonMessageContent {

    private Achievement achievement;

    public AchievementGetResponse(Achievement achievement) {
        this.achievement = achievement;
    }

    public Achievement getAchievement() {
        return achievement;
    }

    public void setAchievement(Achievement achievement) {
        this.achievement = achievement;
    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AchievementGetResponse [");
		builder.append("achievement=").append(achievement);
		builder.append("]");
		return builder.toString();
	}
}
