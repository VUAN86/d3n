package de.ascendro.f4m.service.registry.server;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.store.ServiceData;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionStoreImpl;

public class ServiceRegistrySessionStore extends SessionStoreImpl {

	private ServiceData serviceConnectionData;

	public ServiceRegistrySessionStore(Config config, LoggingUtil loggingUtil, SessionWrapper sessionWrapper) {
		super(config, loggingUtil, sessionWrapper);
	}

	public ServiceData getServiceConnectionData() {
		return serviceConnectionData;
	}

	public void setServiceConnectionData(ServiceData serviceConnectionData) {
		this.serviceConnectionData = serviceConnectionData;
	}

}
