package de.ascendro.f4m.service.winning.di;

import javax.inject.Singleton;

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
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.util.random.RandomUtil;
import de.ascendro.f4m.service.util.random.RandomUtilImpl;
import de.ascendro.f4m.service.winning.client.PaymentServiceCommunicator;
import de.ascendro.f4m.service.winning.client.ResultEngineCommunicator;
import de.ascendro.f4m.service.winning.client.VoucherServiceCommunicator;
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

		//Analytics
		bind(Tracker.class).to(TrackerImpl.class).in(com.google.inject.Singleton.class);
		bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(com.google.inject.Singleton.class);
	}

}
