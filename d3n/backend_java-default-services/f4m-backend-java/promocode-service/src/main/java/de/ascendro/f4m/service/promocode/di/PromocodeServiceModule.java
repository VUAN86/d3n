package de.ascendro.f4m.service.promocode.di;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.promocode.config.PromocodeConfig;
import de.ascendro.f4m.service.promocode.dao.PromocodeAerospikeDao;
import de.ascendro.f4m.service.promocode.dao.PromocodeAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;

public class PromocodeServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(F4MConfigImpl.class).to(PromocodeConfig.class);
		bind(PromocodeConfig.class).in(Singleton.class);

		bind(AerospikeClientProvider.class).in(Singleton.class);
		bind(PromocodeAerospikeDao.class).to(PromocodeAerospikeDaoImpl.class).in(Singleton.class);
		bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);

		bind(AerospikeDao.class).to(PromocodeAerospikeDao.class).in(Singleton.class);
		bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);


		//Analytics
		bind(Tracker.class).to(TrackerImpl.class).in(Singleton.class);
		bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(Singleton.class);
	}

}
