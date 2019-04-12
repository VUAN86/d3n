package de.ascendro.f4m.service.handler;

import javax.websocket.Session;

import de.ascendro.f4m.service.session.SessionWrapper;

public interface F4MMessageHandler<E> extends javax.websocket.MessageHandler.Whole<E> {
	SessionWrapper getSessionWrapper();
	
	void setSessionWrapper(SessionWrapper sessionWrapper);

	void setSession(Session session);
	
	void destroy();
}
