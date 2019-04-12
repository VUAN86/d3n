package de.ascendro.f4m.service.registry.di;

import javax.inject.Singleton;

import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.ServiceModule;
import de.ascendro.f4m.service.registry.config.ServiceRegistryConfig;
import de.ascendro.f4m.service.registry.heartbeat.HeartbeatTimer;
import de.ascendro.f4m.service.registry.heartbeat.HeartbeatTimerTask;
import de.ascendro.f4m.service.registry.monitor.MonitorTimer;
import de.ascendro.f4m.service.registry.monitor.MonitorTimerTask;
import de.ascendro.f4m.service.registry.server.ServiceRegistrySessionStore;
import de.ascendro.f4m.service.registry.store.ServiceRegistry;
import de.ascendro.f4m.service.registry.store.ServiceRegistryImpl;
import de.ascendro.f4m.service.registry.store.ServiceStoreImpl;
import de.ascendro.f4m.service.registry.store.ServiceStoreMock;
import de.ascendro.f4m.service.registry.util.ServiceRegistryEventServiceUtil;
import de.ascendro.f4m.service.session.pool.SessionStoreCreator;
import de.ascendro.f4m.service.util.EventServiceClient;

public class ServiceRegistryServiceModule extends ServiceModule {
	@Override
	protected void configure() {
		bind(F4MConfigImpl.class).to(ServiceRegistryConfig.class);
		bind(ServiceRegistryConfig.class).in(Singleton.class);

		bind(SessionStoreCreator.class).toInstance(ServiceRegistrySessionStore::new);

		bind(ServiceRegistry.class).to(ServiceRegistryImpl.class).in(Singleton.class);
		bind(ServiceRegistryEventServiceUtil.class).in(Singleton.class);
		bind(EventServiceClient.class).to(ServiceRegistryEventServiceUtil.class).in(Singleton.class);

		bind(ServiceStoreImpl.class).to(ServiceStoreMock.class).in(Singleton.class);

		bind(HeartbeatTimerTask.class).in(Singleton.class);
		bind(HeartbeatTimer.class).in(Singleton.class);
		bind(MonitorTimerTask.class).in(Singleton.class);
		bind(MonitorTimer.class).in(Singleton.class);
	}

}
