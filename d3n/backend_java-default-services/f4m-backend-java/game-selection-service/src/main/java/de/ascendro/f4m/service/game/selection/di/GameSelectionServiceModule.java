package de.ascendro.f4m.service.game.selection.di;

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
import de.ascendro.f4m.server.dashboard.dao.DashboardDao;
import de.ascendro.f4m.server.dashboard.dao.DashboardDaoImpl;
import de.ascendro.f4m.server.dashboard.move.dao.MoveDashboardDao;
import de.ascendro.f4m.server.dashboard.move.dao.MoveDashboardDaoImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDaoImpl;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.GameAerospikeDaoImpl;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDaoImpl;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl;
import de.ascendro.f4m.server.multiplayer.dao.MultiplayerGameInstancePrimaryKeyUtil;
import de.ascendro.f4m.server.multiplayer.dao.PublicGameElasticDao;
import de.ascendro.f4m.server.multiplayer.dao.PublicGameElasticDaoImpl;
import de.ascendro.f4m.server.multiplayer.move.dao.MoveMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.move.dao.MoveMultiplayerGameInstanceDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.result.dao.CommonResultEngineDao;
import de.ascendro.f4m.server.result.dao.CommonResultEngineDaoImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDaoImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.game.selection.client.NotificationMessagePreparer;
import de.ascendro.f4m.service.game.selection.client.communicator.AuthServiceCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.FriendManagerCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.GameEngineCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.ProfileCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.UserMessageServiceCommunicator;
import de.ascendro.f4m.service.game.selection.config.GameSelectionConfig;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorAerospikeDao;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorAerospikeDaoImpl;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorPrimaryKeyUtil;
import de.ascendro.f4m.service.game.selection.server.GameSelector;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManager;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManagerImpl;
import de.ascendro.f4m.service.game.selection.subscription.GameStartScheduler;
import de.ascendro.f4m.service.game.selection.subscription.SubscriptionManager;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessDao;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessDaoImpl;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessService;

/**
 * Game Selection Service basic module
 * 
 */
public class GameSelectionServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(GameSelectionConfig.class).in(Singleton.class);
		bind(Config.class).to(GameSelectionConfig.class);
		bind(F4MConfigImpl.class).to(GameSelectionConfig.class);

		bind(AerospikeClientProvider.class).in(Singleton.class);
		bind(AerospikeDao.class).to(Key.get(new TypeLiteral<AerospikeDaoImpl<PrimaryKeyUtil<String>>>() {})).in(Singleton.class);
		bind(GameAerospikeDao.class).to(GameAerospikeDaoImpl.class).in(Singleton.class);
		bind(GameSelectorAerospikeDao.class).to(GameSelectorAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonUserWinningAerospikeDao.class).to(CommonUserWinningAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonMultiplayerGameInstanceDao.class).to(CommonMultiplayerGameInstanceDaoImpl.class).in(Singleton.class);
		bind(ElasticClient.class).in(Singleton.class);
		bind(PublicGameElasticDao.class).to(PublicGameElasticDaoImpl.class).in(Singleton.class);
		bind(DashboardDao.class).to(DashboardDaoImpl.class).in(Singleton.class);
		bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
		bind(CommonGameInstanceAerospikeDao.class).to(CommonGameInstanceAerospikeDaoImpl.class).in(Singleton.class);
		bind(MoveMultiplayerGameInstanceDao.class).to(MoveMultiplayerGameInstanceDaoImpl.class).in(Singleton.class);
		bind(MoveDashboardDao.class).to(MoveDashboardDaoImpl.class).in(Singleton.class);
		bind(CommonResultEngineDao.class).to(CommonResultEngineDaoImpl.class).in(Singleton.class);
		bind(UserGameAccessDao.class).to(UserGameAccessDaoImpl.class).in(Singleton.class);
		bind(UserGameAccessService.class).in(Singleton.class);
		bind(CommonBuddyElasticDao.class).to(CommonBuddyElasticDaoImpl.class).in(Singleton.class);

		bind(GameSelectorPrimaryKeyUtil.class).in(Singleton.class);
		bind(MultiplayerGameInstancePrimaryKeyUtil.class).in(Singleton.class);

		bind(GameSelector.class).in(Singleton.class);
		bind(MultiplayerGameInstanceManager.class).to(MultiplayerGameInstanceManagerImpl.class).in(Singleton.class);
		bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);

		bind(UserMessageServiceCommunicator.class).in(Singleton.class);
		bind(GameEngineCommunicator.class).in(Singleton.class);
		bind(AuthServiceCommunicator.class).in(Singleton.class);
		bind(FriendManagerCommunicator.class).in(Singleton.class);
		bind(ProfileCommunicator.class).in(Singleton.class);

		bind(SubscriptionManager.class).in(Singleton.class);
		bind(GameStartScheduler.class).in(Singleton.class);
		bind(NotificationMessagePreparer.class).in(Singleton.class);

		//Analytics
		bind(Tracker.class).to(TrackerImpl.class).in(Singleton.class);
		bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(Singleton.class);
		
	}

}
