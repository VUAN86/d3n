package de.ascendro.f4m.service.analytics.notification.di;


import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.service.analytics.client.AnalyticsServiceClientMessageHandler;
import de.ascendro.f4m.service.analytics.module.achievement.processor.AchievementsLoader;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.analytics.notification.schema.AnalyticsMessageSchemaMapper;
import de.ascendro.f4m.service.analytics.server.AnalyticsServiceServerMessageHandler;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.util.EventServiceClient;

public class NotificationWebSocketModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(JsonMessageTypeMap.class).to(AnalyticsDefaultMessageTypeMapper.class).in(Singleton.class);
        bind(JsonMessageSchemaMap.class).to(AnalyticsMessageSchemaMapper.class).in(Singleton.class);

        //Client
        bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
                AnalyticsServiceClientMessageHandlerProvider.class);

        //Server
        bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class).to(
                AnalyticsServiceServerMessageHandlerProvider.class);
    }


    static class AnalyticsServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

        @Inject
        NotificationCommon notificationCommon;

        @Inject
        Tracker tracker;
        
        @Inject
    	EventServiceClient eventServiceClient;
        
        @Inject
        AchievementsLoader achievementsLoader;

        @Override
        protected JsonMessageHandler createServiceMessageHandler() {
            return new AnalyticsServiceClientMessageHandler(notificationCommon, tracker, eventServiceClient, achievementsLoader);
        }
    }

    static class AnalyticsServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

        @Override
        protected JsonMessageHandler createServiceMessageHandler() {
            return new AnalyticsServiceServerMessageHandler();
        }
    }
}
