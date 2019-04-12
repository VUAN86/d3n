package de.ascendro.f4m.service.payment.server;

import java.util.EnumSet;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.google.inject.servlet.GuiceFilter;

import de.ascendro.f4m.server.EmbeddedJettyServer;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.config.PaymentConfig;

public class EmbeddedJettyServerWithHttp extends EmbeddedJettyServer {
	private PaymentConfig paymentConfig;

	@Inject
	public EmbeddedJettyServerWithHttp(PaymentConfig config, ServerEndpointConfig serverEndpointConfig,
			LoggingUtil loggedMessageUtil) {
		super(config, serverEndpointConfig, loggedMessageUtil);
		paymentConfig = config;
	}

	@Override
	protected void registerAdditionalHandlers(final HandlerList baseHandlers) {
		final ServletContextHandler httpContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		httpContext.setContextPath(paymentConfig.getCallbackBaseContextPath());
		httpContext.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		baseHandlers.addHandler(httpContext); //first register HTTP with narrower context path, reason in HandlerList.handle
	}
}
