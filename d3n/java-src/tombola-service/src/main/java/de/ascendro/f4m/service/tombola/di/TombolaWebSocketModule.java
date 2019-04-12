package de.ascendro.f4m.service.tombola.di;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.tombola.TombolaDrawingEngine;
import de.ascendro.f4m.service.tombola.TombolaManager;
import de.ascendro.f4m.service.tombola.TombolaMessageTypeMapper;
import de.ascendro.f4m.service.tombola.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.tombola.client.TombolaServiceClientMessageHandler;
import de.ascendro.f4m.service.tombola.model.schema.TombolaMessageSchemaMapper;
import de.ascendro.f4m.service.tombola.server.TombolaServiceServerMessageHandler;
import de.ascendro.f4m.service.util.EventServiceClient;

public class TombolaWebSocketModule extends AbstractModule {
    @Override
    protected void configure() {
//        bind(TombolaMessageTypeMapper.class).in(Singleton.class);
//        bind(JsonMessageTypeMap.class).to(TombolaDefaultMessageMapper.class).in(Singleton.class);
//        bind(PrimaryKeyUtil.class).in(Singleton.class);
//        bind(JsonMessageSchemaMap.class).to(TombolaMessageSchemaMapper.class).in(Singleton.class);
//        bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);
//
//        // Client
//        bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
//                TombolaServiceClientMessageHandlerProvider.class);
//
//        // Server
//        bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class).to(
//                TombolaServiceServerMessageHandlerProvider.class);
    }

//    static class TombolaServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
//
//        @Inject
//        TombolaManager tombolaManager;
//
//        @Inject
//    	private EventServiceClient eventServiceClient;
//
//        @Inject
//        private TombolaDrawingEngine tombolaDrawingEngine;
//
//        @Inject
//        private Tracker tracker;
//
//        @Inject
//        private DependencyServicesCommunicator dependencyServicesCommunicator;
//
//        @Inject
//        private CommonUserWinningAerospikeDao userWinningDao;
//
//        @Override
//        protected JsonMessageHandler createServiceMessageHandler() {
//            return new TombolaServiceClientMessageHandler(tombolaManager, eventServiceClient, tombolaDrawingEngine,
//                    tracker, dependencyServicesCommunicator, userWinningDao);
//        }
//
//    }
//
//    static class TombolaServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
//
//        @Inject
//        private TombolaManager tombolaManager;
//
//        @Inject
//        private TombolaDrawingEngine tombolaDrawingEngine;
//
//        @Override
//        protected JsonMessageHandler createServiceMessageHandler() {
//            return new TombolaServiceServerMessageHandler(tombolaManager, tombolaDrawingEngine);
//        }
//
//    }
}
