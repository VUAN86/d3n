package de.ascendro.f4m.server.achievement.dao;

import java.util.List;

import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.server.achievement.model.UserProgressES;

public interface TopUsersElasticDao {

    List<UserProgressES> getTopUsers(String tenantId, BadgeType achievementType, long offset, int limit);

    List<UserProgressES> getTopUsers(String tenantId, BadgeType achievementType, String[] userIds,  long offset, int limit);

    void createOrUpdate(UserProgressES topUser, String tenantId);
    
    UserProgressES getUserProgress(String tenantId, String userId);

}
