package de.ascendro.f4m.service.payment.di;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.EmbeddedJettyServer;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.market.CommonMarketInstanceAerospikeDao;
import de.ascendro.f4m.server.market.CommonMarketInstanceAerospikeDaoImpl;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.handler.ClientMesageHandler;
import de.ascendro.f4m.service.handler.ServerMesageHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.cache.AccountBalanceCache;
import de.ascendro.f4m.service.payment.client.AdminEmailForwarder;
import de.ascendro.f4m.service.payment.client.PaymentServiceClientMessageHandler;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.*;
import de.ascendro.f4m.service.payment.di.GameManagerProvider.GameManagerFactory;
import de.ascendro.f4m.service.payment.di.PaymentManagerProvider.PaymentManagerFactory;
import de.ascendro.f4m.service.payment.di.TenantDaoProvider.TenantDaoFactory;
import de.ascendro.f4m.service.payment.di.UserAccountManagerProvider.UserAccountManagerFactory;
import de.ascendro.f4m.service.payment.di.UserPaymentManagerProvider.UserPaymentManagerFactory;
import de.ascendro.f4m.service.payment.manager.*;
import de.ascendro.f4m.service.payment.manager.impl.*;
import de.ascendro.f4m.service.payment.model.schema.PaymentMessageSchemaMapper;
import de.ascendro.f4m.service.payment.rest.wrapper.*;
import de.ascendro.f4m.service.payment.server.EmbeddedJettyServerWithHttp;
import de.ascendro.f4m.service.payment.server.PaymentServiceServerMessageHandler;

import javax.inject.Singleton;

public class PaymentServiceModule extends AbstractModule {
	
	@Override
	protected void configure() {
		bind(F4MConfigImpl.class).to(PaymentConfig.class);
		bind(PaymentConfig.class).in(Singleton.class);
		
		bind(AnalyticsEventManager.class).in(Singleton.class);
		bind(TransactionLogCacheManager.class).in(Singleton.class);
		bind(RestClientProvider.class).in(Singleton.class);
		bind(RsaDecryptor.class).in(Singleton.class);
		install(new FactoryModuleBuilder()
				.implement(PaymentManagerImpl.class, PaymentManagerImpl.class)
				.implement(PaymentManagerMockImpl.class, PaymentManagerMockImpl.class)
				.build(PaymentManagerFactory.class));
		bind(PaymentManager.class).toProvider(PaymentManagerProvider.class).in(Singleton.class);
		install(new FactoryModuleBuilder()
				.implement(GameManagerImpl.class, GameManagerImpl.class)
				.implement(GameManagerMockImpl.class, GameManagerMockImpl.class)
				.build(GameManagerFactory.class));
		bind(GameManager.class).toProvider(GameManagerProvider.class).in(Singleton.class);
		install(new FactoryModuleBuilder()
				.implement(UserPaymentManagerImpl.class, UserPaymentManagerImpl.class)
				.implement(UserPaymentManagerMockImpl.class, UserPaymentManagerMockImpl.class)
				.build(UserPaymentManagerFactory.class));
		bind(UserAccountManager.class).toProvider(UserAccountManagerProvider.class).in(Singleton.class);
		install(new FactoryModuleBuilder()
				.implement(UserAccountManagerImpl.class, UserAccountManagerImpl.class)
				.implement(UserAccountManagerMockImpl.class, UserAccountManagerMockImpl.class)
				.build(UserAccountManagerFactory.class));
//		bind(EmbeddedJettyServer.class).to(EmbeddedJettyServerWithHttp.class).in(Singleton.class);
		bind(AerospikeDao.class).to(Key.get(new TypeLiteral<AerospikeDaoImpl<PrimaryKeyUtil<String>>>() {})).in(Singleton.class);
		bind(TenantDao.class).toProvider(TenantDaoProvider.class);
		bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
		bind(PendingIdentificationAerospikeDao.class).to(PendingIdentificationAerospikeDaoImpl.class);
		bind(AccountBalanceCache.class).in(Singleton.class);
		bind(CommonMultiplayerGameInstanceDao.class).to(CommonMultiplayerGameInstanceDaoImpl.class).in(Singleton.class);

		bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonMarketInstanceAerospikeDao.class).to(CommonMarketInstanceAerospikeDaoImpl.class).in(Singleton.class);

