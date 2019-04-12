package de.ascendro.f4m.service.friend.di;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileElasticDao;
import de.ascendro.f4m.server.profile.CommonProfileElasticDaoImpl;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.friend.BuddyManager;
import de.ascendro.f4m.service.friend.BuddyManagerImpl;
import de.ascendro.f4m.service.friend.client.UserMessageServiceCommunicator;
import de.ascendro.f4m.service.friend.config.FriendManagerConfig;
import de.ascendro.f4m.service.friend.dao.BuddyAerospikeDao;
import de.ascendro.f4m.service.friend.dao.BuddyAerospikeDaoImpl;
import de.ascendro.f4m.service.friend.dao.BuddyElasticDao;
import de.ascendro.f4m.service.friend.dao.BuddyElasticDaoImpl;
import de.ascendro.f4m.service.friend.dao.ContactAerospikeDao;
import de.ascendro.f4m.service.friend.dao.ContactAerospikeDaoImpl;
import de.ascendro.f4m.service.friend.dao.ContactElasticDao;
import de.ascendro.f4m.service.friend.dao.ContactElasticDaoImpl;
import de.ascendro.f4m.service.friend.dao.GroupAerospikeDAO;
import de.ascendro.f4m.service.friend.dao.GroupAerospikeDAOImpl;
import de.ascendro.f4m.service.friend.util.BuddyPrimaryKeyUtil;
import de.ascendro.f4m.service.friend.util.FriendPrimaryKeyUtil;

public class FriendManagerServiceModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(F4MConfigImpl.class).to(FriendManagerConfig.class);
		bind(FriendManagerConfig.class).in(Singleton.class);

		bind(AerospikeDao.class).to(Key.get(new TypeLiteral<AerospikeDaoImpl<PrimaryKeyUtil<String>>>() {})).in(Singleton.class);

		bind(UserMessageServiceCommunicator.class).in(Singleton.class);
		
		bind(BuddyManager.class).to(BuddyManagerImpl.class).in(Singleton.class);
		bind(GroupAerospikeDAO.class).to(GroupAerospikeDAOImpl.class).in(Singleton.class);
		bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
		bind(ContactAerospikeDao.class).to(ContactAerospikeDaoImpl.class).in(Singleton.class);
		bind(BuddyAerospikeDao.class).to(BuddyAerospikeDaoImpl.class).in(Singleton.class);
		
		bind(FriendPrimaryKeyUtil.class).in(Singleton.class);
		bind(BuddyPrimaryKeyUtil.class).in(Singleton.class);

		bind(ElasticUtil.class).in(Singleton.class);
		bind(ElasticClient.class).in(Singleton.class);
		bind(CommonProfileElasticDao.class).to(CommonProfileElasticDaoImpl.class).in(Singleton.class);
		bind(ContactElasticDao.class).to(ContactElasticDaoImpl.class).in(Singleton.class);
		bind(BuddyElasticDao.class).to(BuddyElasticDaoImpl.class).in(Singleton.class);
		bind(CommonBuddyElasticDao.class).to(CommonBuddyElasticDaoImpl.class).in(Singleton.class);

		//Analytics
		bind(Tracker.class).to(TrackerImpl.class).in(Singleton.class);
		bind(AnalyticsDao.class).to(AnalyticsDaoImpl.class).in(Singleton.class);
	}

}
