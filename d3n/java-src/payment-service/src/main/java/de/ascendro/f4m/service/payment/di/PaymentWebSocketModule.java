package de.ascendro.f4m.service.payment.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.WebSocketModule;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.payment.cache.AccountBalanceCache;
import de.ascendro.f4m.service.payment.client.AdminEmailForwarder;
import de.ascendro.f4m.service.payment.client.PaymentServiceClientMessageHandler;
import de.ascendro.f4m.service.payment.model.schema.PaymentMessageSchemaMapper;
import de.ascendro.f4m.service.payment.server.PaymentServiceServerMessageHandler;
import de.ascendro.f4m.service.util.EventServiceClient;

public class PaymentWebSocketModule extends WebSocketModule {
	@Override
	protected void configure() {


//		//Client
//		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
//				PaymentServiceClientMessageHandlerProvider.class);
//
//		//Server
//		bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class).to(
//				PaymentServiceServerMessageHandlerProvider.class);
	}

//	static class PaymentServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
//		@Inject
//		private EventServiceClient eventServiceClient;
//		@Inject
//		private PaymentManagerProvider paymentManagerProvider;
//		@Inject
//		private UserAccountManagerProvider userAccountManagerProvider;
//		@Inject
//		private AdminEmailForwarder adminEmailForwarder;

//
//		@Override
//		protected JsonMessageHandler createServiceMessageHandler() {
//			return new PaymentServiceClientMessageHandler(eventServiceClient, paymentManagerProvider.get(),
//					userAccountManagerProvider.get(), adminEmailForwarder);
//		}
//	}
	
//	static class PaymentServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
//		@Inject
//		private PaymentManagerProvider paymentManagerProvider;
//		@Inject
//		private UserPaymentManagerProvider userPaymentManagerProvider;
//		@Inject
//		private UserAccountManagerProvider userAccountManagerProvider;
//		@Inject
//		private GameManagerProvider gameManagerProvider;
//		@Inject
//		private AdminEmailForwarder adminEmailForwarder;
//		@Inject
//		private AccountBalanceCache accountBalanceCache;
//
//		@Override
//		protected JsonMessageHandler createServiceMessageHandler() {
//			return new PaymentServiceServerMessageHandler(paymentManagerProvider.get(),
//					userPaymentManagerProvider.get(), userAccountManagerProvider.get(), gameManagerProvider.get(),
//					accountBalanceCache, adminEmailForwarder);
//		}
//	}
}
