package de.ascendro.f4m.service.analytics.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.achievement.dao.TopUsersElasticDao;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.server.achievement.model.UserProgressES;
import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.profile.model.Profile;

public class AnalyticServiceUtil {

    private final CommonProfileAerospikeDao profileAerospikeDaoImpl;
    private final CommonGameInstanceAerospikeDao gameInstanceDao;
    private final ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;
    private final TopUsersElasticDao topUsersElasticDao;

    @Inject
    public AnalyticServiceUtil(CommonProfileAerospikeDao profileAerospikeDaoImpl,
                            CommonGameInstanceAerospikeDao gameInstanceDao,
                            ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao,
                            TopUsersElasticDao topUsersElasticDao) {
        this.profileAerospikeDaoImpl = profileAerospikeDaoImpl;
        this.gameInstanceDao = gameInstanceDao;
        this.applicationConfigurationAerospikeDao = applicationConfigurationAerospikeDao;
        this.topUsersElasticDao = topUsersElasticDao;
    }

    public GameInstance getGameInstance(String gameInstanceId) {
        return gameInstanceDao.getGameInstance(gameInstanceId);
    }

    public Profile getProfile(String userId) {
        return profileAerospikeDaoImpl.getProfile(userId);
    }

    public String getCurrency(EventContent content) {
        return applicationConfigurationAerospikeDao.getAppConfiguration(content.getTenantId(), content.getAppId()).getTenant().getCurrency();
    }
    
    public void addBadge(String userId, String tenantId, BadgeType type) {
    	if (BadgeType.BATTLE.equals(type)) {
    		throw new IllegalArgumentException("Battle Badges are not counted.");
    	}
    	UserProgressES userProgress = topUsersElasticDao.getUserProgress(tenantId, userId);
    	if (null == userProgress) {
    		userProgress = new UserProgressES(userId, tenantId, 0, 0);
    	}
    	if (BadgeType.COMMUNITY.equals(type)) {
    		userProgress.setNumOfCommunityBadges(userProgress.getNumOfCommunityBadges() + 1);
    	} else if (BadgeType.GAME.equals(type)) {
    		userProgress.setNumOfGameBadges(userProgress.getNumOfCommunityBadges() + 1);
    	}
    	topUsersElasticDao.createOrUpdate(userProgress, tenantId);
    }
}
