package de.ascendro.f4m.service.event.di;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.activemq.BrokerNetworkActiveMQ;
import de.ascendro.f4m.service.event.activemq.EmbeddedActiveMQ;
import de.ascendro.f4m.service.event.config.EventConfig;
import de.ascendro.f4m.service.event.pool.ActiveMqEventPool;
import de.ascendro.f4m.service.event.pool.EventPool;
import de.ascendro.f4m.service.event.session.EventSessionStore;
import de.ascendro.f4m.service.registry.EventServiceStore;
import de.ascendro.f4m.service.registry.store.ServiceStore;
import de.ascendro.f4m.service.session.pool.SessionStoreCreator;

/**
 * Event Service basic Guice module
 *
 */
public class EventServiceModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(F4MConfigImpl.class).to(EventConfig.class);
		bind(EventConfig.class).in(Singleton.class);

		bind(SessionStoreCreator.class).toInstance(EventSessionStore::new);
		bind(EventPool.class).to(ActiveMqEventPool.class).in(Singleton.class);

		bind(EmbeddedActiveMQ.class).to(BrokerNetworkActiveMQ.class).in(Singleton.class);
		bind(BrokerNetworkActiveMQ.class).in(Singleton.class);

		bind(ServiceStore.class).to(EventServiceStore.class).in(Singleton.class);
		bind(EventServiceStore.class).in(Singleton.class);
	}

}
