package de.ascendro.f4m.service.session;

import javax.websocket.Session;

public interface SessionWrapperFactory {
	SessionWrapper create(Session session);
}
