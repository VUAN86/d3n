package de.ascendro.f4m.service.result.engine.di;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDaoImpl;
import de.ascendro.f4m.server.history.dao.GameHistoryPrimaryKeyUtil;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.request.jackpot.JackpotDataGetter;
import de.ascendro.f4m.server.result.dao.ResultEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypeMapper;
import de.ascendro.f4m.service.result.engine.client.ResultEngineServiceClientMessageHandler;
import de.ascendro.f4m.service.result.engine.model.schema.ResultEngineMessageSchemaMapper;
import de.ascendro.f4m.service.result.engine.server.ResultEngineServiceServerMessageHandler;
import de.ascendro.f4m.service.result.engine.util.ResultEngineUtil;
import de.ascendro.f4m.service.result.engine.util.UserInteractionHandler;
import de.ascendro.f4m.service.util.EventServiceClient;

public class ResultEngineWebSocketModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ResultEngineMessageTypeMapper.class).in(Singleton.class);
        bind(JsonMessageTypeMap.class).to(ResultEngineDefaultMessageMapper.class).in(Singleton.class);
        bind(ResultEnginePrimaryKeyUtil.class).in(Singleton.class);
        bind(GameHistoryPrimaryKeyUtil.class).in(Singleton.class);
        bind(JsonMessageSchemaMap.class).to(ResultEngineMessageSchemaMapper.class).in(Singleton.class);
        
        // Client
        bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
                ResultEngineServiceClientMessageHandlerProvider.class);

        // Server
        bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class).to(
                ResultEngineServiceServerMessageHandlerProvider.class);
    }

    static class ResultEngineServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

        @Inject
        private ResultEngineUtil resultEngineUtil;

        @Inject
        private UserInteractionHandler userInteractionHandler;
        
        @Inject
        private TransactionLogAerospikeDao transactionLogDao;
        
        @Inject
        private EventServiceClient eventServiceClient;

        @Inject
        private Tracker tracker;

        @Override
        protected JsonMessageHandler createServiceMessageHandler() {
            return new ResultEngineServiceClientMessageHandler(resultEngineUtil, userInteractionHandler, transactionLogDao, eventServiceClient, tracker);
        }
    }

    static class ResultEngineServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

        @Inject
        private ResultEngineUtil resultEngineUtil;

        @Inject
        private UserInteractionHandler userInteractionHandler;
        
        @Inject
        private CommonProfileAerospikeDao commonProfileAerospikeDao;
        
        @Inject
        private CommonBuddyElasticDao commonBuddyElasticDao;

        @Inject
        private JackpotDataGetter jackpotDataGetter;

        @Inject
        private CommonGameInstanceAerospikeDaoImpl gameInstanceAerospikeDao;

        @Inject
        private CommonUserWinningAerospikeDao userWinningComponentAerospikeDao;

        @Inject
        private TransactionLogAerospikeDao transactionLogAerospikeDao;

        @Override
        protected JsonMessageHandler createServiceMessageHandler() {
            return new ResultEngineServiceServerMessageHandler(resultEngineUtil, userInteractionHandler,
                    commonProfileAerospikeDao, commonBuddyElasticDao, jackpotDataGetter, gameInstanceAerospikeDao,
                    userWinningComponentAerospikeDao, transactionLogAerospikeDao);
        }

    }
}
