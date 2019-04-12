package de.ascendro.f4m.service.tombola.di;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.server.voucher.CommonVoucherAerospikeDao;
import de.ascendro.f4m.server.voucher.CommonVoucherAerospikeDaoImpl;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDaoImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.handler.ClientMesageHandler;
import de.ascendro.f4m.service.handler.ServerMesageHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.tombola.TombolaDrawingEngine;
import de.ascendro.f4m.service.tombola.TombolaManager;
import de.ascendro.f4m.service.tombola.TombolaMessageTypeMapper;
import de.ascendro.f4m.service.tombola.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.tombola.client.TombolaServiceClientMessageHandler;
import de.ascendro.f4m.service.tombola.config.TombolaConfig;
import de.ascendro.f4m.service.tombola.dao.TombolaAerospikeDao;
import de.ascendro.f4m.service.tombola.dao.TombolaAerospikeDaoImpl;
import de.ascendro.f4m.service.tombola.model.schema.TombolaMessageSchemaMapper;
import de.ascendro.f4m.service.tombola.server.TombolaServiceServerMessageHandler;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.EventServiceClientImpl;

public class TombolaServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(F4MConfigImpl.class).to(TombolaConfig.class);
        bind(TombolaConfig.class).in(Singleton.class);

        bind(AerospikeClientProvider.class).in(Singleton.class);
        bind(TombolaAerospikeDao.class).to(TombolaAerospikeDaoImpl.class).in(Singleton.class);
        bind(AerospikeDao.class).to(TombolaAerospikeDao.class).in(Singleton.class);
        bind(CommonUserWinningAerospikeDao.class).to(CommonUserWinningAerospikeDaoImpl.class).in(Singleton.class);
        bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
        bind(CommonVoucherAerospikeDao.class).to(CommonVoucherAerospikeDaoImpl.class).in(Singleton.class);

        bind(TombolaMessageTypeMapper.class).in(Singleton.class);
        bind(JsonMessageTypeMap.class).to(TombolaDefaultMessageMapper.class).in(Singleton.class);
        bind(PrimaryKeyUtil.class).in(Singleton.class);
        bind(JsonMessageSchemaMap.class).to(TombolaMessageSchemaMapper.class).in(Singleton.class);
        bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);

//        bind(EventServiceClient.class).to(EventServiceClientImpl.class).in(Singleton.class);

        //Analytics
        bind(Tracker.class).to(TrackerImpl.class).in(Singleton.class);
        bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(Singleton.class);

        // Server
        bind(ServerMesageHandler.class).to(TombolaServiceServerMessageHandlerProvider.class);
        // Client
        bind(ClientMesageHandler.class).to(TombolaServiceClientMessageHandlerProvider.class);
    }

    static class TombolaServiceClientMessageHandlerProvider implements ClientMesageHandler {

        @Inject
        private TombolaManager tombolaManager;
//        @Inject
//        private EventServiceClient eventServiceClient;
        @Inject
        private TombolaDrawingEngine tombolaDrawingEngine;
        @Inject
        private Tracker tracker;
        @Inject
        private DependencyServicesCommunicator dependencyServicesCommunicator;
        @Inject
        private CommonUserWinningAerospikeDao userWinningDao;
        @Inject
        private Config config;
        @Inject
        private LoggingUtil loggedMessageUtil;
        @Inject
        protected JsonMessageUtil jsonMessageUtil;


        public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
            TombolaServiceClientMessageHandler handler = new TombolaServiceClientMessageHandler(tombolaManager, tombolaDrawingEngine,
                    tracker, dependencyServicesCommunicator, userWinningDao);
            handler.setConfig(config);
            handler.setJsonMessageUtil(jsonMessageUtil);
            handler.setLoggingUtil(loggedMessageUtil);
            return handler;
        }
    }

    static class TombolaServiceServerMessageHandlerProvider implements ServerMesageHandler {
        @Inject
        private TombolaManager tombolaManager;
        @Inject
        private TombolaDrawingEngine tombolaDrawingEngine;
        @Inject
        private Config config;
        @Inject
        private LoggingUtil loggedMessageUtil;
        @Inject
        protected JsonMessageUtil jsonMessageUtil;

        public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
            TombolaServiceServerMessageHandler handler = new TombolaServiceServerMessageHandler(tombolaManager, tombolaDrawingEngine);
            handler.setConfig(config);
            handler.setJsonMessageUtil(jsonMessageUtil);
            handler.setLoggingUtil(loggedMessageUtil);
            return handler;
        }

    }
}
