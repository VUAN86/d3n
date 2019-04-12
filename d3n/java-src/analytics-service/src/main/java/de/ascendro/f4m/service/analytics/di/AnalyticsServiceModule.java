package de.ascendro.f4m.service.analytics.di;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

import de.ascendro.f4m.server.AerospikeClientProvider;
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
import de.ascendro.f4m.service.analytics.AnalyticsScan;
import de.ascendro.f4m.service.analytics.AnalyticsScanImpl;
import de.ascendro.f4m.service.analytics.activemq.AMQStatistics;
import de.ascendro.f4m.service.analytics.activemq.AMQStatisticsImpl;
import de.ascendro.f4m.service.analytics.activemq.IQueueBrokerManager;
import de.ascendro.f4m.service.analytics.activemq.impl.ActiveMqBrokerManager;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.dao.AnalyticsAerospikeDao;
import de.ascendro.f4m.service.analytics.dao.AnalyticsAerospikeDaoImpl;
import de.ascendro.f4m.service.analytics.logging.Slf4jTypeListener;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.analytics.notification.NotificationCommonImpl;
import de.ascendro.f4m.service.analytics.util.AnalyticServiceUtil;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class AnalyticsServiceModule extends AbstractModule {

    @Override
    protected void configure() {
    	bind(AnalyticsConfig.class).in(Singleton.class);
        bind(F4MConfigImpl.class).to(AnalyticsConfig.class);
        bind(AerospikeClientProvider.class).in(Singleton.class);
        bind(AnalyticsScan.class).to(AnalyticsScanImpl.class).in(Singleton.class);
        bind(AnalyticsAerospikeDao.class).to(AnalyticsAerospikeDaoImpl.class).in(Singleton.class);
        bind(IQueueBrokerManager.class).to(ActiveMqBrokerManager.class).in(Singleton.class);
        bind(AMQStatistics.class).to(AMQStatisticsImpl.class).in(Singleton.class);
        bind(NotificationCommon.class).to(NotificationCommonImpl.class).in(Singleton.class);

        //binds for AnalyticServiceUtil
        bind(CommonGameInstanceAerospikeDao.class).to(CommonGameInstanceAerospikeDaoImpl.class).in(Singleton.class);
        bind(ApplicationConfigurationAerospikeDao.class).to(ApplicationConfigurationAerospikeDaoImpl.class).in(Singleton.class);
        bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
        bind(AnalyticServiceUtil.class).in(Singleton.class);

        bind(Tracker.class).to(TrackerImpl.class).in(Singleton.class);
        bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(Singleton.class);

        bindListener(Matchers.any(), new Slf4jTypeListener());
    }
}
