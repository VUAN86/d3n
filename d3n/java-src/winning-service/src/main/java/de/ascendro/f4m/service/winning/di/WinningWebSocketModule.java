package de.ascendro.f4m.service.winning.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.WebSocketModule;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.winning.WinningMessageTypeMapper;
import de.ascendro.f4m.service.winning.client.PaymentServiceCommunicator;
import de.ascendro.f4m.service.winning.client.ResultEngineCommunicator;
import de.ascendro.f4m.service.winning.client.UserMessageServiceCommunicator;
import de.ascendro.f4m.service.winning.client.VoucherServiceCommunicator;
import de.ascendro.f4m.service.winning.client.WinningServiceClientMessageHandler;
import de.ascendro.f4m.service.winning.dao.SuperPrizeAerospikeDao;
import de.ascendro.f4m.service.winning.manager.WinningComponentManager;
import de.ascendro.f4m.service.winning.manager.WinningManager;
import de.ascendro.f4m.service.winning.model.schema.WinningMessageSchemaMapper;
import de.ascendro.f4m.service.winning.server.WinningServiceServerMessageHandler;

public class WinningWebSocketModule extends WebSocketModule {
	@Override
	protected void configure() {
//		bind(WinningMessageTypeMapper.class).in(Singleton.class);
//		bind(ProfileMessageTypeMapper.class).in(Singleton.class);
//		bind(JsonMessageTypeMap.class).to(WinningDefaultMessageMapper.class).in(Singleton.class);
//		bind(JsonMessageSchemaMap.class).to(WinningMessageSchemaMapper.class).in(Singleton.class);

//		// Client
//		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
//				.to(WinningServiceClientMessageHandlerProvider.class);
//
//		// Server
//		bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
//				.to(WinningServiceServerMessageHandlerProvider.class);
	}

//	static class WinningServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
//
//		@Inject
//		private WinningComponentManager winningComponentManager;
//		@Inject
//		private PaymentServiceCommunicator paymentServiceCommunicator;
//		@Inject
//		private ResultEngineCommunicator resultEngineCommunicator;
//		@Inject
//		private TransactionLogAerospikeDao transactionLogAerospikeDao;
//		@Inject
//		private CommonUserWinningAerospikeDao userWinningAerospikeDao;
//		@Inject
//		private EventServiceClient eventServiceClient;
//		@Inject
//		private Tracker tracker;
//		@Inject
//		private WinningManager winningManager;
//		@Inject
//		private CommonProfileAerospikeDao profileDao;
//		@Inject
//		private CommonGameInstanceAerospikeDao gameInstanceDao;
//
//		@Inject
//		private UserMessageServiceCommunicator userMessageServiceCommunicator;
//		@Inject
//		private SuperPrizeAerospikeDao superPrizeAerospikeDao;
//		@Override
//		protected JsonMessageHandler createServiceMessageHandler() {
//			return new WinningServiceClientMessageHandler(winningComponentManager, paymentServiceCommunicator,
//					resultEngineCommunicator, transactionLogAerospikeDao, userWinningAerospikeDao,
//					eventServiceClient, tracker, winningManager, profileDao, gameInstanceDao, userMessageServiceCommunicator,
//                    superPrizeAerospikeDao);
//		}
//
//	}
//
//	static class WinningServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
//
//		@Inject
//		ResultEngineCommunicator resultEngineCommunicator;
//		@Inject
//		PaymentServiceCommunicator paymentServiceCommunicator;
//		@Inject
//		VoucherServiceCommunicator voucherServiceCommunicator;
//		@Inject
//		private WinningComponentManager winningComponentManager;
//		@Inject
//		private WinningManager winningManager;
//		@Inject
//		private Tracker tracker;
//		@Inject
//		private CommonProfileAerospikeDao profileDao;
//		@Inject
//		private CommonGameInstanceAerospikeDao gameInstanceDao;
//		@Inject
//		private GameAerospikeDao gameDao;
//
//		@Override
//		protected JsonMessageHandler createServiceMessageHandler() {
//			return new WinningServiceServerMessageHandler(resultEngineCommunicator, winningComponentManager,
//					winningManager, voucherServiceCommunicator, paymentServiceCommunicator, profileDao, tracker, gameInstanceDao, gameDao);
//		}
//
//	}
}
