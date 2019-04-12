package de.ascendro.f4m.service.util.register;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.exception.F4MNoServiceRegistrySpecifiedException;
import de.ascendro.f4m.service.registry.exception.F4MServiceConnectionInformationNotFoundException;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryListRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryRegisterRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryUnregisterEvent;
import de.ascendro.f4m.service.registry.model.monitor.PushServiceStatistics;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.registry.store.ServiceStore;
import de.ascendro.f4m.service.registry.store.ServiceStoreImpl;
import de.ascendro.f4m.service.util.ServiceUriUtil;

public class ServiceRegistryClientImpl implements ServiceRegistryClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistryClientImpl.class);

	public static final String SERVICE_UNREGISTER_TOPIC_BASE = "serviceRegistry/unregister/";
	public static final String SERVICE_REGISTER_TOPIC_BASE = "serviceRegistry/register/";

	private final F4MConfigImpl config;
	private final JsonWebSocketClientSessionPool webSocketClient;
	private final JsonMessageUtil jsonUtil;
	private final ServiceUriUtil serviceUriUtil;

	private Queue<URI> serviceRegistryUriQueue;
	private final Lock serviceRegistryRefreshLock = new ReentrantLock();
	private final ServiceStoreImpl serviceStoreImpl;
	private LoggingUtil loggingUtil;

	private final Map<String,AtomicBoolean> scheduledConnInfoRequests = new ConcurrentHashMap<>();
	private final ScheduledExecutorService scheduledExecutorService;

	@Inject
	public ServiceRegistryClientImpl(Config config, JsonWebSocketClientSessionPool webSocketClient,
			JsonMessageUtil jsonUtil, ServiceUriUtil serviceUriUtil, ServiceStoreImpl serviceStoreImpl, LoggingUtil loggingUtil) {
		this.config = (F4MConfigImpl) config;
		this.webSocketClient = webSocketClient;
		this.jsonUtil = jsonUtil;
		this.serviceUriUtil = serviceUriUtil;
		this.serviceStoreImpl = serviceStoreImpl;
		this.loggingUtil = loggingUtil;

		ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("ServiceRegistryClient-Scheduler-%d")
				.build();
		scheduledExecutorService = Executors.newScheduledThreadPool(2, factory);

		refreshServiceRegistryUriQueue();
	}

	@PreDestroy
	public void finialize() {
		if (scheduledExecutorService != null) {
			try {
				final List<Runnable> scheduledRunnabled = scheduledExecutorService.shutdownNow();
				LOGGER.info("Stopping ServiceRegistryUtil with {} scheduled tasks", scheduledRunnabled != null
						? scheduledRunnabled.size() : 0);
			} catch (Exception e) {
				LOGGER.error("Failed to shutdown ServiceRegistryUtil scheduler", e);
			}
		}
	}

	@Override
	public ServiceConnectionInformation getServiceConnectionInformation(String serviceName)
			throws F4MNoServiceRegistrySpecifiedException, F4MServiceConnectionInformationNotFoundException {
		final ServiceConnectionInformation serviceConnectionInformation = getServiceConnInfoFromStore(serviceName);
		if (serviceConnectionInformation == null) {
			requestServiceConnectionInformation(serviceName);
			throw new F4MServiceConnectionInformationNotFoundException(serviceName + " service information is not present");
		}
		return serviceConnectionInformation;
	}

	@Override
	public ServiceConnectionInformation getServiceConnInfoFromStore(String serviceName) {
		return serviceStoreImpl.getService(serviceName);
	}

	protected Queue<URI> createServiceRegistryUriQueue() {
		final List<String> serviceRegistryUrisStringList =
				config.getPropertyAsListOfStrings(F4MConfigImpl.SERVICE_REGISTRY_URI);

		final Queue<URI> uriQueue;
		if (!CollectionUtils.isEmpty(serviceRegistryUrisStringList)) {
			final URI[] serviceRegistryURIs = serviceUriUtil.parseServiceUris(serviceRegistryUrisStringList);
			uriQueue = new ArrayBlockingQueue<>(serviceRegistryURIs.length);
			Collections.addAll(uriQueue, serviceRegistryURIs);
		} else {
			LOGGER.warn("No Service Registry URIs sepcifified within (parameter[{}])", F4MConfigImpl.SERVICE_REGISTRY_URI);
			uriQueue = new LinkedList<>();
		}

		return uriQueue;
	}

	@Override
	public boolean containsServiceRegistryUri(URI serviceRegistryUri) {
		final Queue<URI> serviceRegistryUrisQueue = createServiceRegistryUriQueue();

		final int fullSize = serviceRegistryUrisQueue.size();
		serviceRegistryUrisQueue.removeIf(u -> u.equals(serviceRegistryUri));
		final int filteredSize = serviceRegistryUrisQueue.size();

		return fullSize != filteredSize;
	}
	
	@Override
	public boolean pushMonitoringServiceStatistics(ServiceStatistics serviceStatistics) {
		JsonMessage<PushServiceStatistics> message = jsonUtil.createNewMessage(ServiceRegistryMessageTypes.PUSH_SERVICE_STATISTICS,
				new PushServiceStatistics(serviceStatistics));
		return sendAsyncMessage(message);
	}

	private <T extends JsonMessageContent> boolean sendAsyncMessage(JsonMessage<T> message)
			throws F4MNoServiceRegistrySpecifiedException {
		final boolean sent;
		if (getQueueSize() != 0) {
			sent = sendMessageToFirstServiceRegistry(message);
		} else {
			throw new F4MNoServiceRegistrySpecifiedException("No Service Registry URI specified. Use "
					+ F4MConfigImpl.SERVICE_REGISTRY_URI + " parameter to set comma separated list");
		}
		return sent;
	}

	private <T extends JsonMessageContent> boolean sendMessageToFirstServiceRegistry(JsonMessage<T> message) {
		boolean sent = false;
		int rounds = 0;
		int queueSize;

		do {
			URI serviceRegistryURI = peekFirstElement();
			try {
				rounds++;
				final ServiceConnectionInformation serviceRegistryConnInfo =
						new ServiceConnectionInformation(ServiceRegistryMessageTypes.SERVICE_NAME, serviceRegistryURI,
								ServiceRegistryMessageTypes.SERVICE_NAME);
				webSocketClient.sendAsyncMessage(serviceRegistryConnInfo, message);
				sent = true;
			} catch (F4MIOException e) {
				pollFirstToEnd();
				LOGGER.error("Failed to send message to Service Registry[" + serviceRegistryURI + "]", e);
			}

			queueSize = getQueueSize();
		} while (!sent && rounds < queueSize);

		if (!sent) {
			LOGGER.error("Failed to send message to ALL specified Service Registries");
		}
		return sent;
	}

	@Override
	public void refreshServiceRegistryUriQueue() {
		serviceRegistryRefreshLock.lock();
		try {
			serviceRegistryUriQueue = null;
			serviceRegistryUriQueue = createServiceRegistryUriQueue();
		} finally {
			serviceRegistryRefreshLock.unlock();
		}

	}

	private int getQueueSize() {
		serviceRegistryRefreshLock.lock();
		try {
			return serviceRegistryUriQueue.size();
		} finally {
			serviceRegistryRefreshLock.unlock();
		}
	}

	private URI peekFirstElement() {
		serviceRegistryRefreshLock.lock();
		try {
			return serviceRegistryUriQueue.element();
		} finally {
			serviceRegistryRefreshLock.unlock();
		}
	}

	private void pollFirstToEnd() {
		serviceRegistryRefreshLock.lock();
		try {
			serviceRegistryUriQueue.add(serviceRegistryUriQueue.poll());
		} finally {
			serviceRegistryRefreshLock.unlock();
		}
	}

	@Override
	public void register() {
		final List<String> serviceNamespaces = config.getPropertyAsListOfStrings(F4MConfig.SERVICE_NAMESPACES);
		final String serviceName = getServiceName();
		if (!CollectionUtils.isEmpty(serviceNamespaces)) {
			register(serviceName, serviceNamespaces);
		} else {
			LOGGER.error("Service has no namespaces specified");
		}
	}

	private void retryRegister(String serviceName, List<String> namespaces) {
		final Long retryDelay = config.getPropertyAsLong(F4MConfigImpl.SERVICE_CONNECTION_RETRY_DELAY);
		LOGGER.info("Service register call scheduled within {} milliseconds", retryDelay);
		scheduledExecutorService.schedule(() -> {
			loggingUtil.saveBasicInformationInThreadContext();
			register(serviceName, namespaces);
		}, retryDelay, TimeUnit.MILLISECONDS);
	}

	private void register(String serviceName, List<String> serviceNamespaces) {
		String serviceRegistryUriAsString = null;
		try {
			serviceRegistryUriAsString = config.getServiceURI()
					.toString();

			final JsonMessage<ServiceRegistryRegisterRequest> registerRequestMessage =
					jsonUtil.createNewMessage(ServiceRegistryMessageTypes.REGISTER);
			registerRequestMessage.setContent(new ServiceRegistryRegisterRequest(serviceName,
					serviceRegistryUriAsString, serviceNamespaces));
			final boolean sent = sendAsyncMessage(registerRequestMessage);
			if (!sent) {
				retryRegister(serviceName, serviceNamespaces);
			} else {
				discoverDependentServices();
			}
		} catch (URISyntaxException | F4MNoServiceRegistrySpecifiedException e) {
			LOGGER.error("Unable to connect to service registry. No retry shceldued. Please provide valid and running service registry URL[{}]", serviceRegistryUriAsString, e);
		} catch (Exception e) {
			LOGGER.error("Failed to register within service registry", e);
			retryRegister(serviceName, serviceNamespaces);
		}
	}

	private String getServiceName() {
		return config.getProperty(F4MConfig.SERVICE_NAME);
	}

	@Override
	public String getServiceRegisterTopicName(String serviceName) {
		return SERVICE_REGISTER_TOPIC_BASE + serviceName;
	}

	@Override
	public String getServiceUnregisterTopicName(String serviceName) {
		return SERVICE_UNREGISTER_TOPIC_BASE + serviceName;
	}

	@Override
	public void unregister() throws URISyntaxException, F4MNoServiceRegistrySpecifiedException {
		final JsonMessage<ServiceRegistryUnregisterEvent> unregisterRequestMessage =
				jsonUtil.createNewMessage(ServiceRegistryMessageTypes.UNREGISTER);

		final ServiceRegistryUnregisterEvent serviceRegistryUnregisterEvent = new ServiceRegistryUnregisterEvent();
		serviceRegistryUnregisterEvent.setServiceName(getServiceName());
		serviceRegistryUnregisterEvent.setUri(config.getServiceURI()
				.toString());

		unregisterRequestMessage.setContent(serviceRegistryUnregisterEvent);
		sendAsyncMessage(unregisterRequestMessage);
	}

	@Override
	public void requestServiceConnectionInformation(String serviceName) throws F4MNoServiceRegistrySpecifiedException {
		final JsonMessage<ServiceRegistryGetRequest> getServiceInfoMessage =
				jsonUtil.createNewMessage(ServiceRegistryMessageTypes.GET);

		final ServiceRegistryGetRequest getRequestContent = new ServiceRegistryGetRequest(serviceName);
		getServiceInfoMessage.setContent(getRequestContent);

		final boolean sent = sendAsyncMessage(getServiceInfoMessage);
		if (!sent) {
			LOGGER.error("Failed to request service information from service registry");
		}

		final Long deaultInMs = config.getPropertyAsLong(F4MConfigImpl.SERVICE_DISCOVERY_DELAY);
		scheduledServiceInfoRequest(serviceName, deaultInMs);
	}

	@Override
	public ServiceStore<? extends ServiceConnectionInformation> getServiceStore() {
		return serviceStoreImpl;
	}

	@Override
	public void scheduledServiceInfoRequest(final String serviceName, long delayInMiliseconds) {
		AtomicBoolean scheduledConnInfoRequestStatus = getScheduledConnInfoRequestStatus(serviceName);
		if (!scheduledConnInfoRequestStatus.get()) {
			scheduledConnInfoRequestStatus.set(true);
			LOGGER.info("Scheduled service info request for service[{}] in {} ms", serviceName, delayInMiliseconds);
			scheduledExecutorService.schedule(() -> {
				AtomicBoolean isScheduled = getScheduledConnInfoRequestStatus(serviceName);
				synchronized (isScheduled) {
					if (isScheduled.get()) {
						try {
							isScheduled.set(false);
							requestServiceConnectionInformation(serviceName);
						} catch (F4MNoServiceRegistrySpecifiedException e) {
							LOGGER.error("Failed to request service[{}] connection inforation", serviceName, e);
						}
					}
				}
			}, delayInMiliseconds, TimeUnit.MILLISECONDS);
		} else {
			LOGGER.info("Service info request for service[{}] already scheduled", serviceName, delayInMiliseconds);
		}
	}

	@Override
	public void requestServiceList(String serviceName) throws F4MNoServiceRegistrySpecifiedException {
		final JsonMessage<ServiceRegistryListRequest> eventServiceListRequestMessage =
				jsonUtil.createNewMessage(ServiceRegistryMessageTypes.LIST);
		eventServiceListRequestMessage.setContent(new ServiceRegistryListRequest(serviceName));

		sendAsyncMessage(eventServiceListRequestMessage);
	}

	@Override
	public void discoverDependentServices() {
		final List<String> dependentServices = getDependentServiceNames();
		if (!CollectionUtils.isEmpty(dependentServices)) {
			LOGGER.debug("{} try to discover dependent services {}", getServiceName(), dependentServices);
			dependentServices.forEach(n -> requestServiceConnectionInformation(n));
		} else {
			LOGGER.info("Service does not have any dependent services to be discovered");
		}
	}

	@Override
	public List<String> getDependentServiceNames() {
		return config.getPropertyAsListOfStrings(F4MConfig.SERVICE_DEPENDENT_SERVICES);
	}
	
	@Override
	public void addService(ServiceConnectionInformation serviceInfo) {
		Validate.notNull(serviceInfo, "Service connection information must be specified to connect");
		Validate.notNull(serviceInfo.getUri(), "Service URI information must be specified to connect");
		Validate.notNull(serviceInfo.getServiceName(), "Service name information should be specified to connect");

		getServiceStore().addService(serviceInfo);
		AtomicBoolean isScheduled = getScheduledConnInfoRequestStatus(serviceInfo.getServiceName());
		synchronized (isScheduled) {
			isScheduled.set(false);
		}
	}
	
	private AtomicBoolean getScheduledConnInfoRequestStatus(String serviceName) {
		AtomicBoolean isScheduled = scheduledConnInfoRequests.get(serviceName);
		if (isScheduled == null) {
			synchronized (scheduledConnInfoRequests) {
				isScheduled = scheduledConnInfoRequests.get(serviceName);
				if (isScheduled == null) {
					isScheduled = new AtomicBoolean(false);
					scheduledConnInfoRequests.put(serviceName, isScheduled);
				}
			}
		}
		return isScheduled;
	}
	
	@Override
	public boolean hasScheduledServiceConnInfoRequests(){
		return scheduledConnInfoRequests.values().stream().anyMatch(v -> v.get());
	}

}
