package de.ascendro.f4m.service.voucher.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.voucher.VoucherMessageTypeMapper;
import de.ascendro.f4m.service.voucher.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.voucher.client.VoucherServiceClientMessageHandler;
import de.ascendro.f4m.service.voucher.model.schema.VoucherMessageSchemaMapper;
import de.ascendro.f4m.service.voucher.server.VoucherServiceServerMessageHandler;
import de.ascendro.f4m.server.voucher.util.VoucherPrimaryKeyUtil;
import de.ascendro.f4m.service.voucher.util.VoucherUtil;

public class VoucherWebSocketModule extends AbstractModule {
	@Override
	protected void configure() {
//		bind(VoucherMessageTypeMapper.class).in(Singleton.class);
//		bind(JsonMessageTypeMap.class).to(VoucherDefaultMessageMapper.class).in(Singleton.class);
//		bind(ProfileMessageTypeMapper.class).in(Singleton.class);
//		bind(PaymentMessageTypeMapper.class).in(Singleton.class);
//		bind(VoucherPrimaryKeyUtil.class).in(Singleton.class);
//		bind(JsonMessageSchemaMap.class).to(VoucherMessageSchemaMapper.class).in(Singleton.class);

		// Client
//		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
//				VoucherServiceClientMessageHandlerProvider.class);
//
//		// Server
//		bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class).to(
//				VoucherServiceServerMessageHandlerProvider.class);
	}

//	static class VoucherServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
//		@Inject
//		private DependencyServicesCommunicator dependencyServiceCommunicator;
//		@Inject
//		private VoucherUtil voucherUtil;
//		@Inject
//		private TransactionLogAerospikeDao transactionLogDao;
//
//    	@Inject
//    	private EventServiceClient eventServiceClient;
//
//    	@Inject
//		private Tracker tracker;

//    	@Override
//		protected JsonMessageHandler createServiceMessageHandler() {
//			return new VoucherServiceClientMessageHandler(dependencyServiceCommunicator, voucherUtil,
//					eventServiceClient, transactionLogDao, tracker);
//		}
//	}

//	static class VoucherServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
//		@Inject
//		private VoucherUtil voucherUtil;
//
//		@Inject
//		private DependencyServicesCommunicator dependencyServiceCommunicator;
//
//		@Inject
//		private Tracker tracker;
//
//		@Override
//		protected JsonMessageHandler createServiceMessageHandler() {
//			return new VoucherServiceServerMessageHandler(voucherUtil, dependencyServiceCommunicator, tracker);
//		}
//
//	}
}
