package de.ascendro.f4m.service.achievement.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import de.ascendro.f4m.server.achievement.util.AchievementPrimaryKeyUtil;
import de.ascendro.f4m.server.achievement.util.BadgePrimaryKeyUtil;
import de.ascendro.f4m.server.achievement.util.UserAchievementPrimaryKeyUtil;
import de.ascendro.f4m.server.achievement.util.UserBadgePrimaryKeyUtil;
import de.ascendro.f4m.service.achievement.AchievementMessageTypeMapper;
import de.ascendro.f4m.service.achievement.client.AchievementServiceClientMessageHandler;
import de.ascendro.f4m.service.achievement.model.schema.AchievementMessageSchemaMapper;
import de.ascendro.f4m.service.achievement.server.AchievementServiceServerMessageHandler;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.achievement.util.AchievementManager;

public class AchievementWebSocketModule extends AbstractModule {
    @Override
    protected void configure() {
//        bind(AchievementMessageTypeMapper.class).in(Singleton.class);
//        bind(JsonMessageTypeMap.class).to(AchievementDefaultMessageMapper.class).in(Singleton.class);
//        bind(BadgePrimaryKeyUtil.class).in(Singleton.class);
//        bind(AchievementPrimaryKeyUtil.class).in(Singleton.class);
//        bind(UserBadgePrimaryKeyUtil.class).in(Singleton.class);
//        bind(UserAchievementPrimaryKeyUtil.class).in(Singleton.class);
//        bind(JsonMessageSchemaMap.class).to(AchievementMessageSchemaMapper.class).in(Singleton.class);
//
//        // Client
//        bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
//                AchievementServiceClientMessageHandlerProvider.class);
//
//        // Server
//        bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class).to(
//                AchievementServiceServerMessageHandlerProvider.class);
    }

//    static class AchievementServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
//
//        @Override
//        protected JsonMessageHandler createServiceMessageHandler() {
//            return new AchievementServiceClientMessageHandler();
//        }
//    }
//
//    static class AchievementServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
//        @Inject
//        private AchievementManager achievementManager;
//        @Override
//        protected JsonMessageHandler createServiceMessageHandler() {
//            return new AchievementServiceServerMessageHandler(achievementManager);
//        }
//
//    }
}
