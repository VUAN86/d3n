package de.ascendro.f4m.service.ping;

import java.net.URISyntaxException;
import java.util.Arrays;

import javax.inject.Singleton;

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
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.integration.test.F4MIntegrationTestBase;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;

public class PingPongServiceStartup extends ServiceStartup {
	private TestMessageHandlerProvider serverHandlerProvider;
	private TestMessageHandlerProvider clientHandlerProvider;
	private String serviceName;
	
	public PingPongServiceStartup(String serviceName) {
		super(F4MIntegrationTestBase.DEFAULT_TEST_STAGE);
		this.serviceName = serviceName;
	}

	@Override
	protected String getServiceName() {
		return serviceName;
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new AbstractModule() {
			@Override
			protected void configure() {
				serverHandlerProvider = new TestMessageHandlerProvider(getServiceName() + " server");
				clientHandlerProvider = new TestMessageHandlerProvider(getServiceName() + " client");

				bind(JsonMessageTypeMap.class).to(PingPongMessageTypeMapper.class).in(Singleton.class);
				bind(JsonMessageSchemaMap.class).to(PingPongMessageSchemaMapper.class).in(Singleton.class);
				bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
						.toInstance(getServerHandlerProvider());
				bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
						.toInstance(getClientHandlerProvider());
			}
		});
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}

	public TestMessageHandlerProvider getServerHandlerProvider() {
		return serverHandlerProvider;
	}

	public TestMessageHandlerProvider getClientHandlerProvider() {
		return clientHandlerProvider;
	}
	
	@Override
	public void register() throws F4MException, URISyntaxException {
		//do not register in non-existent service registry
	}
}
