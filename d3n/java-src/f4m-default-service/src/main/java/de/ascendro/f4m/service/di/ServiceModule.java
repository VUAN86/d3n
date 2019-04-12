package de.ascendro.f4m.service.di;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import de.ascendro.f4m.server.EmbeddedJettyServer;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.store.EventSubscription;
import de.ascendro.f4m.service.event.store.EventSubscriptionCreator;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.store.ServiceStore;
import de.ascendro.f4m.service.registry.store.ServiceStoreImpl;
import de.ascendro.f4m.service.session.JettySessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.session.pool.SessionPoolImpl;
import de.ascendro.f4m.service.session.pool.SessionStoreCreator;
import de.ascendro.f4m.service.session.pool.SessionStoreImpl;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.EventServiceClientImpl;
import de.ascendro.f4m.service.util.KeyStoreUtil;
import de.ascendro.f4m.service.util.ServiceUtil;
import de.ascendro.f4m.service.util.random.RandomUtil;
import de.ascendro.f4m.service.util.random.RandomUtilImpl;
import de.ascendro.f4m.service.util.register.HeartbeatMonitoringProvider;
import de.ascendro.f4m.service.util.register.MonitoringTimer;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.util.register.ServiceRegistryClientImpl;

public class ServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ServiceUtil.class).in(Singleton.class);
		bind(LoggingUtil.class).in(Singleton.class);
		bind(JsonMessageUtil.class).in(Singleton.class);
		bind(KeyStoreUtil.class).in(Singleton.class);

		bind(Config.class).to(F4MConfigImpl.class);
		bind(F4MConfig.class).to(F4MConfigImpl.class);
		bind(F4MConfigImpl.class).in(Singleton.class);

//		bind(EmbeddedJettyServer.class).in(Singleton.class);
		
		//Random util
		bind(RandomUtil.class).to(RandomUtilImpl.class).in(Singleton.class);
		
		// Session Store
		bind(SessionStoreCreator.class).toInstance(SessionStoreImpl::new);
		bind(SessionPool.class).to(SessionPoolImpl.class).in(Singleton.class);

		// Service Registry Store
		bind(ServiceStore.class).to(ServiceStoreImpl.class).in(Singleton.class);
//		bind(ServiceRegistryClient.class).to(ServiceRegistryClientImpl.class).in(Singleton.class);
//		bind(ServiceMonitoringRegister.class).in(Singleton.class);
//		bind(HeartbeatMonitoringProvider.class).in(Singleton.class);
//		bind(MonitoringTimer.class).in(Singleton.class);

		// Validation
		bind(JsonMessageValidator.class).in(Singleton.class);

		//Event
//		bind(EventServiceClient.class).to(EventServiceClientImpl.class).in(Singleton.class);
		bind(EventSubscriptionCreator.class).toInstance(EventSubscription::new);
		bind(EventSubscriptionStore.class).in(Singleton.class);

		install(new FactoryModuleBuilder().implement(SessionWrapper.class, JettySessionWrapper.class)
				.build(SessionWrapperFactory.class));
	}

}
