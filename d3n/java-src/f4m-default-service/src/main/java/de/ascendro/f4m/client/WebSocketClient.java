package de.ascendro.f4m.client;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.inject.Provider;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.ServiceEndpoint;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;

public class WebSocketClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClient.class);

	public static final String SERVICE_NAME_HEADER = "service_name";
	public static final String SERVICE_CONNECTION_INFO = "info";

	protected final Provider<? extends ServiceEndpoint<?, ?, ?>> endpointProvider;
	protected final Config config;

	private WebSocketContainer webSocketContainer;
	private ContainerWithKeystoreProvider containerProvider;
	
	public WebSocketClient(Provider<? extends ServiceEndpoint<?, ?, ?>> endpointProvider, Config config) {
		this.endpointProvider = endpointProvider;
		this.config = config;
		containerProvider = new ContainerWithKeystoreProvider(config);
	}
	
	public Session connect(ServiceConnectionInformation serviceConnectionInformation) {
		final Endpoint clientEndpointHandler = endpointProvider.get();

		final WebSocketContainer container = getWebSocketContainer();
		container.setDefaultMaxSessionIdleTimeout(config.getPropertyAsLong(F4MConfigImpl.JETTY_DEFAULT_MAX_SESSION_IDLE_TIMEOUT));

		Configurator clientEndpointConfigurator = new ClientEndpointConfig.Configurator() {
			@Override
			public void beforeRequest(Map<String, List<String>> headers) {
				//provide also service server port for easier recognition and identification?
				String serviceName = config.getProperty(F4MConfig.SERVICE_NAME);
				headers.put(SERVICE_NAME_HEADER, Arrays.asList(serviceName));
				super.beforeRequest(headers);
			}
		};
		final ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().configurator(clientEndpointConfigurator).build();
		clientEndpointConfig.getUserProperties().put(SERVICE_CONNECTION_INFO, serviceConnectionInformation);
		URI uri = URI.create(serviceConnectionInformation.getUri());

		LOGGER.debug("Connecting to {} with async send timeout {} and max session idle timeout {}", uri, 
				container.getDefaultAsyncSendTimeout(), container.getDefaultMaxSessionIdleTimeout());
		try {
			return container.connectToServer(clientEndpointHandler, clientEndpointConfig, uri);
		} catch (DeploymentException | IOException e) {
			throw new F4MIOException("Could not connect to " + uri, e);
		}
	}

	private WebSocketContainer getWebSocketContainer() {
		if (webSocketContainer == null) {
			synchronized (this) {
				if (webSocketContainer == null) {
					webSocketContainer = containerProvider.getWebSocketContainer();
				}
			}
		}
		return webSocketContainer;
	}

	@PreDestroy
	public void closeContainer() {
		if (webSocketContainer != null && webSocketContainer instanceof LifeCycle) {
			try {
				((LifeCycle)webSocketContainer).stop();
			} catch (Exception e) {
				LOGGER.error("Could not stop WebSocketContainer", e);
			}
		}
	}
}
