package de.ascendro.f4m.service.integration.rule;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.rules.ErrorCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollectorProvider;
import de.ascendro.f4m.service.integration.test.F4MIntegrationTestBase;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetResponse;

public abstract class MockServiceRule extends JettyServerRule {
	private static final Logger LOGGER = LoggerFactory.getLogger(MockServiceRule.class);
	/**
	 * Services which are not discovered via service registry, but contacted directly 
	 */
	private static final String[] NOT_DISCOVERABLE_SERVICES = new String[]{GatewayMessageTypes.SERVICE_NAME, ServiceRegistryMessageTypes.SERVICE_NAME};

	private final ErrorCollector errorCollector;
	private final ServiceStartup testableServiceStartup;
	protected final URI uri;

	public MockServiceRule(int jettySslPort, ErrorCollector errorCollector, ServiceStartup testableServiceStartup) {
		super(jettySslPort);
		this.uri = URI.create("wss://localhost:" + jettySslPort);
		this.errorCollector = errorCollector;
		this.testableServiceStartup = testableServiceStartup;
	}

	@Override
	protected void before() throws Throwable {
		initServerStartup();
		
		//Set handler and error collector before Service started
		final MockServiceServerMessageHandlerProvider mockServiceServerHandlerProvider = (MockServiceServerMessageHandlerProvider) this
				.getServerStartup().getInjector()
				.getInstance(Key.get(JsonMessageHandlerProvider.class, ServerMessageHandler.class));
		mockServiceServerHandlerProvider.setMessageHandler(getMessageHandler(), uri);
		mockServiceServerHandlerProvider.setErrorCollector(errorCollector);
		mockServiceServerHandlerProvider.setTestableServiceStartup(testableServiceStartup);
		
		super.before();
	}
	
	public URI getUri() {
		return uri;
	}
	
	@Override
	protected void startup() throws Exception {
		getServerStartup().startupJetty();
	}

	protected void initServerStartup() {
		setServerStartup(getMockServiceStartup());
	}

