package de.ascendro.f4m.service.promocode.di;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import com.google.inject.Inject;
import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.service.communicator.RabbitClientSender;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.handler.ClientMesageHandler;
import de.ascendro.f4m.service.handler.ServerMesageHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.promocode.PromocodeMessageTypeMapper;
import de.ascendro.f4m.service.promocode.client.PromocodeServiceClientMessageHandler;
import de.ascendro.f4m.service.promocode.config.PromocodeConfig;
import de.ascendro.f4m.service.promocode.dao.PromocodeAerospikeDao;
import de.ascendro.f4m.service.promocode.dao.PromocodeAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.service.promocode.model.schema.PromocodeMessageSchemaMapper;
import de.ascendro.f4m.service.promocode.server.PromocodeServiceServerMessageHandler;
import de.ascendro.f4m.service.promocode.util.PromocodeManager;
import de.ascendro.f4m.service.promocode.util.PromocodePrimaryKeyUtil;

public class PromocodeServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(F4MConfigImpl.class).to(PromocodeConfig.class);
        bind(PromocodeConfig.class).in(Singleton.class);

        bind(AerospikeClientProvider.class).in(Singleton.class);
        bind(PromocodeAerospikeDao.class).to(PromocodeAerospikeDaoImpl.class).in(Singleton.class);
        bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);

        bind(AerospikeDao.class).to(PromocodeAerospikeDao.class).in(Singleton.class);
        bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);

        bind(PromocodeMessageTypeMapper.class).in(Singleton.class);
        bind(JsonMessageTypeMap.class).to(PromocodeDefaultMessageMapper.class).in(Singleton.class);
        bind(ProfileMessageTypeMapper.class).in(Singleton.class);
        bind(PaymentMessageTypeMapper.class).in(Singleton.class);
        bind(PromocodePrimaryKeyUtil.class).in(Singleton.class);
        bind(JsonMessageSchemaMap.class).to(PromocodeMessageSchemaMapper.class).in(Singleton.class);


        //Analytics
        bind(Tracker.class).to(TrackerImpl.class).in(Singleton.class);
        bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(Singleton.class);

        // Server
        bind(ServerMesageHandler.class).to(PromocodeServiceServerMessageHandlerProvider.class);
        // Client
        bind(ClientMesageHandler.class).to(PromocodeServiceClientMessageHandlerProvider.class);
    }

    static class PromocodeServiceClientMessageHandlerProvider implements ClientMesageHandler {

        @Inject
        private PromocodeManager promocodeManager;
        @Inject
        private TransactionLogAerospikeDao transactionLogAerospikeDao;
        @Inject
        private Config config;
        @Inject
        private LoggingUtil loggedMessageUtil;
        @Inject
        protected JsonMessageUtil jsonMessageUtil;


        public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
            PromocodeServiceClientMessageHandler handler = new PromocodeServiceClientMessageHandler(promocodeManager, transactionLogAerospikeDao);
            handler.setConfig(config);
            handler.setJsonMessageUtil(jsonMessageUtil);
            handler.setLoggingUtil(loggedMessageUtil);
            return handler;
        }
    }

    static class PromocodeServiceServerMessageHandlerProvider implements ServerMesageHandler {
        @Inject
        private PromocodeManager promocodeManager;
        @Inject
        private Config config;
        @Inject
        private LoggingUtil loggedMessageUtil;
        @Inject
        protected JsonMessageUtil jsonMessageUtil;

        public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
            PromocodeServiceServerMessageHandler handler = new PromocodeServiceServerMessageHandler(promocodeManager);
            handler.setConfig(config);
            handler.setJsonMessageUtil(jsonMessageUtil);
            handler.setLoggingUtil(loggedMessageUtil);
            return handler;
        }

    }
}
