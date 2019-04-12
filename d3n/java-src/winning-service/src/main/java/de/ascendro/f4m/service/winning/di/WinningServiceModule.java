package de.ascendro.f4m.service.winning.di;

import javax.inject.Singleton;
import javax.inject.Inject;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.GameAerospikeDaoImpl;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDaoImpl;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.game.util.GamePrimaryKeyUtil;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDaoImpl;
import de.ascendro.f4m.server.winning.util.UserWinningPrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.handler.ClientMesageHandler;
import de.ascendro.f4m.service.handler.ServerMesageHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
//import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.random.RandomUtil;
import de.ascendro.f4m.service.util.random.RandomUtilImpl;
import de.ascendro.f4m.service.winning.WinningMessageTypeMapper;
import de.ascendro.f4m.service.winning.client.*;
import de.ascendro.f4m.service.winning.config.WinningConfig;
import de.ascendro.f4m.service.winning.dao.SuperPrizeAerospikeDao;
import de.ascendro.f4m.service.winning.dao.SuperPrizeAerospikeDaoImpl;
import de.ascendro.f4m.service.winning.dao.UserWinningComponentAerospikeDao;
import de.ascendro.f4m.service.winning.dao.UserWinningComponentAerospikeDaoImpl;
import de.ascendro.f4m.service.winning.dao.UserWinningComponentPrimaryKeyUtil;
import de.ascendro.f4m.service.winning.dao.WinningComponentAerospikeDao;
import de.ascendro.f4m.service.winning.dao.WinningComponentAerospikeDaoImpl;
import de.ascendro.f4m.service.winning.manager.InsuranceInterfaceWrapper;
import de.ascendro.f4m.service.winning.manager.WinningComponentManager;
import de.ascendro.f4m.service.winning.manager.WinningManager;
import de.ascendro.f4m.service.winning.model.schema.WinningMessageSchemaMapper;
import de.ascendro.f4m.service.winning.server.WinningServiceServerMessageHandler;

