package de.ascendro.f4m.server.analytics.model;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.analytics.model.base.GameBaseEvent;

public class InviteEvent extends GameBaseEvent {

    public static final String FRIENDS_INVITED_PROPERTY = "friendsInvited";
    public static final String BONUS_INVITE_PROPERTY = "bonusInvite";
    public static final String INVITED_FROM_FRIENDS = "invitedFromFriends";
    public static final String FRIENDS_INVITED_TOO = "friendsInvitedToo";
    public static final String FRIENDS_BLOCKED = "friendsBlocked";
    public static final String FRIENDS_UNBLOCKED = "friendsUnblocked";


    private static Map<AchievementRule, String> achievementRulesToProperties;
    static
    {
        achievementRulesToProperties = new HashMap<>();
        achievementRulesToProperties.put(AchievementRule.RULE_GAME_PLAYED, FRIENDS_INVITED_PROPERTY);
    }

    public InviteEvent() {
        //default constructor
    }

    public InviteEvent(JsonObject inviteJsonObject) {
        super(inviteJsonObject);
    }

    public void setFriendsInvited(Integer friendInvited) {
        setProperty(FRIENDS_INVITED_PROPERTY, friendInvited);
    }

    public Integer getFriendsInvited() {
        return getPropertyAsInteger(FRIENDS_INVITED_PROPERTY);
    }

    public void setBonusInvite(Boolean bonusInvite) {
        setProperty(BONUS_INVITE_PROPERTY, bonusInvite);
    }

    public Boolean isBonusInvite() {
        return getPropertyAsBoolean(BONUS_INVITE_PROPERTY);
    }

    public void setInvitedFromFriends(Boolean invitedFromFriends) {
        setProperty(INVITED_FROM_FRIENDS, invitedFromFriends);
    }

    public Boolean isInvitedFromFriends() {
        return getPropertyAsBoolean(INVITED_FROM_FRIENDS);
    }

    public void setFriendsInvitedToo(Boolean friendsInvitedToo) {
        setProperty(FRIENDS_INVITED_TOO, friendsInvitedToo);
    }

    public Boolean isFriendsInvitedToo() {
        return getPropertyAsBoolean(FRIENDS_INVITED_TOO);
    }

    public void setFriendsBlocked(Long friendsBlocked) {
        setProperty(FRIENDS_BLOCKED, friendsBlocked);
    }

    public Long getFriendsBlocked() {
        return getPropertyAsLong(FRIENDS_BLOCKED);
    }

    public void setFriendsUnblocked(Long friendsUnblocked) {
        setProperty(FRIENDS_UNBLOCKED, friendsUnblocked);
    }

    public Long getFriendsUnblocked() {
        return getPropertyAsLong(FRIENDS_UNBLOCKED);
    }

    @Override
    public Integer getAchievementIncrementingCounter(AchievementRule rule) {
        String propertyName = achievementRulesToProperties.get(rule);
        return getPropertyAsInt(propertyName);
    }
}
