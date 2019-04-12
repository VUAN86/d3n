package de.ascendro.f4m.service.di;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.server.ServerEndpointConfig;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPoolImpl;
import de.ascendro.f4m.service.ServiceEndpoint;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.auth.AuthMessageTypeMapper;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.di.client.JsonServiceClientEndpointProvider;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.di.handler.JsonParallelServiceMessageHandlerProvider;
import de.ascendro.f4m.service.di.handler.MessageHandlerProvider;
import de.ascendro.f4m.service.di.server.JsonServiceServerEndpointProvider;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.json.JsonMessageDeserializer;
import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.session.SessionWrapperFactory;

public abstract class WebSocketModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(Gson.class).toProvider(GsonProvider.class);
		bind(JsonDeserializer.class).to(JsonMessageDeserializer.class).in(Singleton.class);

		//Default type mappers from API
		bind(AuthMessageTypeMapper.class).in(Singleton.class);
		bind(GatewayMessageTypeMapper.class).in(Singleton.class);
		bind(ServiceRegistryMessageTypeMapper.class).in(Singleton.class);
		bind(EventMessageTypeMapper.class).in(Singleton.class);
		
//		// Client
//		bind(new TypeLiteral<MessageHandlerProvider<String>>(){}).annotatedWith(ClientMessageHandler.class)
//				.to(ClientJsonParallelServiceMessageHandlerProvider.class)
//				.in(Singleton.class);
//
//		// Server
//		bind(new TypeLiteral<MessageHandlerProvider<String>>(){}).annotatedWith(ServerMessageHandler.class)
//				.to(ServerJsonParallelServiceMessageHandlerProvider.class)
//				.in(Singleton.class);

//		bind(ServiceEndpoint.class).toProvider(JsonServiceClientEndpointProvider.class).in(Singleton.class);
//		bind(ServerEndpointConfig.class).toProvider(JsonServiceServerEndpointProvider.class).in(Singleton.class);

//		bind(JsonWebSocketClientSessionPool.class).to(JsonWebSocketClientSessionPoolImpl.class).in(Singleton.class);
		
		bind(JsonMessageSchemaMap.class).to(DefaultMessageSchemaMapper.class).in(Singleton.class);
	}
	
//	static class ClientJsonParallelServiceMessageHandlerProvider extends JsonParallelServiceMessageHandlerProvider {
//
//		@Inject
//		public ClientJsonParallelServiceMessageHandlerProvider(@ClientMessageHandler JsonMessageHandlerProvider jsonMessageHandlerProvider,
//				Config config, SessionWrapperFactory sessionWrapperFactory, LoggingUtil loggingUtil) {
//			super(jsonMessageHandlerProvider, config, sessionWrapperFactory, loggingUtil);
//		}
//	}
//
//	static class ServerJsonParallelServiceMessageHandlerProvider extends JsonParallelServiceMessageHandlerProvider {
//
//		@Inject
//		public ServerJsonParallelServiceMessageHandlerProvider(
//				@ServerMessageHandler JsonMessageHandlerProvider jsonMessageHandlerProvider, Config config,
//				SessionWrapperFactory sessionWrapperFactory, LoggingUtil loggingUtil) {
//			super(jsonMessageHandlerProvider, config, sessionWrapperFactory, loggingUtil);
//		}
//	}
}
