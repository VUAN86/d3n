package de.ascendro.f4m.service.game.engine.di;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.advertisement.dao.AdvertisementDao;
import de.ascendro.f4m.server.advertisement.dao.AdvertisementDaoImpl;
import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.GameAerospikeDaoImpl;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl;
import de.ascendro.f4m.server.multiplayer.dao.PublicGameElasticDao;
import de.ascendro.f4m.server.multiplayer.dao.PublicGameElasticDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.session.GlobalClientSessionPrimaryKeyUtil;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.store.EventSubscriptionCreator;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.game.engine.advertisement.GameEngineAdvertisementManager;
import de.ascendro.f4m.service.game.engine.client.results.ResultEngineCommunicator;
import de.ascendro.f4m.service.game.engine.client.results.ResultEngineCommunicatorImpl;
import de.ascendro.f4m.service.game.engine.client.selection.GameSelectionCommunicator;
import de.ascendro.f4m.service.game.engine.client.voucher.VoucherCommunicator;
import de.ascendro.f4m.service.game.engine.client.voucher.VoucherCommunicatorImpl;
import de.ascendro.f4m.service.game.engine.client.winning.WinningCommunicator;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.dao.HealthCheckJsonArrayDao;
import de.ascendro.f4m.service.game.engine.dao.history.GameHistoryDao;
import de.ascendro.f4m.service.game.engine.dao.history.GameHistoryDaoImpl;
import de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDao;
import de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDaoImpl;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDaoImpl;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolAerospikeDao;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolAerospikeDaoImpl;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolPrimaryKeyUtil;
import de.ascendro.f4m.service.game.engine.dao.preselected.PreselectedQuestionsDao;
import de.ascendro.f4m.service.game.engine.dao.preselected.PreselectedQuestionsDaoImpl;
import de.ascendro.f4m.service.game.engine.feeder.QuestionFeeder;
import de.ascendro.f4m.service.game.engine.health.HealthCheckManager;
import de.ascendro.f4m.service.game.engine.history.ActiveGameTimer;
import de.ascendro.f4m.service.game.engine.history.ActiveGameTimerTask;
import de.ascendro.f4m.service.game.engine.history.GameHistoryManager;
import de.ascendro.f4m.service.game.engine.history.GameHistoryManagerImpl;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManagerImpl;
import de.ascendro.f4m.service.game.engine.server.GameEngine;
import de.ascendro.f4m.service.game.engine.server.GameEngineImpl;
import de.ascendro.f4m.service.game.engine.server.MessageCoordinator;
import de.ascendro.f4m.service.game.engine.server.QuestionSelector;
import de.ascendro.f4m.service.game.engine.server.subscription.EventSubscriptionManager;
import de.ascendro.f4m.service.game.engine.server.subscription.LiveTournamentEventSubscription;
import de.ascendro.f4m.service.game.engine.server.subscription.LiveTournamentEventSubscriptionStore;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessDao;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessDaoImpl;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessService;

public class GameEngineServiceModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(GameEngineConfig.class).in(Singleton.class);
		bind(Config.class).to(GameEngineConfig.class);
		bind(F4MConfigImpl.class).to(GameEngineConfig.class);

		bind(QuestionPoolPrimaryKeyUtil.class).in(Singleton.class);
		bind(GameEnginePrimaryKeyUtil.class).in(Singleton.class);
		bind(GlobalClientSessionPrimaryKeyUtil.class).in(Singleton.class);

		bind(AerospikeClientProvider.class).in(Singleton.class);
		bind(AerospikeDao.class).to(Key.get(new TypeLiteral<AerospikeDaoImpl<PrimaryKeyUtil<String>>>() {})).in(Singleton.class);
		
		bind(GameInstanceAerospikeDao.class).to(GameInstanceAerospikeDaoImpl.class).in(Singleton.class);
		bind(QuestionPoolAerospikeDao.class).to(QuestionPoolAerospikeDaoImpl.class).in(Singleton.class);
		bind(GameAerospikeDao.class).to(GameAerospikeDaoImpl.class).in(Singleton.class);
		bind(GameHistoryDao.class).to(GameHistoryDaoImpl.class).in(Singleton.class);
		bind(HealthCheckJsonArrayDao.class).in(Singleton.class);
		bind(PreselectedQuestionsDao.class).to(PreselectedQuestionsDaoImpl.class).in(Singleton.class);
		bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonMultiplayerGameInstanceDao.class).to(CommonMultiplayerGameInstanceDaoImpl.class).in(Singleton.class);
		bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);
		bind(AdvertisementDao.class).to(AdvertisementDaoImpl.class).in(Singleton.class);
		bind(ActiveGameInstanceDao.class).to(ActiveGameInstanceDaoImpl.class).in(Singleton.class);
		bind(ElasticClient.class).in(Singleton.class);
		bind(PublicGameElasticDao.class).to(PublicGameElasticDaoImpl.class).in(Singleton.class);
		
		bind(MessageCoordinator.class).in(Singleton.class);
		bind(ResultEngineCommunicator.class).to(ResultEngineCommunicatorImpl.class).in(Singleton.class);
		bind(WinningCommunicator.class).in(Singleton.class);
		bind(GameSelectionCommunicator.class).in(Singleton.class);
		bind(VoucherCommunicator.class).to(VoucherCommunicatorImpl.class).in(Singleton.class);

		bind(HealthCheckManager.class).in(Singleton.class);
		bind(GameHistoryManager.class).to(GameHistoryManagerImpl.class).in(Singleton.class);
		bind(MultiplayerGameManager.class).to(MultiplayerGameManagerImpl.class).in(Singleton.class);
		bind(GameEngineAdvertisementManager.class).in(Singleton.class);

		bind(QuestionSelector.class).in(Singleton.class);
		bind(QuestionFeeder.class).in(Singleton.class);
		bind(GameEngine.class).to(GameEngineImpl.class).in(Singleton.class);

		bind(EventSubscriptionManager.class).in(Singleton.class);
		bind(EventSubscriptionStore.class).to(LiveTournamentEventSubscriptionStore.class).in(Singleton.class);
		bind(LiveTournamentEventSubscriptionStore.class).in(Singleton.class);
		bind(EventSubscriptionCreator.class).toInstance(LiveTournamentEventSubscription::new);
		bind(UserGameAccessDao.class).to(UserGameAccessDaoImpl.class).in(Singleton.class);
		bind(UserGameAccessService.class).in(Singleton.class);
		
		//Analytics
		bind(Tracker.class).to(TrackerImpl.class).in(com.google.inject.Singleton.class);
		bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(com.google.inject.Singleton.class);
		
		//Clean up
		bind(ActiveGameTimer.class).in(Singleton.class);
		bind(ActiveGameTimerTask.class).in(Singleton.class);
	}

}
