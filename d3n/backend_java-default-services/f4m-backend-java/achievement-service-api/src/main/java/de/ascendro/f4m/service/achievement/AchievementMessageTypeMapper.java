package de.ascendro.f4m.service.achievement;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.achievement.model.get.AchievementGetRequest;
import de.ascendro.f4m.service.achievement.model.get.AchievementGetResponse;
import de.ascendro.f4m.service.achievement.model.get.AchievementListRequest;
import de.ascendro.f4m.service.achievement.model.get.AchievementListResponse;
import de.ascendro.f4m.service.achievement.model.get.BadgeGetRequest;
import de.ascendro.f4m.service.achievement.model.get.BadgeGetResponse;
import de.ascendro.f4m.service.achievement.model.get.BadgeListRequest;
import de.ascendro.f4m.service.achievement.model.get.BadgeListResponse;
import de.ascendro.f4m.service.achievement.model.get.user.TopUsersRequest;
import de.ascendro.f4m.service.achievement.model.get.user.TopUsersResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserAchievementGetRequest;
import de.ascendro.f4m.service.achievement.model.get.user.UserAchievementGetResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserAchievementListRequest;
import de.ascendro.f4m.service.achievement.model.get.user.UserAchievementListResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeGetRequest;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeGetResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeInstanceListRequest;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeInstanceListResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeListRequest;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeListResponse;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;

public class AchievementMessageTypeMapper extends JsonMessageTypeMapImpl {

	private static final long serialVersionUID = 8926394330929110698L;

	public AchievementMessageTypeMapper() {
        init();
    }

    protected void init() {
        this.register(AchievementMessageTypes.ACHIEVEMENT_LIST.getMessageName(),
                new TypeToken<AchievementListRequest>() {}.getType());
        this.register(AchievementMessageTypes.ACHIEVEMENT_LIST_RESPONSE.getMessageName(),
                new TypeToken<AchievementListResponse>() {}.getType());

        this.register(AchievementMessageTypes.USER_ACHIEVEMENT_LIST.getMessageName(),
                new TypeToken<UserAchievementListRequest>() {}.getType());
        this.register(AchievementMessageTypes.USER_ACHIEVEMENT_LIST_RESPONSE.getMessageName(),
                new TypeToken<UserAchievementListResponse>() {}.getType());

        this.register(AchievementMessageTypes.ACHIEVEMENT_GET.getMessageName(),
                new TypeToken<AchievementGetRequest>() {}.getType());
        this.register(AchievementMessageTypes.ACHIEVEMENT_GET_RESPONSE.getMessageName(),
                new TypeToken<AchievementGetResponse>() {}.getType());

        this.register(AchievementMessageTypes.USER_ACHIEVEMENT_GET.getMessageName(),
                new TypeToken<UserAchievementGetRequest>() {}.getType());
        this.register(AchievementMessageTypes.USER_ACHIEVEMENT_GET_RESPONSE.getMessageName(),
                new TypeToken<UserAchievementGetResponse>() {}.getType());

        this.register(AchievementMessageTypes.BADGE_LIST.getMessageName(),
                new TypeToken<BadgeListRequest>() {}.getType());
        this.register(AchievementMessageTypes.BADGE_LIST_RESPONSE.getMessageName(),
                new TypeToken<BadgeListResponse>() {}.getType());


        this.register(AchievementMessageTypes.USER_BADGE_LIST.getMessageName(),
                new TypeToken<UserBadgeListRequest>() {}.getType());
        this.register(AchievementMessageTypes.USER_BADGE_LIST_RESPONSE.getMessageName(),
                new TypeToken<UserBadgeListResponse>() {}.getType());

        this.register(AchievementMessageTypes.BADGE_GET.getMessageName(),
                new TypeToken<BadgeGetRequest>() {}.getType());
        this.register(AchievementMessageTypes.BADGE_GET_RESPONSE.getMessageName(),
                new TypeToken<BadgeGetResponse>() {}.getType());

        this.register(AchievementMessageTypes.USER_BADGE_GET.getMessageName(),
                new TypeToken<UserBadgeGetRequest>() {}.getType());
        this.register(AchievementMessageTypes.USER_BADGE_GET_RESPONSE.getMessageName(),
                new TypeToken<UserBadgeGetResponse>() {}.getType());

        this.register(AchievementMessageTypes.USER_BADGE_INSTANCE_LIST.getMessageName(),
                new TypeToken<UserBadgeInstanceListRequest>() {}.getType());
        this.register(AchievementMessageTypes.USER_BADGE_INSTANCE_LIST_RESPONSE.getMessageName(),
                new TypeToken<UserBadgeInstanceListResponse>() {}.getType());

        this.register(AchievementMessageTypes.TOP_USERS_LIST.getMessageName(),
                new TypeToken<TopUsersRequest>() {}.getType());
        this.register(AchievementMessageTypes.TOP_USERS_LIST_RESPONSE.getMessageName(),
                new TypeToken<TopUsersResponse>() {}.getType());
    }
}
