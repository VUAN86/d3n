package de.ascendro.f4m.service.registry.util;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.ServiceRegistryUnregisterEvent;
import de.ascendro.f4m.service.registry.store.ServiceData;
import de.ascendro.f4m.service.registry.store.ServiceRegistry;
import de.ascendro.f4m.service.util.EventServiceClientImpl;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class ServiceRegistryEventServiceUtil extends EventServiceClientImpl {

	private final ServiceRegistry serviceRegistry;
	
	@Inject
	public ServiceRegistryEventServiceUtil(JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool,
			JsonMessageUtil jsonUtil, ServiceRegistryClient serviceRegistryClient,
			EventSubscriptionStore eventSubscriptionStore, Config config, ServiceRegistry serviceRegistry,
			LoggingUtil loggingUtil) {
		super(jsonWebSocketClientSessionPool, jsonUtil, serviceRegistryClient, eventSubscriptionStore, config,
				loggingUtil);
		this.serviceRegistry = serviceRegistry;
	}

	public void publishUnregisterEvent(ServiceData serviceData) {
		final String unregisterTopic = serviceRegistryClient.getServiceUnregisterTopicName(serviceData.getServiceName());
		ServiceRegistryUnregisterEvent content = new ServiceRegistryUnregisterEvent(serviceData.getServiceName(), serviceData.getUri());
		publish(unregisterTopic, jsonUtil.toJsonElement(content));
	}

	public void publishRegisterEvent(String serviceName, JsonMessageContent content) {
		final String registerTopic = serviceRegistryClient.getServiceRegisterTopicName(serviceName);
		publish(registerTopic, jsonUtil.toJsonElement(content));
	}

	@Override
	protected ServiceConnectionInformation getServiceConnectionInformation() {
		return serviceRegistry.getServiceConnectionInformation(EventMessageTypes.SERVICE_NAME);
	}
	
}
