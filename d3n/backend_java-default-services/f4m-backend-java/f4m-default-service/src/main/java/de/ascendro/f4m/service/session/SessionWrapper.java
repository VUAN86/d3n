package de.ascendro.f4m.service.session;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.cert.Certificate;
import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.session.pool.SessionStore;

public interface SessionWrapper {

	Session getSession();

	String getSessionId();

	boolean isOpen();

	Async getAsyncRemote();

	Basic getBasicRemote();

	Certificate[] getLocalCertificates();

	Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException;

	<S extends SessionStore> S getSessionStore();

	void sendAsynMessage(JsonMessage<? extends JsonMessageContent> jsonMessage) throws F4MValidationFailedException;

	void sendAsynMessage(JsonMessage<? extends JsonMessageContent> jsonMessage, RequestInfo requestInfo,
			boolean forwardClientInfo);

	void sendAsynText(String text);
	
	void sendErrorMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded, Throwable e);
	
	void sendErrorMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded, JsonMessageError error);
	
	Map<String, Object> getUserProperties();

	/**
	 * Returns within connection user properties stored service connection information.
	 * Applicable only to Web Socket Client establish connection from current service.
	 * Jetty server connections will return null
	 * @return connection information for client connection or null for server connection
	 */
	ServiceConnectionInformation getServiceConnectionInformation();
	
	URI getLocalClientSessionURI();

	Config getConfig();

	boolean isClient();

	String getSource();
	
	String getTarget();
	
	String getConnectedClientServiceName();

	InetSocketAddress getRemoteAddress();
}