public class WinningServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(F4MConfigImpl.class).to(WinningConfig.class);
		bind(WinningConfig.class).in(Singleton.class);

		bind(AerospikeClientProvider.class).in(Singleton.class);
		bind(AerospikeDao.class).to(Key.get(new TypeLiteral<AerospikeDaoImpl<PrimaryKeyUtil<String>>>() {})).in(Singleton.class);
		bind(UserWinningComponentAerospikeDao.class).to(UserWinningComponentAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonGameInstanceAerospikeDao.class).to(CommonGameInstanceAerospikeDaoImpl.class).in(Singleton.class);
		bind(WinningComponentAerospikeDao.class).to(WinningComponentAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonUserWinningAerospikeDao.class).to(CommonUserWinningAerospikeDaoImpl.class).in(Singleton.class);
		bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
		bind(GameAerospikeDao.class).to(GameAerospikeDaoImpl.class).in(Singleton.class);

		bind(UserWinningComponentPrimaryKeyUtil.class).in(Singleton.class);
		bind(GameEnginePrimaryKeyUtil.class).in(Singleton.class);
		bind(GamePrimaryKeyUtil.class).in(Singleton.class);
		bind(UserWinningPrimaryKeyUtil.class).in(Singleton.class);

		bind(ResultEngineCommunicator.class).in(Singleton.class);
		bind(PaymentServiceCommunicator.class).in(Singleton.class);
		bind(VoucherServiceCommunicator.class).in(Singleton.class);

		bind(WinningComponentManager.class).in(Singleton.class);
		bind(WinningManager.class).in(Singleton.class);

		bind(InsuranceInterfaceWrapper.class).in(Singleton.class);
		bind(SuperPrizeAerospikeDao.class).to(SuperPrizeAerospikeDaoImpl.class).in(Singleton.class);
		bind(RandomUtil.class).to(RandomUtilImpl.class).in(Singleton.class);

		bind(WinningMessageTypeMapper.class).in(Singleton.class);
		bind(ProfileMessageTypeMapper.class).in(Singleton.class);
		bind(JsonMessageTypeMap.class).to(WinningDefaultMessageMapper.class).in(Singleton.class);
		bind(JsonMessageSchemaMap.class).to(WinningMessageSchemaMapper.class).in(Singleton.class);

		//Analytics
		bind(Tracker.class).to(TrackerImpl.class).in(com.google.inject.Singleton.class);
		bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(com.google.inject.Singleton.class);

		// Server
		bind(ServerMesageHandler.class).to(WinningServiceServerMessageHandlerProvider.class);
		// Client
		bind(ClientMesageHandler.class).to(WinningServiceClientMessageHandlerProvider.class);
	}

	static class WinningServiceClientMessageHandlerProvider implements ClientMesageHandler {

		@Inject
		private WinningComponentManager winningComponentManager;
		@Inject
		private PaymentServiceCommunicator paymentServiceCommunicator;
		@Inject
		private ResultEngineCommunicator resultEngineCommunicator;
		@Inject
		private TransactionLogAerospikeDao transactionLogAerospikeDao;
		@Inject
		private CommonUserWinningAerospikeDao userWinningAerospikeDao;
//		@Inject
//		private EventServiceClient eventServiceClient;
		@Inject
		private Tracker tracker;
		@Inject
		private WinningManager winningManager;
		@Inject
		private CommonProfileAerospikeDao profileDao;
		@Inject
		private CommonGameInstanceAerospikeDao gameInstanceDao;
		@Inject
		private Config config;
		@Inject
		private LoggingUtil loggedMessageUtil;
		@Inject
		protected JsonMessageUtil jsonMessageUtil;
		@Inject
		private UserMessageServiceCommunicator userMessageServiceCommunicator;
		@Inject
		private SuperPrizeAerospikeDao superPrizeAerospikeDao;


		public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
			WinningServiceClientMessageHandler handler = new WinningServiceClientMessageHandler(winningComponentManager, paymentServiceCommunicator,
					resultEngineCommunicator, transactionLogAerospikeDao, userWinningAerospikeDao,
					tracker, winningManager, profileDao, gameInstanceDao, userMessageServiceCommunicator,
					superPrizeAerospikeDao);
			handler.setConfig(config);
			handler.setJsonMessageUtil(jsonMessageUtil);
			handler.setLoggingUtil(loggedMessageUtil);
			return handler;
		}
	}

	static class WinningServiceServerMessageHandlerProvider implements ServerMesageHandler {
		@Inject
		ResultEngineCommunicator resultEngineCommunicator;
		@Inject
		PaymentServiceCommunicator paymentServiceCommunicator;
		@Inject
		VoucherServiceCommunicator voucherServiceCommunicator;
		@Inject
		private WinningComponentManager winningComponentManager;
		@Inject
		private WinningManager winningManager;
		@Inject
		private Tracker tracker;
		@Inject
		private CommonProfileAerospikeDao profileDao;
		@Inject
		private CommonGameInstanceAerospikeDao gameInstanceDao;
		@Inject
		private GameAerospikeDao gameDao;
		@Inject
		private Config config;
		@Inject
		private LoggingUtil loggedMessageUtil;
		@Inject
		protected JsonMessageUtil jsonMessageUtil;

		public JsonAuthenticationMessageMQHandler createServiceMessageHandler() {
			WinningServiceServerMessageHandler handler = new WinningServiceServerMessageHandler(resultEngineCommunicator, winningComponentManager,
					winningManager, voucherServiceCommunicator, paymentServiceCommunicator, profileDao, tracker, gameInstanceDao, gameDao);
			handler.setConfig(config);
			handler.setJsonMessageUtil(jsonMessageUtil);
			handler.setLoggingUtil(loggedMessageUtil);
			return handler;
		}

	}

}
