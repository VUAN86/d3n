package de.ascendro.f4m.service.advertisement.di;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import de.ascendro.f4m.server.EmbeddedJettyServer;
import de.ascendro.f4m.server.advertisement.config.AdvertisementConfig;
import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDaoImpl;
import de.ascendro.f4m.service.advertisement.server.EmbeddedJettyServerWithHttp;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class AdvertisementServiceModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(EmbeddedJettyServer.class).to(EmbeddedJettyServerWithHttp.class).in(javax.inject.Singleton.class);

            bind(F4MConfigImpl.class).to(AdvertisementConfig.class);
            bind(AdvertisementConfig.class).in(Singleton.class);
            bind(CommonUserWinningAerospikeDao.class).to(CommonUserWinningAerospikeDaoImpl.class).in(Singleton.class);


            bind(Tracker.class).to(TrackerImpl.class).in(Singleton.class);
            bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(Singleton.class);
        }
}
