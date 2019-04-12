package de.ascendro.f4m.service.promocode.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.json.handler.JsonMessageMQHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.promocode.PromocodeMessageTypeMapper;
import de.ascendro.f4m.service.promocode.model.schema.PromocodeMessageSchemaMapper;
import de.ascendro.f4m.service.promocode.server.PromocodeServiceServerMessageHandler;
import de.ascendro.f4m.service.promocode.util.PromocodeManager;
import de.ascendro.f4m.service.promocode.util.PromocodePrimaryKeyUtil;

public class PromocodeWebSocketModule extends AbstractModule {
	@Override
	protected void configure() {
//		bind(PromocodeMessageTypeMapper.class).in(Singleton.class);
//		bind(JsonMessageTypeMap.class).to(PromocodeDefaultMessageMapper.class).in(Singleton.class);
//		bind(ProfileMessageTypeMapper.class).in(Singleton.class);
//		bind(PaymentMessageTypeMapper.class).in(Singleton.class);
//		bind(PromocodePrimaryKeyUtil.class).in(Singleton.class);
//		bind(JsonMessageSchemaMap.class).to(PromocodeMessageSchemaMapper.class).in(Singleton.class);

//		// Client
//		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
//				PromocodeServiceClientMessageHandlerProvider.class);

		// Server
		// Client
//		bind(JsonMessageHandlerProvider.class).to(PromocodeServiceServerMessageHandlerProvider.class);
	}

//	static class PromocodeServiceClientMessageHandlerProvider  {
//
//		@Inject
//		private PromocodeManager promocodeManager;
//
//		@Inject
//		private TransactionLogAerospikeDao transactionLogAerospikeDao;

//		protected JsonMessageHandler createServiceMessageHandler() {
//			return new PromocodeServiceClientMessageHandler(promocodeManager, transactionLogAerospikeDao);
//		}
//	}
//
//	static class PromocodeServiceServerMessageHandlerProvider {
//		@Inject
//		private PromocodeManager promocodeManager;
//
//		protected PromocodeServiceServerMessageHandler createServiceMessageHandler() {
//			return new PromocodeServiceServerMessageHandler(promocodeManager);
//		}
//
//	}
//
//	static class PromocodeServiceServerMessageHandlerProvider extends JsonAuthenticationMessageMQHandler {
//		@Inject
//		private PromocodeManager promocodeManager;
//
//		@Inject
//		private TransactionLogAerospikeDao transactionLogAerospikeDao;
//
//		protected JsonMessageMQHandler createServiceMessageHandler() {
//			return new PromocodeServiceServerMessageHandler(promocodeManager, transactionLogAerospikeDao);
//		}
//	}
}
