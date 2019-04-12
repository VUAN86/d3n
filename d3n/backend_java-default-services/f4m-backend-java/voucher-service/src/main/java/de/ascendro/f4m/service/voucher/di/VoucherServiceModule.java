package de.ascendro.f4m.service.voucher.di;

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
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.EventServiceClientImpl;
import de.ascendro.f4m.service.voucher.config.VoucherConfig;
import de.ascendro.f4m.service.voucher.dao.VoucherAerospikeDao;
import de.ascendro.f4m.service.voucher.dao.VoucherAerospikeDaoImpl;
import de.ascendro.f4m.service.voucher.util.VoucherUtil;

public class VoucherServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(F4MConfigImpl.class).to(VoucherConfig.class);
		bind(VoucherConfig.class).in(Singleton.class);
		bind(VoucherUtil.class).in(Singleton.class);

		bind(AerospikeClientProvider.class).in(Singleton.class);
		bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
		bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);
		bind(VoucherAerospikeDao.class).to(VoucherAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonGameInstanceAerospikeDao.class).to(CommonGameInstanceAerospikeDaoImpl.class).in(Singleton.class);
		bind(ApplicationConfigurationAerospikeDao.class).to(ApplicationConfigurationAerospikeDaoImpl.class)
				.in(Singleton.class);

		bind(AerospikeDao.class).to(VoucherAerospikeDao.class).in(Singleton.class);
		bind(EventServiceClient.class).to(EventServiceClientImpl.class).in(Singleton.class);

		//Analytics
		bind(Tracker.class).to(TrackerImpl.class).in(com.google.inject.Singleton.class);
		bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(com.google.inject.Singleton.class);
	}

}
