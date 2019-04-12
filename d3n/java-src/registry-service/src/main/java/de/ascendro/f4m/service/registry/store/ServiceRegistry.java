package de.ascendro.f4m.service.registry.store;

import java.util.List;
import java.util.Map;

import de.ascendro.f4m.service.registry.exception.F4MServiceConnectionInformationNotFoundException;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.session.SessionWrapper;

public interface ServiceRegistry {

	/**
	 * Register service information bound to WebSocket session
	 *
	 * @param serviceName
	 *            - Name of the service to be registered
	 * @param serviceNamespaces
	 *            - Namespaces handled by the service
	 * @param uri
	 *            - Service uri
	 * @param session
	 *            - WebSocket session
	 * @return Registered service data
	 */
	ServiceData register(String serviceName, String uri, List<String> serviceNamespaces, SessionWrapper session);

	/**
	 * Remove service information form service register
	 * 
	 * @param serviceData
	 *            - Service data for service to be unregistered
	 * @return success - true, if service was found and was unregistered, false otherwise
	 */
	boolean unregister(ServiceData serviceData);

	/**
	 * Get the name of service handling given namespace.
	 * 
	 * @param namespace
	 *            - Namespace
	 * @return Name of the service handling given namespace
	 * @throws F4MServiceConnectionInformationNotFoundException
	 *             Thrown if no service handling given namespace found.
	 */
	String getServiceName(String namespace) throws F4MServiceConnectionInformationNotFoundException;

	/**
	 * Fetches service instance for given service name. If multiple instances registered, one will be chosen randomly.
	 * 
	 * @param serviceName
	 *            - Service name
	 * @return Service connection information or null, if no instance currently registered
	 * @throws F4MServiceConnectionInformationNotFoundException
	 *             Thrown if no service with given name ever registered
	 */
	ServiceConnectionInformation getServiceConnectionInformation(String serviceName) throws F4MServiceConnectionInformationNotFoundException;

	/**
	 * List all services by name
	 * 
	 * @param request
	 * @return
	 */
	List<ServiceConnectionInformation> list(String serviceName);

	/**
	 * Fetch all registered services.
	 * 
	 * @return Map of service name -> uri -> service data
	 */
	Map<String, Map<String, ServiceData>> getRegisteredServices();

	/**
	 * List service statistics from all registered services.
	 * @return
	 */
	List<ServiceStatistics> getServiceStatisticsList();

}
