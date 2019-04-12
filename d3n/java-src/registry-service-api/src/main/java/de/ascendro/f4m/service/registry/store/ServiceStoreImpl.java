package de.ascendro.f4m.service.registry.store;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;

public class ServiceStoreImpl extends ConcurrentHashMap<String, ServiceConnectionInformation>
		implements ServiceStore<ServiceConnectionInformation> {
	private static final long serialVersionUID = 6639038315819040323L;

	@Override
	public ServiceConnectionInformation addService(String serviceName, String uri, List<String> serviceNamespaces) {
		return addService(new ServiceConnectionInformation(serviceName, uri, serviceNamespaces));
	}

	@Override
	public ServiceConnectionInformation getService(String name) {
		return get(name);
	}

	@Override
	public ServiceConnectionInformation removeService(String name) {
		return remove(name);
	}

	@Override
	public ServiceConnectionInformation addService(ServiceConnectionInformation serviceConnectionInformation) {
		return put(serviceConnectionInformation.getServiceName(), serviceConnectionInformation);
	}

}
