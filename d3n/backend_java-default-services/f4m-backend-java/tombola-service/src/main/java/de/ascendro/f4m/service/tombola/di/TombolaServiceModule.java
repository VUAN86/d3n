package de.ascendro.f4m.service.tombola.di;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.voucher.CommonVoucherAerospikeDao;
import de.ascendro.f4m.server.voucher.CommonVoucherAerospikeDaoImpl;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDaoImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.tombola.config.TombolaConfig;
import de.ascendro.f4m.service.tombola.dao.TombolaAerospikeDao;
import de.ascendro.f4m.service.tombola.dao.TombolaAerospikeDaoImpl;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.EventServiceClientImpl;

public class TombolaServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(F4MConfigImpl.class).to(TombolaConfig.class);
        bind(TombolaConfig.class).in(Singleton.class);

        bind(AerospikeClientProvider.class).in(Singleton.class);
        bind(TombolaAerospikeDao.class).to(TombolaAerospikeDaoImpl.class).in(Singleton.class);
        bind(AerospikeDao.class).to(TombolaAerospikeDao.class).in(Singleton.class);
        bind(CommonUserWinningAerospikeDao.class).to(CommonUserWinningAerospikeDaoImpl.class).in(Singleton.class);
        bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
        bind(CommonVoucherAerospikeDao.class).to(CommonVoucherAerospikeDaoImpl.class).in(Singleton.class);
        bind(ApplicationConfigurationAerospikeDao.class).to(ApplicationConfigurationAerospikeDaoImpl.class)
                .in(javax.inject.Singleton.class);
        bind(EventServiceClient.class).to(EventServiceClientImpl.class).in(Singleton.class);

        //Analytics
        bind(Tracker.class).to(TrackerImpl.class).in(Singleton.class);
        bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(Singleton.class);
    }
}
