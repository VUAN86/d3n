package de.ascendro.f4m.service.registry.di;

import javax.inject.Inject;
import javax.websocket.server.ServerEndpointConfig;

import com.google.inject.Singleton;

import de.ascendro.f4m.service.ServiceEndpoint;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.di.WebSocketModule;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.di.handler.MessageHandlerProvider;
import de.ascendro.f4m.service.di.server.JsonServiceServerEndpointProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.registry.client.ServiceRegistryClientMessageHandler;
import de.ascendro.f4m.service.registry.server.ServiceRegistryServerEndpoint;
import de.ascendro.f4m.service.registry.server.ServiceRegistryServerMessageHandler;
import de.ascendro.f4m.service.registry.store.ServiceRegistry;
import de.ascendro.f4m.service.registry.util.ServiceRegistryEventServiceUtil;
import de.ascendro.f4m.service.util.register.MonitoringTimerTask;

public class ServiceRegistryWebSocketModule extends WebSocketModule {
	@Override
	protected void configure() {
		bind(JsonMessageTypeMap.class).to(ServiceRegistryDefaultMessageMapper.class);

		bind(ServerEndpointConfig.class).toProvider(ServiceRegistryServerEndpointProvider.class).in(Singleton.class);

		//Client
		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
			.to(ServiceRegistryClientMessageHandlerProvider.class).in(Singleton.class);
		//Server		
		bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
			.to(ServiceRegistryServerMessageHandlerProvider.class).in(Singleton.class);

		bind(JsonMessageSchemaMap.class).to(DefaultMessageSchemaMapper.class).in(Singleton.class);
	}

	static class ServiceRegistryServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
		@Inject
		private ServiceRegistry serviceRegistry;
		@Inject
		private ServiceRegistryEventServiceUtil serviceRegistryEventServiceUtil;
		@Inject
		private MonitoringTimerTask monitoringTimerTask;

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new ServiceRegistryServerMessageHandler(serviceRegistry, serviceRegistryEventServiceUtil,
					monitoringTimerTask);
		}
	}

	static class ServiceRegistryServerEndpointProvider extends JsonServiceServerEndpointProvider {
		
		@Inject
		protected ServiceRegistryEventServiceUtil serviceRegistryEventServiceUtil;
		
		@Inject
		protected ServiceRegistry serviceRegistry;
		
		@Inject
		public ServiceRegistryServerEndpointProvider(
				@ServerMessageHandler MessageHandlerProvider<String> messageHandlerProvider, Config config) {
			super(messageHandlerProvider, config);
		}

		@Override
		protected ServiceEndpoint<JsonMessage<? extends JsonMessageContent>, String, JsonMessageContent> getServiceEndpoint() {
			return new ServiceRegistryServerEndpoint(messageHandlerProvider, sessionPool, loggedMessageUtil,
					sessionWrapperFactory, config, serviceRegistryClient, serviceRegistryEventServiceUtil, serviceRegistry);
		}
	}

	static class ServiceRegistryClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new ServiceRegistryClientMessageHandler();
		}

	}

}
