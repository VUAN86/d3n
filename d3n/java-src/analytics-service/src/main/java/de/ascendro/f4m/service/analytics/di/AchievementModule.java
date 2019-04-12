package de.ascendro.f4m.service.analytics.di;

import javax.inject.Singleton;

import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.dao.BadgeAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.BadgeAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.dao.TenantsWithAchievementsAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.TenantsWithAchievementsAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.dao.TopUsersElasticDao;
import de.ascendro.f4m.server.achievement.dao.TopUsersElasticDaoImpl;
import de.ascendro.f4m.server.achievement.dao.UserAchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.UserAchievementAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.dao.UserBadgeAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.UserBadgeAerospikeDaoImpl;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDaoImpl;
import de.ascendro.f4m.service.analytics.module.achievement.AchievementProcessor;
import de.ascendro.f4m.service.analytics.module.achievement.IEventProcessor;
import de.ascendro.f4m.service.analytics.module.achievement.processor.AchievementsLoader;
import de.ascendro.f4m.service.analytics.module.achievement.processor.EventProcessor;
import de.ascendro.f4m.service.analytics.module.achievement.updater.AchievementsUpdaterScheduler;
import de.ascendro.f4m.service.analytics.module.jobs.IAchievementProcessor;
import de.ascendro.f4m.service.analytics.util.AnalyticServiceUtil;

public class AchievementModule extends BaseConsumerModule {

    @Override
    protected void configure() {
        bind(AchievementConfig.class).in(Singleton.class);
        bind(AchievementAerospikeDao.class).to(AchievementAerospikeDaoImpl.class).in(Singleton.class);
        bind(UserAchievementAerospikeDao.class).to(UserAchievementAerospikeDaoImpl.class).in(Singleton.class);
        bind(BadgeAerospikeDao.class).to(BadgeAerospikeDaoImpl.class).in(Singleton.class);
        bind(UserBadgeAerospikeDao.class).to(UserBadgeAerospikeDaoImpl.class).in(Singleton.class);
        bind(TenantsWithAchievementsAerospikeDao.class).to(TenantsWithAchievementsAerospikeDaoImpl.class).in(Singleton.class);
        bind(TopUsersElasticDao.class).to(TopUsersElasticDaoImpl.class).in(Singleton.class);
        bind(CommonBuddyElasticDao.class).to(CommonBuddyElasticDaoImpl.class).in(Singleton.class);


        bind(AnalyticServiceUtil.class).in(Singleton.class);
        bind(IAchievementProcessor.class).to(AchievementProcessor.class).in(Singleton.class);
        bind(EventProcessor.class).in(Singleton.class);
        bind(IEventProcessor.class).to(EventProcessor.class).in(Singleton.class);

        bind(AchievementsLoader.class).in(Singleton.class);
        bind(AchievementsUpdaterScheduler.class).in(Singleton.class);

    }
}
