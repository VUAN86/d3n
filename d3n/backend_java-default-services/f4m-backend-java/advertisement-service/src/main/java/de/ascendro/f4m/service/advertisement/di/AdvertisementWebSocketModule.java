package de.ascendro.f4m.service.advertisement.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import de.ascendro.f4m.advertisement.AdvertisementMessageTypeMapper;
import de.ascendro.f4m.server.advertisement.AdvertisementManager;
import de.ascendro.f4m.server.advertisement.dao.AdvertisementDao;
import de.ascendro.f4m.server.advertisement.dao.AdvertisementDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDaoImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.advertisement.client.AdvertisementServiceClientMessageHandler;
import de.ascendro.f4m.service.advertisement.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.advertisement.client.DependencyServicesCommunicatorImpl;
import de.ascendro.f4m.service.advertisement.model.schema.AdvertisementMessageSchemaMapper;
import de.ascendro.f4m.service.advertisement.server.AdvertisementServiceServerMessageHandler;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;

public class AdvertisementWebSocketModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AdvertisementMessageTypeMapper.class).in(Singleton.class);
        bind(JsonMessageTypeMap.class).to(AdvertisementDefaultMessageMapper.class).in(Singleton.class);
        bind(JsonMessageSchemaMap.class).to(AdvertisementMessageSchemaMapper.class).in(Singleton.class);
        bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);
        bind(DependencyServicesCommunicator.class).to(DependencyServicesCommunicatorImpl.class).in(Singleton.class);


        bind(AdvertisementDao.class).to(AdvertisementDaoImpl.class).in(Singleton.class);
        bind(ApplicationConfigurationAerospikeDao.class).to(ApplicationConfigurationAerospikeDaoImpl.class).in(Singleton.class);

        // Client
        bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
                AdvertisementServiceClientMessageHandlerProvider.class);
        // Server
        bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class).to(
                AdvertisementServiceServerMessageHandlerProvider.class);
    }

    static class AdvertisementServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

        @Inject
        private DependencyServicesCommunicator dependencyServicesCommunicator;

        @Inject
        private Tracker tracker;

        @Inject
        private CommonUserWinningAerospikeDao userWinningDao;

        @Override
        protected JsonMessageHandler createServiceMessageHandler() {
            return new AdvertisementServiceClientMessageHandler(dependencyServicesCommunicator,
                    tracker, userWinningDao);
        }
    }

    static class AdvertisementServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
        @Inject
        private AdvertisementManager advertisementManager;

        @Inject
        private ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;


        @Override
        protected JsonMessageHandler createServiceMessageHandler() {
            return new AdvertisementServiceServerMessageHandler(advertisementManager, applicationConfigurationAerospikeDao);
        }

    }
}
