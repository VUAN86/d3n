package de.ascendro.f4m.service.achievement.di;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.dao.BadgeAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.BadgeAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.dao.UserAchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.UserAchievementAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.dao.UserBadgeAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.UserBadgeAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.dao.TopUsersElasticDao;
import de.ascendro.f4m.server.achievement.dao.TopUsersElasticDaoImpl;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDaoImpl;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.EventServiceClientImpl;

public class AchievementServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(F4MConfigImpl.class).to(AchievementConfig.class);
        bind(AchievementConfig.class).in(Singleton.class);

        bind(AerospikeClientProvider.class).in(Singleton.class);
        bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
        bind(AchievementAerospikeDao.class).to(AchievementAerospikeDaoImpl.class).in(Singleton.class);
        bind(UserAchievementAerospikeDao.class).to(UserAchievementAerospikeDaoImpl.class).in(Singleton.class);
        bind(BadgeAerospikeDao.class).to(BadgeAerospikeDaoImpl.class).in(Singleton.class);
        bind(UserBadgeAerospikeDao.class).to(UserBadgeAerospikeDaoImpl.class).in(Singleton.class);
        bind(TopUsersElasticDao.class).to(TopUsersElasticDaoImpl.class).in(Singleton.class);
        bind(CommonBuddyElasticDao.class).to(CommonBuddyElasticDaoImpl.class).in(Singleton.class);
        bind(ApplicationConfigurationAerospikeDao.class).to(ApplicationConfigurationAerospikeDaoImpl.class)
                .in(Singleton.class);

        bind(AerospikeDao.class).to(AchievementAerospikeDao.class).in(Singleton.class);
        bind(EventServiceClient.class).to(EventServiceClientImpl.class).in(Singleton.class);
    }
}