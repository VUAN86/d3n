package de.ascendro.f4m.service.session;

import java.security.cert.Certificate;

import javax.inject.Inject;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.Session;

import org.eclipse.jetty.websocket.jsr356.JsrSession;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;

import com.google.inject.assistedinject.Assisted;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.pool.SessionPool;

public class JettySessionWrapper extends WebSocketSessionWrapper {
	private static final String REQUEST_SSL_SESSION_ATTRIBUTE = "org.eclipse.jetty.servlet.request.ssl_session";

	@Inject
	public JettySessionWrapper(@Assisted Session session, SessionPool sessionPool, Config config, JsonMessageUtil jsonUtil,
                               LoggingUtil loggedMessageUtil) {
		super(session, sessionPool, config, jsonUtil, loggedMessageUtil);
	}

	@Override
	public Certificate[] getLocalCertificates() {
		final JsrSession jsrSession = (JsrSession) session;
		final ServletUpgradeRequest upgradeRequest = (ServletUpgradeRequest) jsrSession.getUpgradeRequest();
		final HttpServletRequest httpServletRequest = upgradeRequest.getHttpServletRequest();
		final SSLSession sslSession = (SSLSession) httpServletRequest.getAttribute(REQUEST_SSL_SESSION_ATTRIBUTE);
		return sslSession.getLocalCertificates();
	}

	@Override
	public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
		final JsrSession jsrSession = (JsrSession) session;
		final ServletUpgradeRequest upgradeRequest = (ServletUpgradeRequest) jsrSession.getUpgradeRequest();
		final HttpServletRequest httpServletRequest = upgradeRequest.getHttpServletRequest();
		final SSLSession sslSession = (SSLSession) httpServletRequest.getAttribute(REQUEST_SSL_SESSION_ATTRIBUTE);
		return sslSession.getPeerCertificates();
	}
}
