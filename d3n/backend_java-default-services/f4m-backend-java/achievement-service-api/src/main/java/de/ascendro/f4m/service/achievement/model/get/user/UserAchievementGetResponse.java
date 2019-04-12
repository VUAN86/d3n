package de.ascendro.f4m.service.achievement.model.get.user;

import de.ascendro.f4m.server.achievement.model.UserAchievement;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserAchievementGetResponse implements JsonMessageContent {
    private UserAchievement userAchievement;

    public UserAchievementGetResponse(UserAchievement userAchievement) {
        this.userAchievement = userAchievement;
    }

    public UserAchievement getUserAchievement() {
        return userAchievement;
    }

    public void setUserAchievement(UserAchievement userAchievement) {
        this.userAchievement = userAchievement;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserAchievementGetResponse [");
        builder.append("userAchievement=").append(userAchievement);
        builder.append("]");
        return builder.toString();
    }
}
