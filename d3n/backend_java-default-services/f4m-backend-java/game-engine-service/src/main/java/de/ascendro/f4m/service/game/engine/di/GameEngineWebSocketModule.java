package de.ascendro.f4m.service.game.engine.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.WebSocketModule;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.game.engine.client.GameEngineServiceClientMessageHandler;
import de.ascendro.f4m.service.game.engine.client.selection.GameSelectionCommunicator;
import de.ascendro.f4m.service.game.engine.client.winning.WinningCommunicator;
import de.ascendro.f4m.service.game.engine.health.HealthCheckManager;
import de.ascendro.f4m.service.game.engine.joker.JokerRequestHandler;
import de.ascendro.f4m.service.game.engine.model.schema.GameEngineMessageSchemaMapper;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.game.engine.server.GameEngine;
import de.ascendro.f4m.service.game.engine.server.GameEngineServiceServerMessageHandler;
import de.ascendro.f4m.service.game.engine.server.MessageCoordinator;
import de.ascendro.f4m.service.game.engine.server.subscription.EventSubscriptionManager;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessService;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypeMapper;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.ServiceUtil;
import de.ascendro.f4m.service.voucher.VoucherMessageTypeMapper;
import de.ascendro.f4m.service.winning.WinningMessageTypeMapper;

public class GameEngineWebSocketModule extends WebSocketModule {

	@Override
	protected void configure() {
		bind(EventMessageTypeMapper.class).in(Singleton.class);
		bind(ResultEngineMessageTypeMapper.class).in(Singleton.class);
		bind(PaymentMessageTypeMapper.class).in(Singleton.class);
		bind(WinningMessageTypeMapper.class).in(Singleton.class);
		bind(VoucherMessageTypeMapper.class).in(Singleton.class);

		bind(JsonMessageTypeMap.class).to(GameEngineDefaultMessageTypeMapper.class).in(Singleton.class);
 
		bind(JsonMessageSchemaMap.class).to(GameEngineMessageSchemaMapper.class).in(Singleton.class);

		// Client
		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
				.to(GameEngineServiceClientMessageHandlerProvider.class);

		// Server
		bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
				.to(GameEngineServiceServerMessageHandlerProvider.class);
	}

	static class GameEngineServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Inject
		private GameEngine gameEngine;
		
		@Inject
		private JokerRequestHandler jokerRequestHandler;
		
		@Inject
		private TransactionLogAerospikeDao transactionLogAerospikeDao;

		@Inject
		private WinningCommunicator winningCommunicator;
		
		@Inject
		private EventSubscriptionManager eventSubscriptionManager;

		@Inject
		private MessageCoordinator messageCoordinator;
		
		@Inject
		private GameSelectionCommunicator gameSelectionCommunicator;

		@Inject
		private Tracker tracker;
		
		@Inject
		private MultiplayerGameManager multiplayerGameManager;

		@Inject 
		private ServiceUtil serviceUtil;

		@Inject
		private JsonMessageUtil jsonUtil;

		@Inject
		EventServiceClient eventServiceClient;

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new GameEngineServiceClientMessageHandler(gameEngine,
															 jokerRequestHandler,
															 transactionLogAerospikeDao,
															 winningCommunicator,
															 eventSubscriptionManager,
															 messageCoordinator,
															 tracker,
															 gameSelectionCommunicator,
															 multiplayerGameManager,
															 serviceUtil,
															 jsonUtil,
															 eventServiceClient);
		}
	}

	static class GameEngineServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
		@Inject
		private GameEngine gameEngine;		
		@Inject
		private JokerRequestHandler jokerRequestHandler;
		@Inject
		private HealthCheckManager healthCheckDao;
		@Inject
		private EventSubscriptionManager subscriptionManager;
		@Inject
		private ServiceUtil serviceUtil;
		@Inject
		private CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
		@Inject
		private MessageCoordinator messageCoordinator;
		@Inject
		private UserGameAccessService userGameAccessService;
		
		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new GameEngineServiceServerMessageHandler(gameEngine, jokerRequestHandler, healthCheckDao,
					serviceUtil, commonMultiplayerGameInstanceDao, subscriptionManager,
					messageCoordinator, userGameAccessService);
		}
	}
}
