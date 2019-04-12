package de.ascendro.f4m.service.registry.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.registry.exception.F4MNoServiceRegistrySpecifiedException;
import de.ascendro.f4m.service.registry.exception.F4MServiceConnectionInformationNotFoundException;
import de.ascendro.f4m.service.registry.store.ServiceData;
import de.ascendro.f4m.service.registry.store.ServiceRegistry;

public class ServiceRegistryRegistrationHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistryRegistrationHelper.class);

	private ServiceRegistryRegistrationHelper() {
		// This class is not intended to be instantiated
	}
	
	public static void unregister(ServiceData serviceData, ServiceRegistry serviceRegistry,
			ServiceRegistryEventServiceUtil eventServiceUtil) {
		if (serviceData != null) {
			LOGGER.debug("Unregistering service name [{}], uri [{}]", serviceData.getServiceName(), serviceData.getUri());
			boolean success = serviceRegistry.unregister(serviceData);
			if (success) {
				try {
					eventServiceUtil.publishUnregisterEvent(serviceData);
				} catch (F4MServiceConnectionInformationNotFoundException | F4MNoServiceRegistrySpecifiedException | F4MIOException e) {
					LOGGER.error("Failed to publish service unregister event", e);
				}
			}
		}
	}

}
