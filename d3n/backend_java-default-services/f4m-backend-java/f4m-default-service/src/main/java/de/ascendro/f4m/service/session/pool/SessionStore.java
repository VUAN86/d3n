package de.ascendro.f4m.service.session.pool;

import de.ascendro.f4m.service.exception.server.F4MSessionStoreDestroyException;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreInitException;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.session.SessionWrapper;

public interface SessionStore {

	void init() throws F4MSessionStoreInitException;

	void destroy() throws F4MSessionStoreDestroyException;

	void registerClient(String clientId);

    boolean hasClient(String clientId);

	void removeClient(String clientId);

	int getClientCount();

	void registerRequest(long sequence, RequestInfo requestStore);

	/**
	 * Remove request by sequence number
	 * @param sequence - sequence/acknowledgment of JsonMessage
	 * @return just removed RequestInfo if present
	 */
	RequestInfo popRequest(long sequence);
	
	/**
	 * Remove request by sequence number
	 * @param sequence - sequence/acknowledgment of JsonMessage
	 * @return just removed RequestInfo if present
	 */
	RequestInfo peekRequest(long sequence);
	
	SessionWrapper getSessionWrapper();

}
