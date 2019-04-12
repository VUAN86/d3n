package de.ascendro.f4m.service.achievement.di;

import javax.inject.Inject;
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
import de.ascendro.f4m.server.achievement.util.AchievementPrimaryKeyUtil;
import de.ascendro.f4m.server.achievement.util.BadgePrimaryKeyUtil;
import de.ascendro.f4m.server.achievement.util.UserAchievementPrimaryKeyUtil;
import de.ascendro.f4m.server.achievement.util.UserBadgePrimaryKeyUtil;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDaoImpl;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.service.achievement.AchievementMessageTypeMapper;
import de.ascendro.f4m.service.achievement.client.AchievementServiceClientMessageHandler;
import de.ascendro.f4m.service.achievement.model.schema.AchievementMessageSchemaMapper;
import de.ascendro.f4m.service.achievement.server.AchievementServiceServerMessageHandler;
import de.ascendro.f4m.service.achievement.util.AchievementManager;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.handler.ClientMesageHandler;
import de.ascendro.f4m.service.handler.ServerMesageHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.logging.LoggingUtil;
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
//        bind(EventServiceClient.class).to(EventServiceClientImpl.class).in(Singleton.class);

        bind(AchievementMessageTypeMapper.class).in(Singleton.class);
        bind(JsonMessageTypeMap.class).to(AchievementDefaultMessageMapper.class).in(Singleton.class);
        bind(BadgePrimaryKeyUtil.class).in(Singleton.class);
        bind(AchievementPrimaryKeyUtil.class).in(Singleton.class);
        bind(UserBadgePrimaryKeyUtil.class).in(Singleton.class);
        bind(UserAchievementPrimaryKeyUtil.class).in(Singleton.class);
        bind(JsonMessageSchemaMap.class).to(AchievementMessageSchemaMapper.class).in(Singleton.class);

        // Server
        bind(ServerMesageHandler.class).to(AchievementServiceServerMessageHandlerProvider.class);
        // Client
        bind(ClientMesageHandler.class).to(AchievementServiceClientMessageHandlerProvider.class);
    }

    static class AchievementServiceClientMessageHandlerProvider implements ClientMesageHandler {

        @Inject
        private Config config;
        @Inject
        private LoggingUtil loggedMessageUtil;
        @Inject
        protected JsonMessageUtil jsonMessageUtil;

        @Override
        public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
            AchievementServiceClientMessageHandler handler = new AchievementServiceClientMessageHandler();
            handler.setConfig(config);
            handler.setJsonMessageUtil(jsonMessageUtil);
            handler.setLoggingUtil(loggedMessageUtil);
            return handler;
        }
    }

    static class AchievementServiceServerMessageHandlerProvider implements ServerMesageHandler {
        @Inject
        private AchievementManager achievementManager;
        @Inject
        private Config config;
        @Inject
        private LoggingUtil loggedMessageUtil;
        @Inject
        protected JsonMessageUtil jsonMessageUtil;
        @Override
        public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
            AchievementServiceServerMessageHandler handler = new AchievementServiceServerMessageHandler(achievementManager);
            handler.setConfig(config);
            handler.setJsonMessageUtil(jsonMessageUtil);
            handler.setLoggingUtil(loggedMessageUtil);
            return handler;
        }

    }
}