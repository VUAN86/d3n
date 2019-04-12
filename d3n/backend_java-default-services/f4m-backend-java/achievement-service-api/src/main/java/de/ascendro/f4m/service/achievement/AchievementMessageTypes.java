package de.ascendro.f4m.service.achievement;

import de.ascendro.f4m.service.json.model.type.MessageType;

public enum AchievementMessageTypes implements MessageType {

    ACHIEVEMENT_LIST, ACHIEVEMENT_LIST_RESPONSE,
    USER_ACHIEVEMENT_LIST, USER_ACHIEVEMENT_LIST_RESPONSE,
    ACHIEVEMENT_GET, ACHIEVEMENT_GET_RESPONSE,
    USER_ACHIEVEMENT_GET, USER_ACHIEVEMENT_GET_RESPONSE,

    BADGE_LIST, BADGE_LIST_RESPONSE,
    USER_BADGE_LIST, USER_BADGE_LIST_RESPONSE,
    BADGE_GET, BADGE_GET_RESPONSE,
    USER_BADGE_GET, USER_BADGE_GET_RESPONSE,
    USER_BADGE_INSTANCE_LIST, USER_BADGE_INSTANCE_LIST_RESPONSE,

    TOP_USERS_LIST, TOP_USERS_LIST_RESPONSE;

    public static final String SERVICE_NAME = "achievement";

    @Override
    public String getShortName() {
        return convertEnumNameToMessageShortName(name());
    }

    @Override
    public String getNamespace() {
        return SERVICE_NAME;
    }
}
