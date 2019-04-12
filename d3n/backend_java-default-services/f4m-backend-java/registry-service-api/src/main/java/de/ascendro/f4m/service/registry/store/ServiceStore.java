package de.ascendro.f4m.service.registry.store;

import java.util.List;

import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;

public interface ServiceStore<D extends ServiceConnectionInformation> {

	D addService(String name, String uri, List<String> serviceNamespaces);
	
	D addService(ServiceConnectionInformation serviceConnectionInformation);

	D getService(String name);

	D removeService(String name);
}
