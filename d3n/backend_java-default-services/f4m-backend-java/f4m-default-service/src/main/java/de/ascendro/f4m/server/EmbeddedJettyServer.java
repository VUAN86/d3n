package de.ascendro.f4m.server;

import javax.inject.Inject;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.servlet.GuiceServletContextListener;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.logging.LoggedMessageAction;
import de.ascendro.f4m.service.logging.LoggedMessageType;
import de.ascendro.f4m.service.logging.LoggingUtil;

public class EmbeddedJettyServer extends Server {
	private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedJettyServer.class);

	private final ServerEndpointConfig serverEndpointConfig;
	private final F4MConfigImpl config;
	private final LoggingUtil loggingUtil;

	@Inject
	public EmbeddedJettyServer(F4MConfigImpl config, ServerEndpointConfig serverEndpointConfig, LoggingUtil loggingUtil) {
		super(new QueuedThreadPool(config.getPropertyAsInteger(F4MConfigImpl.JETTY_MAX_THREADS)));
		this.config = config;
		this.serverEndpointConfig = serverEndpointConfig;
		this.loggingUtil = loggingUtil;
	}

	public void startServer(GuiceServletContextListener guiceServletContextListener) throws Exception {
		loggingUtil.saveBasicInformationInThreadContext();
		Validate.notNull(System.getProperty("javax.net.ssl.trustStore"),
				"Must specify path to trust store to run the server");
		Validate.notNull(System.getProperty("javax.net.ssl.trustStorePassword"),
				"Must provide trust store password to run the server");

		final SslContextFactory sslContextFactory = new SslContextFactory(true);

		final String excludeChiperSuites = config.getProperty(F4MConfigImpl.JETTY_EXCLUDE_CHIPHER_SUITES);
		sslContextFactory
				.addExcludeCipherSuites(excludeChiperSuites != null ? excludeChiperSuites.split(",") : new String[0]);

		final String excludeProtocols = config.getProperty(F4MConfigImpl.JETTY_EXCLUDE_PROTCOLS);
		sslContextFactory.addExcludeProtocols(excludeProtocols != null ? excludeProtocols.split(",") : new String[0]);
		sslContextFactory.setKeyStorePath(config.getProperty(Config.KEY_STORE));
		sslContextFactory.setKeyStorePassword(config.getProperty(Config.KEY_STORE_PASSWORD));
		sslContextFactory.setNeedClientAuth(config.getPropertyAsBoolean(F4MConfigImpl.JETTY_HANDSHAKE_REQUIRE_CLIENT_AUTH));

		final SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory,
				HttpVersion.HTTP_1_1.asString());

		final HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSecurePort(config.getPropertyAsInteger(F4MConfigImpl.JETTY_SSL_PORT));
		httpConfiguration.setSecureScheme("https");
		httpConfiguration.addCustomizer(new SecureRequestCustomizer());
		final HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);
		final ServerConnector sslConnector = new ServerConnector(this, sslConnectionFactory, httpConnectionFactory);
		sslConnector.setPort(config.getPropertyAsInteger(F4MConfigImpl.JETTY_SSL_PORT));
		addConnector(sslConnector);

		final HandlerList baseHandlers = new HandlerList();
		setHandler(baseHandlers);

		// Setup the basic application "context" for this application at "/"
		// This is also known as the handler tree (in jetty speak)
		final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(config.getProperty(F4MConfigImpl.JETTY_CONTEXT_PATH));
		context.addEventListener(guiceServletContextListener);
		
		registerAdditionalHandlers(baseHandlers);

		baseHandlers.addHandler(context); //first register HTTP with narrower context path, reason in HandlerList.handle

		try {
			// Initialize javax.websocket layer
			final ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
			wscontainer.setDefaultMaxSessionIdleTimeout(config.getPropertyAsInteger(F4MConfigImpl.JETTY_DEFAULT_MAX_SESSION_IDLE_TIMEOUT));

			// Add WebSocket endpoint to javax.websocket layer
			wscontainer.addEndpoint(serverEndpointConfig);

			LOGGER.debug("Jetty begin start");
			start();
			loggingUtil.logProtocol(LoggedMessageType.SERVICE, LoggedMessageAction.CREATED, "Service started");
		} catch (Exception e) {
			LOGGER.error("Could not start Jetty server", e);
			throw e;
		}
	}

	protected void registerAdditionalHandlers(final HandlerList baseHandlers) {
		//no other handlers by default
	}

	public void stopServer() throws Exception {
		stop();
		loggingUtil.logProtocol(LoggedMessageType.SERVICE, LoggedMessageAction.DESTROYED, "Service stopped");
		LOGGER.debug("Jetty server stopped");
	}
	
	public void setThreadName(String name) {
		((QueuedThreadPool)this.getThreadPool()).setName(name);
	}

}
