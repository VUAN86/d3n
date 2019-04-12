package de.ascendro.f4m.service.usermessage.di;

import javax.inject.Singleton;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.sns.AmazonSNS;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.util.ApplicationConfigrationPrimaryKeyUtil;
import de.ascendro.f4m.server.session.GlobalClientSessionDao;
import de.ascendro.f4m.server.session.GlobalClientSessionDaoImpl;
import de.ascendro.f4m.server.session.GlobalClientSessionPrimaryKeyUtil;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;
import de.ascendro.f4m.service.usermessage.dao.UserMessageAerospikeDao;
import de.ascendro.f4m.server.onesignal.OneSignalWrapper;
import de.ascendro.f4m.service.usermessage.onesignal.PushNotificationTypeMessageMapper;

public class UserMessageServiceModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(F4MConfigImpl.class).to(UserMessageConfig.class);
		bind(UserMessageConfig.class).in(Singleton.class);
		
		bind(AmazonSimpleEmailService.class).toProvider(AmazonSimpleEmailServiceProvider.class);
		bind(AmazonSNS.class).toProvider(AmazonSimpleNotificationServiceProvider.class);
		bind(AerospikeClientProvider.class).in(Singleton.class);
		bind(AerospikeDao.class).to(Key.get(new TypeLiteral<AerospikeDaoImpl<PrimaryKeyUtil<String>>>() {})).in(Singleton.class);
		bind(AerospikeOperateDao.class).to(Key.get(new TypeLiteral<AerospikeOperateDaoImpl<PrimaryKeyUtil<String>>>() {})).in(Singleton.class);
		bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
		bind(ApplicationConfigurationAerospikeDao.class).to(ApplicationConfigurationAerospikeDaoImpl.class).in(
				Singleton.class);
		bind(ApplicationConfigrationPrimaryKeyUtil.class).in(Singleton.class);
		bind(UserMessageAerospikeDao.class).in(Singleton.class);
		bind(GlobalClientSessionPrimaryKeyUtil.class).in(Singleton.class);
		bind(GlobalClientSessionDao.class).to(GlobalClientSessionDaoImpl.class).in(Singleton.class);
		bind(OneSignalWrapper.class).in(Singleton.class);
		bind(PushNotificationTypeMessageMapper.class).in(Singleton.class);
	}

}