	protected abstract ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandler();

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
				return "SERVICE-MOCK";
			}
		};
	}
	
	protected AbstractModule getMockServiceInjectionModule() {
		return new BindableAbstractModule();
	}

	/**
	 * Inject necessary classes like JsonMessageTypeMap implementation.
	 * 
	 * @param module
	 */
	protected abstract void configureInjectionModule(BindableAbstractModule module);

	public class BindableAbstractModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
					.to(ReceivedMessageCollectorProvider.class).in(Singleton.class);
			bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
					.to(MockServiceServerMessageHandlerProvider.class).in(Singleton.class);
			configureInjectionModule(this);
		};

		public <T> AnnotatedBindingBuilder<T> bindToModule(Class<T> clazz) {
			return this.bind(clazz);
		}

	}

	static class MockServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		protected Function<RequestContext, JsonMessageContent> onReceiveMessage;
		
		protected ErrorCollector errorCollector;
		protected ServiceStartup testableServiceStartup;
		protected URI mockServiceUri;
		

		public void setMessageHandler(
				ThrowingFunction<RequestContext, JsonMessageContent> onReceiveMessage, URI mockServiceUri) {
			this.onReceiveMessage = onReceiveMessage;
			this.mockServiceUri = mockServiceUri;
		}

		public void setErrorCollector(ErrorCollector errorCollector) {
			this.errorCollector = errorCollector;
		}
		
		public void setTestableServiceStartup(ServiceStartup testableServiceStartup) {
			this.testableServiceStartup = testableServiceStartup;
		}

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new MockServiceServerMessageHandler(onReceiveMessage, errorCollector, testableServiceStartup, mockServiceUri);
		}

	}

	static class MockServiceServerMessageHandler extends DefaultJsonMessageHandler {

		protected Function<RequestContext, JsonMessageContent> onReceiveMessage;
		
		private final ErrorCollector errorCollector;
		protected final ServiceStartup testableServiceStartup;
		private final URI mockServiceUri;

		public MockServiceServerMessageHandler(Function<RequestContext, JsonMessageContent> onReceiveMessage,
				ErrorCollector errorCollector, ServiceStartup testableServiceStartup, URI mockServiceUri) {
			this.onReceiveMessage = onReceiveMessage;
			this.errorCollector = errorCollector;
			this.testableServiceStartup = testableServiceStartup;
			this.mockServiceUri = mockServiceUri;
		}
		
		@Override
		public void handleUserErrorMessage(RequestContext requestContext) {
			Object errorInfo;
			if (requestContext != null) {
				JsonMessage<? extends JsonMessageContent> originalMessageDecoded = requestContext.getMessage();
				errorInfo = originalMessageDecoded != null ? (originalMessageDecoded.getContent() != null
						? originalMessageDecoded.getContent() : originalMessageDecoded.getError())
						: "Empty originalMessageDecoded";
			} else {
				errorInfo = "No request context";
			}
			LOGGER.error("Mock service received unexpected user error {} in request {}", errorInfo, requestContext);
			errorCollector.addError(new F4MValidationFailedException("Unexpected error message " + errorInfo));
		}
		
		@Override
		public void onFailure(String originalMessageEncoded,
				RequestContext requestContext, Throwable e) {
			LOGGER.error("Mock service received unexpected error {}, {}", originalMessageEncoded, requestContext, e);
			errorCollector.addError(e);
			super.onFailure(originalMessageEncoded, requestContext, e);
		}

		@Override
		public JsonMessageContent onUserMessage(RequestContext context) {
			if (testableServiceStartup != null) {
				assertIfMessageDependentServicePresent(context);
			}
			final JsonMessage<? extends JsonMessageContent> message = context.getMessage();
			final ServiceRegistryMessageTypes serviceRegistryMessageType = message
					.getType(ServiceRegistryMessageTypes.class);
			if (serviceRegistryMessageType == ServiceRegistryMessageTypes.REGISTER) {
				return new EmptyJsonMessageContent();
			} else if (serviceRegistryMessageType == ServiceRegistryMessageTypes.GET) {
				final ServiceRegistryGetRequest serviceRegistryGetRequest = (ServiceRegistryGetRequest) message
						.getContent();
				final String serviceName = serviceRegistryGetRequest.getServiceName();
				final ServiceConnectionInformation serviceInfo = new ServiceConnectionInformation(serviceName,
						mockServiceUri.toString(), Arrays.asList(serviceName));
				return new ServiceRegistryGetResponse(serviceInfo);
			} else {
				return onReceiveMessage.apply(context);
			}		
		}

		protected void assertIfMessageDependentServicePresent(RequestContext context) {
			JsonMessage<? extends JsonMessageContent> originalMessageDecoded = context.getMessage();
			if (originalMessageDecoded != null && !StringUtils.isEmpty(originalMessageDecoded.getName())){
				final List<String> dependentServices = testableServiceStartup.initDependentServices();
				Config serviceConfig = testableServiceStartup.getInjector().getInstance(Config.class);
				dependentServices.add(serviceConfig.getProperty(F4MConfig.SERVICE_NAME));
				final String serviceName = originalMessageDecoded.getTypeName().split("/")[0];
				if(!StringUtils.isEmpty(serviceName) && ArrayUtils.indexOf(NOT_DISCOVERABLE_SERVICES, serviceName) < 0){
					assertThat(dependentServices, hasItems(serviceName));
				}
			}			
		}
	}

	public interface ThrowingFunction<T, R> extends Function<T, R> {
		@Override
		default R apply(T t) {
			try {
				return applyThrowing(t);
			} catch (RuntimeException e) {
				LOGGER.error("Could not process message to return mocked response", e);
				throw e;
			} catch (Exception | AssertionError e) {
				LOGGER.error("Could not process message to return mocked response", e);
				throw new RuntimeException(e);
			}
		}

		R applyThrowing(T t) throws Exception;
	}

}
