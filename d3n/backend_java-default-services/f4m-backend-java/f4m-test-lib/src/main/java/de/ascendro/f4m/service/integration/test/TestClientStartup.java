package de.ascendro.f4m.service.integration.test;

import java.util.Arrays;

import javax.inject.Singleton;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.util.Modules;
import com.mycila.guice.ext.closeable.CloseableInjector;
import com.mycila.guice.ext.closeable.CloseableModule;
import com.mycila.guice.ext.jsr250.Jsr250Module;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPoolImpl;
import de.ascendro.f4m.service.session.JettySessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.session.pool.SessionPoolImpl;
import de.ascendro.f4m.service.session.pool.SessionStoreCreator;
import de.ascendro.f4m.service.session.pool.SessionStoreImpl;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.util.register.ServiceRegistryClientImpl;

public class TestClientStartup {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestClientStartup.class);
	
	public static final Stage DEFAULT_STAGE = Stage.DEVELOPMENT;
	private final Injector injector;

	public TestClientStartup(Module... modules) {
		this.injector = createInjector(DEFAULT_STAGE, modules);
	}

	protected Injector createInjector(Stage stage, Module... modules) {
		return Guice.createInjector(Modules.override(getModules()).with(modules));
	}

	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new CloseableModule(), new Jsr250Module(), new AbstractModule() {
			@Override
			protected void configure() {
				bind(SessionStoreCreator.class).toInstance(SessionStoreImpl::new);
				bind(SessionPool.class).to(SessionPoolImpl.class).in(Singleton.class);
				
				bind(ServiceRegistryClient.class).to(ServiceRegistryClientImpl.class).in(Singleton.class);
				bind(EventServiceClient.class).toInstance(Mockito.mock(EventServiceClient.class));
				
				bind(JsonWebSocketClientSessionPool.class).to(JsonWebSocketClientSessionPoolImpl.class).in(Singleton.class);
				install(new FactoryModuleBuilder().implement(SessionWrapper.class, JettySessionWrapper.class)
						.build(SessionWrapperFactory.class));
			};
		});
	}

	public Injector getInjector() {
		return injector;
	}
	
	public void closeInjector() {
		try {
			injector.getInstance(CloseableInjector.class).close();
		} catch (ConfigurationException e) {
			LOGGER.error("Could not close client injector", e);
		}
	}
}
