package de.ascendro.f4m.service.friend.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.WebSocketModule;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.friend.BuddyManager;
import de.ascendro.f4m.service.friend.client.FriendManagerServiceClientMessageHandler;
import de.ascendro.f4m.service.friend.client.PaymentServiceCommunicator;
import de.ascendro.f4m.service.friend.model.schema.FriendManagerMessageSchemaMapper;
import de.ascendro.f4m.service.friend.server.FriendManagerServiceServerMessageHandler;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.util.EventServiceClient;

public class FriendManagerWebSocketModule extends WebSocketModule {

	@Override
	protected void configure() {
		bind(JsonMessageTypeMap.class).to(FriendManagerDefaultMessageTypeMapper.class).in(Singleton.class);
		bind(JsonMessageSchemaMap.class).to(FriendManagerMessageSchemaMapper.class).in(Singleton.class);
        bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(com.google.inject.Singleton.class);
		bind(ApplicationConfigurationAerospikeDao.class).to(ApplicationConfigurationAerospikeDaoImpl.class).in(Singleton.class);

		//Client
		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
				FriendManagerServiceClientMessageHandlerProvider.class);

		//Server		
		bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class).to(
				FriendManagerServiceServerMessageHandlerProvider.class);
	}

	static class FriendManagerServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Inject
		private BuddyManager buddyManager;
		
		@Inject
		private EventServiceClient eventServiceClient;

		@Inject
		private CommonProfileAerospikeDao profileDao;
		
		@Inject
		private Tracker tracker;

        @Inject
		private CommonProfileAerospikeDao commonProfileAerospikeDao;

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new FriendManagerServiceClientMessageHandler(buddyManager, eventServiceClient, profileDao, tracker,
					commonProfileAerospikeDao);
		}
	}

	static class FriendManagerServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Inject
		private BuddyManager friendManager;

		@Inject
		private CommonProfileAerospikeDao profileDao;

		@Inject
		private Tracker tracker;

		@Inject
		private PaymentServiceCommunicator paymentServiceCommunicator;

        @Inject
        private CommonProfileAerospikeDao commonProfileAerospikeDao;

        @Inject
		private ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new FriendManagerServiceServerMessageHandler(friendManager, profileDao, tracker,
					paymentServiceCommunicator, commonProfileAerospikeDao, applicationConfigurationAerospikeDao);
		}
	}
}
