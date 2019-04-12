package de.ascendro.f4m.service.profile.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.WebSocketModule;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.profile.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.profile.client.ProfileServiceClientMessageHandler;
import de.ascendro.f4m.service.profile.dao.EndConsumerInvoiceAerospikeDao;
import de.ascendro.f4m.service.profile.model.schema.ProfileMessageSchemaMapper;
import de.ascendro.f4m.service.profile.server.ProfileServiceServerMessageHandler;
import de.ascendro.f4m.service.profile.util.ProfileUtil;

public class ProfileWebSocketModule extends WebSocketModule {
	@Override
	protected void configure() {
		bind(ProfileMessageTypeMapper.class).in(Singleton.class);
		bind(JsonMessageTypeMap.class).to(ProfileDefaultMessageMapper.class).in(Singleton.class);
		bind(JsonMessageSchemaMap.class).to(ProfileMessageSchemaMapper.class).in(Singleton.class);

		//Client
		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
				ProfileServiceClientMessageHandlerProvider.class);

		//Server		
		bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class).to(
				ProfileServiceServerMessageHandlerProvider.class);
	}

	static class ProfileServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
		@Inject
		private ProfileUtil profileUtil;

		@Inject
		private ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;
		
		@Inject
		private DependencyServicesCommunicator dependencyServicesCommunicator;

		@Inject
		private EndConsumerInvoiceAerospikeDao endConsumerInvoiceAerospikeDao;

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new ProfileServiceServerMessageHandler(profileUtil, applicationConfigurationAerospikeDao,
					dependencyServicesCommunicator, endConsumerInvoiceAerospikeDao);
		}
	}

	static class ProfileServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new ProfileServiceClientMessageHandler();
		}
	}
}
