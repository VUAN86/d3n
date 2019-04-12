package de.ascendro.f4m.service.util.register;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import de.ascendro.f4m.service.registry.exception.F4MNoServiceRegistrySpecifiedException;
import de.ascendro.f4m.service.registry.exception.F4MServiceConnectionInformationNotFoundException;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.registry.store.ServiceStore;

public interface ServiceRegistryClient {

	void requestServiceList(String serviceName) throws F4MNoServiceRegistrySpecifiedException;

	ServiceConnectionInformation getServiceConnectionInformation(String serviceName)
			throws F4MNoServiceRegistrySpecifiedException, F4MServiceConnectionInformationNotFoundException;

	ServiceConnectionInformation getServiceConnInfoFromStore(String serviceName);

	void refreshServiceRegistryUriQueue();

	void register();

	String getServiceRegisterTopicName(String serviceName);

	String getServiceUnregisterTopicName(String serviceName);

	void unregister() throws URISyntaxException, F4MNoServiceRegistrySpecifiedException;

	void requestServiceConnectionInformation(String serviceName) throws F4MNoServiceRegistrySpecifiedException;

	ServiceStore<? extends ServiceConnectionInformation> getServiceStore();

	void scheduledServiceInfoRequest(final String serviceName, long delayInMiliseconds);

	boolean containsServiceRegistryUri(URI serviceRegistryUri);

	void discoverDependentServices();

	void addService(ServiceConnectionInformation serviceInfo);
	
	boolean hasScheduledServiceConnInfoRequests();

	List<String> getDependentServiceNames();

	boolean pushMonitoringServiceStatistics(ServiceStatistics serviceStatistics);
	
}
