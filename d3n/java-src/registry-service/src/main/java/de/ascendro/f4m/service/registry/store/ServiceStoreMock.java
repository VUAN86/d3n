package de.ascendro.f4m.service.registry.store;

import javax.inject.Inject;

import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;

public class ServiceStoreMock extends ServiceStoreImpl {
	private static final long serialVersionUID = -6885549237622231143L;
	private final ServiceRegistry serviceRegistry;

	@Inject
	public ServiceStoreMock(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public ServiceConnectionInformation getService(String name) {
		return serviceRegistry.getServiceConnectionInformation(name);
	}

	@Override
	public ServiceConnectionInformation addService(ServiceConnectionInformation serviceConnectionInformation) {
		throw new UnsupportedOperationException("Register is not supported, please use ServiceRegistry");
	}

	@Override
	public ServiceConnectionInformation removeService(String name) {
		throw new UnsupportedOperationException("Remove is not supported, please use ServiceRegistry");
	}
}
