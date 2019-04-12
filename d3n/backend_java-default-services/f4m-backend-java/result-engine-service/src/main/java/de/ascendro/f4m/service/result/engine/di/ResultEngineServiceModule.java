package de.ascendro.f4m.service.result.engine.di;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.event.log.EventLogAerospikeDao;
import de.ascendro.f4m.server.event.log.EventLogAerospikeDaoImpl;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDaoImpl;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDaoImpl;
import de.ascendro.f4m.server.history.dao.CommonGameHistoryDao;
import de.ascendro.f4m.server.history.dao.CommonGameHistoryDaoImpl;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDaoImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.result.engine.client.ServiceCommunicator;
import de.ascendro.f4m.service.result.engine.client.ServiceCommunicatorImpl;
import de.ascendro.f4m.service.result.engine.config.ResultEngineConfig;
import de.ascendro.f4m.service.result.engine.dao.CompletedGameHistoryAerospikeDao;
import de.ascendro.f4m.service.result.engine.dao.CompletedGameHistoryAerospikeDaoImpl;
import de.ascendro.f4m.service.result.engine.dao.GameStatisticsAerospikeDaoImpl;
import de.ascendro.f4m.service.result.engine.dao.GameStatisticsDao;
import de.ascendro.f4m.service.result.engine.dao.MultiplayerGameResultElasticDao;
import de.ascendro.f4m.service.result.engine.dao.MultiplayerGameResultElasticDaoImpl;
import de.ascendro.f4m.service.result.engine.dao.QuestionStatisticsAerospikeDao;
import de.ascendro.f4m.service.result.engine.dao.QuestionStatisticsAerospikeDaoImpl;
import de.ascendro.f4m.service.result.engine.dao.ResultEngineAerospikeDao;
import de.ascendro.f4m.service.result.engine.dao.ResultEngineAerospikeDaoImpl;
import de.ascendro.f4m.service.result.engine.util.ResultEngineUtil;
import de.ascendro.f4m.service.result.engine.util.ResultEngineUtilImpl;
import de.ascendro.f4m.service.result.engine.util.UserInteractionHandler;
import de.ascendro.f4m.service.util.random.RandomUtil;
import de.ascendro.f4m.service.util.random.RandomUtilImpl;

public class ResultEngineServiceModule extends AbstractModule {

    @Override
    protected void configure() {
		bind(ResultEngineConfig.class).in(Singleton.class);
		bind(Config.class).to(ResultEngineConfig.class);
		bind(F4MConfigImpl.class).to(ResultEngineConfig.class);
        
        bind(AerospikeClientProvider.class).in(Singleton.class);
        bind(ResultEngineAerospikeDao.class).to(ResultEngineAerospikeDaoImpl.class).in(Singleton.class);
        bind(CompletedGameHistoryAerospikeDao.class).to(CompletedGameHistoryAerospikeDaoImpl.class).in(Singleton.class);
        bind(QuestionStatisticsAerospikeDao.class).to(QuestionStatisticsAerospikeDaoImpl.class).in(Singleton.class);
        bind(CommonGameHistoryDao.class).to(CommonGameHistoryDaoImpl.class).in(Singleton.class);
        bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
        bind(EventLogAerospikeDao.class).to(EventLogAerospikeDaoImpl.class).in(Singleton.class);
        bind(GameStatisticsDao.class).to(GameStatisticsAerospikeDaoImpl.class).in(Singleton.class);
		bind(AerospikeDao.class).to(Key.get(new TypeLiteral<AerospikeDaoImpl<PrimaryKeyUtil<String>>>() {})).in(Singleton.class);
		bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonMultiplayerGameInstanceDao.class).to(CommonMultiplayerGameInstanceDaoImpl.class).in(Singleton.class);
		bind(CommonGameInstanceAerospikeDao.class).to(CommonGameInstanceAerospikeDaoImpl.class).in(Singleton.class);
		bind(ElasticClient.class).in(Singleton.class);
		bind(CommonBuddyElasticDao.class).to(CommonBuddyElasticDaoImpl.class).in(Singleton.class);
		bind(ResultEngineUtil.class).to(ResultEngineUtilImpl.class).in(Singleton.class);
		bind(ServiceCommunicator.class).to(ServiceCommunicatorImpl.class).in(Singleton.class);
		bind(UserInteractionHandler.class).in(Singleton.class);
		bind(CommonUserWinningAerospikeDao.class).to(CommonUserWinningAerospikeDaoImpl.class).in(Singleton.class);
		bind(RandomUtil.class).to(RandomUtilImpl.class).in(Singleton.class);
		bind(MultiplayerGameResultElasticDao.class).to(MultiplayerGameResultElasticDaoImpl.class).in(Singleton.class);
        //Analytics
        bind(Tracker.class).to(TrackerImpl.class).in(Singleton.class);
        bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(Singleton.class);

    }
}
