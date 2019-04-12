package de.ascendro.f4m.service.advertisement.server;

import java.util.EnumSet;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.google.inject.servlet.GuiceFilter;

import de.ascendro.f4m.server.EmbeddedJettyServer;
import de.ascendro.f4m.service.advertisement.config.AdvertisementConfig;
import de.ascendro.f4m.service.logging.LoggingUtil;

public class EmbeddedJettyServerWithHttp extends EmbeddedJettyServer {
	private AdvertisementConfig advertisementConfig;

	@Inject
	public EmbeddedJettyServerWithHttp(AdvertisementConfig config, ServerEndpointConfig serverEndpointConfig,
                                       LoggingUtil loggedMessageUtil) {
		super(config, serverEndpointConfig, loggedMessageUtil);
		advertisementConfig = config;
	}

	@Override
	protected void registerAdditionalHandlers(final HandlerList baseHandlers) {
		final ServletContextHandler httpContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		httpContext.setContextPath(advertisementConfig.getCallbackBaseContextPath());
		httpContext.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		baseHandlers.addHandler(httpContext); //first register HTTP with narrower context path, reason in HandlerList.handle
	}
}
