package de.ascendro.f4m.service.achievement.model.get.user;

import de.ascendro.f4m.server.achievement.model.UserBadge;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserBadgeGetResponse implements JsonMessageContent {
    private UserBadge userBadge;

    public UserBadgeGetResponse(UserBadge userBadge) {
        this.userBadge = userBadge;
    }

    public UserBadge getUserBadge() {
        return userBadge;
    }

    public void setUserBadge(UserBadge userBadge) {
        this.userBadge = userBadge;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserBadgeGetResponse [");
        builder.append("userBadge=").append(getUserBadge());
        builder.append("]");
        return builder.toString();
    }
}
