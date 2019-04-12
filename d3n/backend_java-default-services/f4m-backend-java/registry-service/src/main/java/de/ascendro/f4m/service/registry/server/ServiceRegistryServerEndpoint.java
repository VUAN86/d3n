package de.ascendro.f4m.service.registry.server;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.di.handler.MessageHandlerProvider;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.json.JsonServiceEndpoint;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.exception.F4MNoServiceRegistrySpecifiedException;
import de.ascendro.f4m.service.registry.exception.F4MServiceConnectionInformationNotFoundException;
import de.ascendro.f4m.service.registry.store.ServiceData;
import de.ascendro.f4m.service.registry.store.ServiceRegistry;
import de.ascendro.f4m.service.registry.util.ServiceRegistryEventServiceUtil;
import de.ascendro.f4m.service.registry.util.ServiceRegistryRegistrationHelper;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class ServiceRegistryServerEndpoint extends JsonServiceEndpoint {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistryServerEndpoint.class);

	private final ServiceRegistry serviceRegistry;

	public ServiceRegistryServerEndpoint(MessageHandlerProvider<String> jsonMessageHandlerProvider,
			SessionPool sessionPool, LoggingUtil loggedMessageUtil, SessionWrapperFactory sessionWrapperFactory,
			Config config, ServiceRegistryClient serviceRegistryClient, ServiceRegistryEventServiceUtil serviceRegistryEventServiceUtil,
			ServiceRegistry serviceRegistry) {
		super(jsonMessageHandlerProvider, sessionPool, loggedMessageUtil, sessionWrapperFactory, config,
				serviceRegistryClient, serviceRegistryEventServiceUtil);
		this.serviceRegistry = serviceRegistry;
	}
	
	@Override
	public void onClose(Session session, CloseReason closeReason) {
		try {
			LOGGER.debug("Trying to unregister disconnected service session [{}], reason [{}]", session.getId(), closeReason.getCloseCode().getCode());
			unregisterDisconnectedService(session);
		} catch (F4MServiceConnectionInformationNotFoundException | F4MNoServiceRegistrySpecifiedException | F4MIOException e) {
			LOGGER.error("Failed to completely unregister service on connection close[{}]", session.getId(), e);
		} finally {
			super.onClose(session, closeReason);
		}
	}
	
	private void unregisterDisconnectedService(Session session) {
		final ServiceRegistrySessionStore sessionStore = (ServiceRegistrySessionStore) sessionPool
				.getSession(session.getId());
		final ServiceData serviceData = sessionStore.getServiceConnectionData();		
		ServiceRegistryRegistrationHelper.unregister(serviceData, serviceRegistry, getServiceRegistryEventServiceUtil());
	}
	
	private ServiceRegistryEventServiceUtil getServiceRegistryEventServiceUtil(){
		return (ServiceRegistryEventServiceUtil) eventServiceClient;
	}

}
