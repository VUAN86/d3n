package de.ascendro.f4m.service.analytics.di;

import javax.inject.Singleton;

import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;

import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDaoImpl;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.module.notification.NotificationMessageListener;
import de.ascendro.f4m.service.analytics.module.notification.processor.EmailNotificationProcessor;
import de.ascendro.f4m.service.analytics.module.notification.processor.PushNotificationProcessor;
import de.ascendro.f4m.service.analytics.module.notification.processor.base.INotificationProcessor;
import de.ascendro.f4m.service.analytics.module.notification.util.NotificationUtil;
import de.ascendro.f4m.service.tombola.dao.TombolaAerospikeDao;
import de.ascendro.f4m.service.tombola.dao.TombolaAerospikeDaoImpl;

public class NotificationModule extends BaseConsumerModule {

    @Override
    protected void configure() {
        bind(TombolaAerospikeDao.class).to(TombolaAerospikeDaoImpl.class).in(Singleton.class);
        bind(NotificationUtil.class).in(Singleton.class);
        bind(ElasticClient.class).in(Singleton.class);
        bind(CommonBuddyElasticDao.class).to(CommonBuddyElasticDaoImpl.class).in(Singleton.class);

        Multibinder<AnalyticMessageListener> messageHandlers = Multibinder.newSetBinder(binder(), AnalyticMessageListener.class);
        messageHandlers.addBinding().to(NotificationMessageListener.class).in(Scopes.SINGLETON);

        Multibinder<INotificationProcessor> notificationHandlers = Multibinder.newSetBinder(binder(), INotificationProcessor.class);
        notificationHandlers.addBinding().to(EmailNotificationProcessor.class).in(Scopes.SINGLETON);
        notificationHandlers.addBinding().to(PushNotificationProcessor.class).in(Scopes.SINGLETON);
    }
}
