package de.ascendro.f4m.client;

import java.util.List;

import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.session.SessionWrapper;

public interface WebSocketClientSessionPool {
	SessionWrapper getSession(ServiceConnectionInformation serviceConnectionInformation);

	void sendAsyncText(ServiceConnectionInformation serviceConnectionInformation, String message);

	List<SessionWrapper> getOpenSessions();
}
