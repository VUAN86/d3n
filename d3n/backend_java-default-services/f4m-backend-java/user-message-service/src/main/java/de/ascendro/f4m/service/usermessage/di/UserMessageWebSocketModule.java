package de.ascendro.f4m.service.usermessage.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.WebSocketModule;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.usermessage.aws.AmazonServiceCoordinator;
import de.ascendro.f4m.service.usermessage.client.UserMessageServiceClientMessageHandler;
import de.ascendro.f4m.service.usermessage.direct.DirectWebsocketMessageCoordinator;
import de.ascendro.f4m.service.usermessage.model.schema.UserMessageMessageSchemaMapper;
import de.ascendro.f4m.server.onesignal.OneSignalCoordinator;
import de.ascendro.f4m.service.usermessage.onesignal.PushNotificationTypeMessageMapper;
import de.ascendro.f4m.service.usermessage.server.UserMessageServiceServerMessageHandler;
import de.ascendro.f4m.service.usermessage.translation.TranslationPlaceholderReplacer;
import de.ascendro.f4m.service.usermessage.translation.Translator;

public class UserMessageWebSocketModule extends WebSocketModule {
	@Override
	protected void configure() {
		bind(JsonMessageTypeMap.class).to(UserMessageDefaultMessageTypeMapper.class).in(Singleton.class);
		bind(JsonMessageSchemaMap.class).to(UserMessageMessageSchemaMapper.class).in(Singleton.class);

		//Client
		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
				UserMessageServiceClientMessageHandlerProvider.class);

		//Server		
		bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class).to(
				UserMessageServiceServerMessageHandlerProvider.class);
	}

	static class UserMessageServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
		@Inject
		private DirectWebsocketMessageCoordinator directWebsocketMessageCoordinator;

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new UserMessageServiceClientMessageHandler(directWebsocketMessageCoordinator);
		}
	}

	static class UserMessageServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
		@Inject
		private DirectWebsocketMessageCoordinator directMessageCoordinator;
		@Inject
		private AmazonServiceCoordinator amazonServiceCoordinator;
		@Inject
		private OneSignalCoordinator pushNotificationCoordinator;
		@Inject
		private Translator translator;
		@Inject
		private CommonProfileAerospikeDao profileAerospikeDao;
		@Inject
		private TranslationPlaceholderReplacer placeholderReplacer;
		@Inject
		private PushNotificationTypeMessageMapper pushNotificationTypeMessageMapper;

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new UserMessageServiceServerMessageHandler(directMessageCoordinator, amazonServiceCoordinator,
					pushNotificationCoordinator, translator, profileAerospikeDao, placeholderReplacer,
					pushNotificationTypeMessageMapper);
		}
	}

}
