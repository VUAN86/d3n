package de.ascendro.f4m.service.registry;

import java.util.Arrays;

import javax.inject.Singleton;

import org.junit.Before;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.integration.test.F4MServiceIntegrationTestBase;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;

public class ServiceRegistryStartupTestBase extends F4MServiceIntegrationTestBase {
	protected ReceivedMessageCollectorWithHeartbeat receivedMessageCollector;

	protected JsonMessageUtil jsonUtil;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		receivedMessageCollector = (ReceivedMessageCollectorWithHeartbeat) testClientReceivedMessageCollector;
		jsonUtil = clientInjector.getInstance(JsonMessageUtil.class);
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(ServiceRegistryMessageTypeMapper.class, DefaultMessageSchemaMapper.class,
				ReceivedMessageCollectorWithHeartbeatProvider.class);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new RegistryServiceStartup(DEFAULT_TEST_STAGE);
	}

	protected ServiceStartup getEventServiceStartup() {
		return new ServiceStartup(DEFAULT_TEST_STAGE) {
			@Override
			public Injector createInjector(Stage stage) {
				return Guice.createInjector(stage, Modules.override(super.getModules()).with(getModules()));
			}

			@Override
			protected Iterable<? extends Module> getModules() {
				return Arrays.asList(getEventServiceInjectionModule());
			}

			@Override
			protected String getServiceName() {
				return EventMessageTypes.SERVICE_NAME;
			}
		};
	}

	protected AbstractModule getEventServiceInjectionModule() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(JsonMessageTypeMap.class).to(EventMessageTypeMapper.class);
				bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
						.to(ReceivedMessageCollectorWithHeartbeatProvider.class).in(Singleton.class);
				bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
						.to(ReceivedMessageCollectorWithHeartbeatProvider.class).in(Singleton.class);
			};
		};
	}
}
