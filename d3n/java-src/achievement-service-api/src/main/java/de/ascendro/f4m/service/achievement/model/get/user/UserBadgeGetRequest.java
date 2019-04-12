package de.ascendro.f4m.service.achievement.model.get.user;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserBadgeGetRequest implements JsonMessageContent {
    private String badgeId;

    public String getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserBadgeGetRequest [");
        builder.append("badgeId=").append(badgeId);
        builder.append("]");
        return builder.toString();
    }
}
