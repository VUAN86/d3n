package de.ascendro.f4m.service.registry.store;

import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.exception.F4MServiceConnectionInformationNotFoundException;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.session.SessionWrapper;

public class ServiceRegistryImpl implements ServiceRegistry {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistryImpl.class);

	/** Service name -> service uri -> service data map. */
	private final Map<String, Map<String, ServiceData>> registeredServices = new ConcurrentHashMap<>();

	/** Namespace to service name map. */
	private final Map<String, String> registeredNamespaces = new ConcurrentHashMap<>();

	/** Service to namespace map. */
	private final Map<String, List<String>> registeredServiceNamespaces = new ConcurrentHashMap<>();

	private final SecureRandom balancingRandom = new SecureRandom();

	@Override
	public ServiceData register(String serviceName, String uri, List<String> serviceNamespaces,
			SessionWrapper session) {
		Validate.notBlank(serviceName, ServiceRegistryMessageTypes.REGISTER.getMessageName()
				+ ".serviceName is mandatory");
		Validate.notBlank(uri, ServiceRegistryMessageTypes.REGISTER.getMessageName() + ".uri is mandatory");
		Validate.notEmpty(serviceNamespaces, ServiceRegistryMessageTypes.REGISTER.getMessageName()
				+ ".serviceNamespaces is mandatory");

		Map<String, ServiceData> serviceMap = registeredServices.get(serviceName);
		if (serviceMap == null) {
			synchronized (registeredServices) {
				serviceMap = registeredServices.get(serviceName);
				if (serviceMap == null) {
					serviceMap = new ConcurrentHashMap<>();
					registeredServices.put(serviceName, serviceMap);
				}
			}
		}

		ServiceData data = new ServiceData(serviceName, uri, serviceNamespaces, session);
		serviceNamespaces.forEach(ns -> {
			String oldServiceName = registeredNamespaces.put(ns, serviceName);
			if (oldServiceName != null && !oldServiceName.equals(serviceName)) {
				LOGGER.error("Namespace {} registered with different service names {} and {} - this will lead to wrong message handling and needs to be corrected", ns, oldServiceName, serviceName);
			}
		});
		registeredServiceNamespaces.put(serviceName, serviceNamespaces);
		synchronized (serviceMap) {
			if (serviceMap.put(uri, data) != null) {
				LOGGER.warn("Replaced existing service data for service: {}, uri: {}, namespaces: {}", serviceName, uri, serviceNamespaces);
			} else {
				LOGGER.debug("Created new service data for service: {}, uri: {}, namespaces: {}", serviceName, uri, serviceNamespaces);
			}
		}
		return data;
	}

	@Override
	public boolean unregister(ServiceData serviceData) {
		Validate.notNull(serviceData, "Service data must be provided");
		String serviceName = serviceData.getServiceName();
		Validate.notBlank(serviceName, "Service name is mandatory");
		String uri = serviceData.getUri();
		Validate.notBlank(uri, "Service URI is mandatory");

		boolean unregistrationSuccess;
		Map<String, ServiceData> serviceMap = registeredServices.get(serviceName);
		if (serviceMap != null) {
			synchronized (serviceMap) {
				if (serviceMap.remove(uri) == null) {
					LOGGER.warn("No service '{}' for unregistering found at uri: {}", serviceName, uri);
					unregistrationSuccess = false;
				} else {
					LOGGER.debug("Service '{}' unregistered for uri: {}", serviceName, uri);
					unregistrationSuccess = true;
				}
			}
		} else {
			LOGGER.warn("Trying to unregister service '{}' which cannot be found", serviceName);
			unregistrationSuccess = false;
		}
		return unregistrationSuccess;
	}

	@Override
	public String getServiceName(String namespace) throws F4MServiceConnectionInformationNotFoundException {
		Validate.notBlank(namespace, "Namespace must be provided");
		String serviceName = registeredNamespaces.get(namespace);
		if (namespace == null) {
			throw new F4MServiceConnectionInformationNotFoundException(namespace + " namespace not found");
		} else {
			return serviceName;
		}
	}

	@Override
	public ServiceConnectionInformation getServiceConnectionInformation(String serviceName)
			throws F4MServiceConnectionInformationNotFoundException {
		Validate.notBlank(serviceName, "Service name must be provided");

		ServiceConnectionInformation serviceConnectionInformation = null;

		final Map<String, ServiceData> serviceMap = registeredServices.get(serviceName);
		if (serviceMap != null) {
			synchronized (serviceMap) {
				final String uri;
				if (!serviceMap.isEmpty()) {
					final String[] uris = serviceMap.keySet()
							.toArray(new String[serviceMap.size()]);
					final int nextServiceIndex = balancingRandom.nextInt(uris.length);
					uri = uris[nextServiceIndex];
				}else{
					uri = null;
				}
				serviceConnectionInformation = new ServiceConnectionInformation(serviceName, uri,
						registeredServiceNamespaces.get(serviceName));
			}
		}
		return serviceConnectionInformation;
	}

	@Override
	public List<ServiceConnectionInformation> list(String serviceName) {
		List<ServiceConnectionInformation> results = new LinkedList<>();
		if (StringUtils.isNotBlank(serviceName)) {
			fillServiceListResults(results, serviceName);
		} else {
			registeredServices.keySet()
					.forEach(s -> fillServiceListResults(results, s));
		}
		return results;
	}

	private void fillServiceListResults(List<ServiceConnectionInformation> results, String serviceName) {
		Map<String, ServiceData> serviceMap = registeredServices.get(serviceName);
		if (serviceMap != null) {
			synchronized (serviceMap) {
				serviceMap.entrySet()
						.forEach(entry -> results.add(new ServiceConnectionInformation(serviceName, entry.getKey(),
								registeredServiceNamespaces.get(serviceName))));
			}
		}
	}

	@Override
	public Map<String, Map<String, ServiceData>> getRegisteredServices() {
		return registeredServices;
	}

	@Override
	public List<ServiceStatistics> getServiceStatisticsList() {
		return registeredServices.values().stream()
				.flatMap(serviceInstances -> serviceInstances.values().stream())
				.map(serviceData -> serviceData.getLatestStatistics())
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
}