		install(new FactoryModuleBuilder()
				.implement(TenantAerospikeDaoImpl.class, TenantAerospikeDaoImpl.class)
				.implement(TenantFileDaoImpl.class, TenantFileDaoImpl.class)
				.build(TenantDaoFactory.class));
		bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);
		install(new FactoryModuleBuilder()
				.implement(AccountRestWrapper.class, AccountRestWrapper.class)
				.build(new TypeLiteral<RestWrapperFactory<AccountRestWrapper>>() {}));
		install(new FactoryModuleBuilder()
				.implement(CurrencyRestWrapper.class, CurrencyRestWrapper.class)
				.build(new TypeLiteral<RestWrapperFactory<CurrencyRestWrapper>>() {}));
		install(new FactoryModuleBuilder()
				.implement(ExchangeRateRestWrapper.class, ExchangeRateRestWrapper.class)
				.build(new TypeLiteral<RestWrapperFactory<ExchangeRateRestWrapper>>() {}));
		install(new FactoryModuleBuilder()
				.implement(IdentificationRestWrapper.class, IdentificationRestWrapper.class)
				.build(new TypeLiteral<RestWrapperFactory<IdentificationRestWrapper>>() {}));
		install(new FactoryModuleBuilder()
				.implement(PaymentTransactionRestWrapper.class, PaymentTransactionRestWrapper.class)
				.build(new TypeLiteral<RestWrapperFactory<PaymentTransactionRestWrapper>>() {}));
		install(new FactoryModuleBuilder()
				.implement(UserRestWrapper.class, UserRestWrapper.class)
				.build(new TypeLiteral<RestWrapperFactory<UserRestWrapper>>() {}));
		install(new FactoryModuleBuilder()
				.implement(TransactionRestWrapper.class, TransactionRestWrapper.class)
				.build(new TypeLiteral<RestWrapperFactory<TransactionRestWrapper>>() {}));
		install(new FactoryModuleBuilder()
				.implement(GameRestWrapper.class, GameRestWrapper.class)
				.build(new TypeLiteral<RestWrapperFactory<GameRestWrapper>>() {}));
		install(new FactoryModuleBuilder()
				.implement(AuthRestWrapper.class, AuthRestWrapper.class)
				.build(new TypeLiteral<RestWrapperFactory<AuthRestWrapper>>() {}));
		//Analytics
		bind(Tracker.class).to(TrackerImpl.class).in(Singleton.class);
		bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(Singleton.class);

		bind(JsonMessageTypeMap.class).to(PaymentDefaultMessageTypeMapper.class).in(Singleton.class);
		bind(JsonMessageSchemaMap.class).to(PaymentMessageSchemaMapper.class).in(Singleton.class);

		// Server
		bind(ServerMesageHandler.class).to(PaymentServiceServerMessageHandlerProvider.class);
		// Client
		bind(ClientMesageHandler.class).to(PaymentServiceClientMessageHandlerProvider.class);
	}

	static class PaymentServiceClientMessageHandlerProvider implements ClientMesageHandler {
//		@Inject
//		private EventServiceClient eventServiceClient;
		@Inject
		private PaymentManagerProvider paymentManagerProvider;
		@Inject
		private UserAccountManagerProvider userAccountManagerProvider;
		@Inject
		private AdminEmailForwarder adminEmailForwarder;
		@Inject
		private Config config;
		@Inject
		private LoggingUtil loggedMessageUtil;
		@Inject
		protected JsonMessageUtil jsonMessageUtil;

		@Override
		public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
			PaymentServiceClientMessageHandler handler = new PaymentServiceClientMessageHandler(paymentManagerProvider.get(),
					userAccountManagerProvider.get(), adminEmailForwarder);
			handler.setConfig(config);
			handler.setJsonMessageUtil(jsonMessageUtil);
			handler.setLoggingUtil(loggedMessageUtil);
			return handler;
		}
	}

	static class PaymentServiceServerMessageHandlerProvider implements ServerMesageHandler {
		@Inject
		private PaymentManagerProvider paymentManagerProvider;
		@Inject
		private UserPaymentManagerProvider userPaymentManagerProvider;
		@Inject
		private UserAccountManagerProvider userAccountManagerProvider;
		@Inject
		private GameManagerProvider gameManagerProvider;
		@Inject
		private AdminEmailForwarder adminEmailForwarder;
		@Inject
		private AccountBalanceCache accountBalanceCache;
		@Inject
		private Config config;
		@Inject
		private LoggingUtil loggedMessageUtil;
		@Inject
		protected JsonMessageUtil jsonMessageUtil;

		@Override
		public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
			PaymentServiceServerMessageHandler handler = new PaymentServiceServerMessageHandler(paymentManagerProvider.get(), userPaymentManagerProvider.get(),
				userAccountManagerProvider.get(), gameManagerProvider.get(), accountBalanceCache, adminEmailForwarder);
			handler.setConfig(config);
			handler.setJsonMessageUtil(jsonMessageUtil);
			handler.setLoggingUtil(loggedMessageUtil);
			return handler;
		}
	}
}
