package de.ascendro.f4m.service.event.di;

import javax.inject.Inject;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.event.activemq.BrokerNetworkActiveMQ;
import de.ascendro.f4m.service.event.client.EventServiceClientMessageHandler;
import de.ascendro.f4m.service.event.pool.EventPool;
import de.ascendro.f4m.service.event.server.EventServiceServerMessageHandler;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.registry.EventServiceStore;

/**
 * Event Service Web Socket Guice Module for received/sent message handling
 *
 */
public class EventWebSocketModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(JsonMessageTypeMap.class).to(EventDefaultJsonMessageMapper.class).in(Singleton.class);

		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
				.to(EventServiceClientMessageHandlerProvider.class).in(Singleton.class);
		bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
				.to(EventServiceServerMessageHandlerProvider.class).in(Singleton.class);

		bind(JsonMessageSchemaMap.class).to(DefaultMessageSchemaMapper.class).in(Singleton.class);
	}

	static class EventServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Inject
		private EventServiceStore eventServiceStore;

		@Inject
		private BrokerNetworkActiveMQ brokerNetworkActiveMq;

		@Inject
		private JsonWebSocketClientSessionPool webSocketClient;

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new EventServiceClientMessageHandler(eventServiceStore, brokerNetworkActiveMq, webSocketClient);
		}
	}

	static class EventServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Inject
		private EventPool eventPool;

		@Inject
		private EventServiceStore eventServiceStore;

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new EventServiceServerMessageHandler(eventPool, eventServiceStore);
		}
	}
}
