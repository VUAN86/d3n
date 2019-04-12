package de.ascendro.f4m.service.game.selection.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.request.jackpot.JackpotDataGetter;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.WebSocketModule;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypeMapper;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypeMapper;
import de.ascendro.f4m.service.game.selection.client.GameSelectionServiceClientMessageHandler;
import de.ascendro.f4m.service.game.selection.client.communicator.AuthServiceCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.FriendManagerCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.GameEngineCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.ProfileCommunicator;
import de.ascendro.f4m.service.game.selection.model.schema.GameSelectionMessageSchemaMapper;
import de.ascendro.f4m.service.game.selection.server.DashboardManager;
import de.ascendro.f4m.service.game.selection.server.GameSelectionServiceServerMessageHandler;
import de.ascendro.f4m.service.game.selection.server.GameSelector;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManager;
import de.ascendro.f4m.service.game.selection.subscription.SubscriptionManager;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypeMapper;
import de.ascendro.f4m.service.util.EventServiceClient;

/**
 * Game Selection Service Web Socket Guice Module for received/sent message
 * handling
 * 
 */
public class GameSelectionWebSocketModule extends WebSocketModule {

	@Override
	protected void configure() {
		bind(GatewayMessageTypeMapper.class).in(Singleton.class);
		bind(FriendManagerMessageTypeMapper.class).in(Singleton.class);
		bind(UserMessageMessageTypeMapper.class).in(Singleton.class);
		bind(ProfileMessageTypeMapper.class).in(Singleton.class);
		bind(EventMessageTypeMapper.class).in(Singleton.class);
		bind(GameSelectionMessageTypeMapper.class).in(Singleton.class);
		bind(JsonMessageTypeMap.class).to(GameSelectionDefaultMessageTypeMapper.class).in(Singleton.class);
		bind(JsonMessageSchemaMap.class).to(GameSelectionMessageSchemaMapper.class).in(Singleton.class);

		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
				.to(GameSelectionServiceClientMessageHandlerProvider.class);
		bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
				.to(GameSelectionServiceServerMessageHandlerProvider.class);
	}

	static class GameSelectionServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Inject
		private GameSelector gameSelector;
		@Inject
		private SubscriptionManager subscriptionManager;
		@Inject
		private MultiplayerGameInstanceManager mgiManager;
		@Inject
		private AuthServiceCommunicator authServiceCommunicator;
		@Inject
		private GameEngineCommunicator gameEngineCommunicator;
		@Inject
		private ProfileCommunicator profileCommunicator;
		@Inject
		private EventServiceClient eventServiceClient;
		@Inject
		private CommonProfileAerospikeDao profileDao;
		@Inject
		private DashboardManager dashboardManager;
		@Inject
		private FriendManagerCommunicator friendManagerCommunicator;
		@Inject
		private Tracker tracker;
		@Inject
		private JackpotDataGetter jackpotDataGetter;
		@Inject
		private JsonMessageUtil jsonUtil;

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new GameSelectionServiceClientMessageHandler(gameSelector, subscriptionManager, mgiManager,
					authServiceCommunicator, gameEngineCommunicator, profileCommunicator, eventServiceClient,
					profileDao, dashboardManager, friendManagerCommunicator, tracker, jackpotDataGetter, jsonUtil);
		}

	}

	static class GameSelectionServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Inject
		private GameSelector gameSelector;
		@Inject
		private FriendManagerCommunicator friendManagerCommunicator;
		@Inject
		private SubscriptionManager subscriptionManager;
		@Inject
		private MultiplayerGameInstanceManager mgiManager;
		@Inject
		private GameEngineCommunicator gameEngineCommunicator;
		@Inject
		private AuthServiceCommunicator authServiceCommunicator;
		@Inject
		private DashboardManager dashboardManager;
		@Inject
		private JackpotDataGetter jackpotDataGetter;
		
		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new GameSelectionServiceServerMessageHandler(gameSelector, friendManagerCommunicator,
					subscriptionManager, mgiManager, gameEngineCommunicator, authServiceCommunicator, dashboardManager,
					jackpotDataGetter);
		}

	}

}
