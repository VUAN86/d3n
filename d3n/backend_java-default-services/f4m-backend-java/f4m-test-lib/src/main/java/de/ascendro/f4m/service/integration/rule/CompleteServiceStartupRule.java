package de.ascendro.f4m.service.integration.rule;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.rules.ErrorCollector;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.integration.test.F4MIntegrationTestBase;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;

public abstract class CompleteServiceStartupRule extends MockServiceRule {
	private final String serviceName;
	private final List<String> dependentServices;
	private final boolean register;

	public CompleteServiceStartupRule(int jettySslPort, ErrorCollector errorCollector, String serviceName,
			List<String> dependentServices, boolean register) {
		super(jettySslPort, errorCollector, null);
		this.serviceName = serviceName;
		this.dependentServices = dependentServices;
		this.register = register;
	}

	@Override
	protected void configureInjectionModule(BindableAbstractModule module) {
		//mapper does not matter as no messages are sent
		module.bindToModule(JsonMessageTypeMap.class).to(ServiceRegistryMessageTypeMapper.class);
	}

	@Override
	protected AbstractModule getMockServiceInjectionModule() {
		return new BindableAbstractModule() {
			@Override
			protected void configure() {
				bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
						.to(EmptyDefaultJsonMessageHandlerProviderImpl.class).in(Singleton.class);
				bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
						.to(CompletedServiceServerMessageHandlerProvider.class).in(Singleton.class);
				configureInjectionModule(this);
			}
		};
	}

	@Override
	protected void startup() throws Exception {
		getServerStartup().start();
	}

	@Override
	protected void stop() throws Exception {
		getServerStartup().stop();
	}

	@Override
	protected ServiceStartup getMockServiceStartup() {
		return new ServiceStartup(F4MIntegrationTestBase.DEFAULT_TEST_STAGE) {
			@Override
			public Injector createInjector(Stage stage) {
				return Guice.createInjector(stage, Modules.override(super.getModules()).with(getModules()));
			}

			@Override
			protected Iterable<? extends Module> getModules() {
				return Arrays.asList(getMockServiceInjectionModule());
			}

			@Override
			protected String getServiceName() {
				return serviceName;
			}

			@Override
			protected List<String> getDependentServiceNames() {
				return dependentServices;
			}
			
			@Override
			protected List<String> getDefaultDependentServiceNames() {
				return null;
			}

			@Override
			public void register() throws F4MException, URISyntaxException {
				if (register) {
					super.register();
				}
			}
			
			@Override
			protected void addShutdownHook() {
			}
		};
	}

	static class EmptyDefaultJsonMessageHandlerProviderImpl extends DefaultJsonMessageHandlerProviderImpl {

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new DefaultJsonMessageHandler() {};
		}

	}
	
	static class CompletedServiceServerMessageHandlerProvider extends MockServiceServerMessageHandlerProvider {
		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new CompleteServiceServerMessageHandler(onReceiveMessage, errorCollector, testableServiceStartup, mockServiceUri);
		}
	}

	static class CompleteServiceServerMessageHandler extends MockServiceServerMessageHandler {

		@Inject
		public CompleteServiceServerMessageHandler(Function<RequestContext, JsonMessageContent> onReceiveMessage,
				ErrorCollector errorCollector, ServiceStartup testableServiceStartup, URI mockServiceUri) {
			super(onReceiveMessage, errorCollector, testableServiceStartup, mockServiceUri);
		}
		
		@Override //discard mock service self auto resolve
		public JsonMessageContent onUserMessage(RequestContext context) {
			if (testableServiceStartup != null) {
				assertIfMessageDependentServicePresent(context);
			}		
			return onReceiveMessage.apply(context);				
		}		
	}
}
