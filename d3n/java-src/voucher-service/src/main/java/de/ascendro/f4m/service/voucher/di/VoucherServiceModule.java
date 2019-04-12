package de.ascendro.f4m.service.voucher.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.server.voucher.util.VoucherPrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.handler.ClientMesageHandler;
import de.ascendro.f4m.service.handler.ServerMesageHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.EventServiceClientImpl;
import de.ascendro.f4m.service.voucher.VoucherMessageTypeMapper;
import de.ascendro.f4m.service.voucher.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.voucher.client.VoucherServiceClientMessageHandler;
import de.ascendro.f4m.service.voucher.config.VoucherConfig;
import de.ascendro.f4m.service.voucher.dao.VoucherAerospikeDao;
import de.ascendro.f4m.service.voucher.dao.VoucherAerospikeDaoImpl;
import de.ascendro.f4m.service.voucher.model.schema.VoucherMessageSchemaMapper;
import de.ascendro.f4m.service.voucher.server.VoucherServiceServerMessageHandler;
import de.ascendro.f4m.service.voucher.util.VoucherUtil;

public class VoucherServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(F4MConfigImpl.class).to(VoucherConfig.class);
		bind(VoucherConfig.class).in(Singleton.class);

		bind(AerospikeClientProvider.class).in(Singleton.class);
		bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
		bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);
		bind(VoucherAerospikeDao.class).to(VoucherAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonGameInstanceAerospikeDao.class).to(CommonGameInstanceAerospikeDaoImpl.class).in(Singleton.class);
		bind(ApplicationConfigurationAerospikeDao.class).to(ApplicationConfigurationAerospikeDaoImpl.class)
				.in(Singleton.class);

		bind(AerospikeDao.class).to(VoucherAerospikeDao.class).in(Singleton.class);
//		bind(EventServiceClient.class).to(EventServiceClientImpl.class).in(Singleton.class);

		bind(VoucherMessageTypeMapper.class).in(Singleton.class);
		bind(JsonMessageTypeMap.class).to(VoucherDefaultMessageMapper.class).in(Singleton.class);
		bind(ProfileMessageTypeMapper.class).in(Singleton.class);
		bind(PaymentMessageTypeMapper.class).in(Singleton.class);
		bind(VoucherPrimaryKeyUtil.class).in(Singleton.class);
		bind(JsonMessageSchemaMap.class).to(VoucherMessageSchemaMapper.class).in(Singleton.class);

		//Analytics
		bind(Tracker.class).to(TrackerImpl.class).in(com.google.inject.Singleton.class);
		bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(com.google.inject.Singleton.class);

		// Server
		bind(ServerMesageHandler.class).to(VoucherServiceServerMessageHandlerProvider.class);
		// Client
		bind(ClientMesageHandler.class).to(VoucherServiceClientMessageHandlerProvider.class);
	}

	static class VoucherServiceClientMessageHandlerProvider implements ClientMesageHandler {

		@Inject
		private DependencyServicesCommunicator dependencyServiceCommunicator;
		@Inject
		private VoucherUtil voucherUtil;
		@Inject
		private TransactionLogAerospikeDao transactionLogDao;
//		@Inject
//		private EventServiceClient eventServiceClient;
		@Inject
		private Tracker tracker;
		@Inject
		private Config config;
		@Inject
		private LoggingUtil loggedMessageUtil;
		@Inject
		protected JsonMessageUtil jsonMessageUtil;

		@Override
		public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
			VoucherServiceClientMessageHandler handler = new VoucherServiceClientMessageHandler(dependencyServiceCommunicator, voucherUtil,
					transactionLogDao, tracker);
			handler.setConfig(config);
			handler.setJsonMessageUtil(jsonMessageUtil);
			handler.setLoggingUtil(loggedMessageUtil);
			return handler;
		}
	}

	static class VoucherServiceServerMessageHandlerProvider implements ServerMesageHandler {
		@Inject
		private VoucherUtil voucherUtil;
		@Inject
		private DependencyServicesCommunicator dependencyServiceCommunicator;
		@Inject
		private Tracker tracker;
		@Inject
		private Config config;
		@Inject
		private LoggingUtil loggedMessageUtil;
		@Inject
		protected JsonMessageUtil jsonMessageUtil;
		@Override
		public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
			VoucherServiceServerMessageHandler handler = new VoucherServiceServerMessageHandler(voucherUtil, dependencyServiceCommunicator, tracker);
			handler.setConfig(config);
			handler.setJsonMessageUtil(jsonMessageUtil);
			handler.setLoggingUtil(loggedMessageUtil);
			return handler;
		}

	}
}
