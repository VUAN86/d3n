package de.ascendro.f4m.service.analytics.di;

import java.sql.Connection;

import javax.inject.Singleton;

import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;

import de.ascendro.f4m.server.winning.util.UserWinningPrimaryKeyUtil;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.module.statistic.StatisticMessageListener;
import de.ascendro.f4m.service.analytics.module.statistic.StatisticWatcher;
import de.ascendro.f4m.service.analytics.module.statistic.StatisticWatcherImpl;
import de.ascendro.f4m.service.analytics.module.statistic.jdbc.DatabaseFactory;
import de.ascendro.f4m.service.analytics.module.statistic.jdbc.InjectConfig;
import de.ascendro.f4m.service.analytics.module.statistic.query.AdvertisementUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.ApplicationUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.GameUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.ProfileStatsUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.PromoCodeUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.QuestionUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.VoucherUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.winning.dao.UserWinningComponentAerospikeDao;
import de.ascendro.f4m.service.winning.dao.UserWinningComponentAerospikeDaoImpl;

public class StatisticModule extends BaseConsumerModule {

    @Override
    protected void configure() {
    	bind(AnalyticsConfig.class).in(Singleton.class);
		bind(Config.class).to(AnalyticsConfig.class);
    	bind(F4MConfigImpl.class).to(AnalyticsConfig.class);
        bind(StatisticWatcher.class).to(StatisticWatcherImpl.class);

        bind(UserWinningComponentAerospikeDao.class).to(UserWinningComponentAerospikeDaoImpl.class).in(Singleton.class);
        bind(UserWinningPrimaryKeyUtil.class).in(Singleton.class);
        bind(DatabaseFactory.class).in(Singleton.class);

        bind(Connection.class).
                annotatedWith(InjectConfig.class).
                toProvider(DatabaseFactory.class);

        Multibinder<AnalyticMessageListener> messageHandlers = Multibinder.newSetBinder(binder(), AnalyticMessageListener.class);
        messageHandlers.addBinding().to(StatisticMessageListener.class).in(Scopes.SINGLETON);

        @SuppressWarnings("rawtypes")
		MapBinder<String, ITableUpdater> queryHandlers = MapBinder.newMapBinder(binder(), String.class, ITableUpdater.class);
        queryHandlers.addBinding(ApplicationUpdater.class.getSimpleName()).to(ApplicationUpdater.class).in(Scopes.SINGLETON);
        queryHandlers.addBinding(VoucherUpdater.class.getSimpleName()).to(VoucherUpdater.class).in(Scopes.SINGLETON);
        queryHandlers.addBinding(AdvertisementUpdater.class.getSimpleName()).to(AdvertisementUpdater.class).in(Scopes.SINGLETON);
        queryHandlers.addBinding(GameUpdater.class.getSimpleName()).to(GameUpdater.class).in(Scopes.SINGLETON);
        //queryHandlers.addBinding(ProfileUpdater.class.getSimpleName()).to(ProfileUpdater.class).in(Scopes.SINGLETON);
        queryHandlers.addBinding(ProfileStatsUpdater.class.getSimpleName()).to(ProfileStatsUpdater.class).in(Scopes.SINGLETON);
        queryHandlers.addBinding(QuestionUpdater.class.getSimpleName()).to(QuestionUpdater.class).in(Scopes.SINGLETON);
        queryHandlers.addBinding(PromoCodeUpdater.class.getSimpleName()).to(PromoCodeUpdater.class).in(Scopes.SINGLETON);
    }
}
