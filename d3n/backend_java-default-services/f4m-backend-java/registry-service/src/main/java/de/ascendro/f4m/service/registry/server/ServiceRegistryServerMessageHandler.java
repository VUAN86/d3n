package de.ascendro.f4m.service.registry.server;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.exception.F4MNoServiceRegistrySpecifiedException;
import de.ascendro.f4m.service.registry.exception.F4MServiceConnectionInformationNotFoundException;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetResponse;
import de.ascendro.f4m.service.registry.model.ServiceRegistryListRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryListResponse;
import de.ascendro.f4m.service.registry.model.ServiceRegistryRegisterRequest;
import de.ascendro.f4m.service.registry.model.monitor.InfrastructureStatisticsResponse;
import de.ascendro.f4m.service.registry.model.monitor.PushServiceStatistics;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.registry.store.ServiceData;
import de.ascendro.f4m.service.registry.store.ServiceRegistry;
import de.ascendro.f4m.service.registry.util.ServiceRegistryEventServiceUtil;
import de.ascendro.f4m.service.registry.util.ServiceRegistryRegistrationHelper;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.register.MonitoringTimerTask;

public class ServiceRegistryServerMessageHandler extends DefaultJsonMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistryServerMessageHandler.class);

	private final ServiceRegistry serviceRegistry;
	private final ServiceRegistryEventServiceUtil serviceRegistryEventServiceUtil;
	private final MonitoringTimerTask monitoringTimerTask;

	public ServiceRegistryServerMessageHandler(ServiceRegistry serviceRegistry,
			ServiceRegistryEventServiceUtil serviceRegistryEventServiceUtil, MonitoringTimerTask monitoringTimerTask) {
		this.serviceRegistry = serviceRegistry;
		this.serviceRegistryEventServiceUtil = serviceRegistryEventServiceUtil;
		this.monitoringTimerTask = monitoringTimerTask;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message)
			throws F4MException {
		Validate.notNull(message, "Message is mandatory");

		JsonMessageContent response = null;// error messages and unregister have no content!
		ServiceRegistryMessageTypes type = message.getType(ServiceRegistryMessageTypes.class);

		switch (type) {
		case REGISTER: // comparing to string to a common interface/implementation
			Validate.notNull(message.getContent(), "Message content is mandatory");
			Validate.isInstanceOf(ServiceRegistryRegisterRequest.class, message.getContent());
			response = onRegister((ServiceRegistryRegisterRequest) message.getContent());
			break;
		case UNREGISTER:
			response = onUnregister();
			break;
		case GET:
			Validate.notNull(message.getContent(), "Message content is mandatory");
			Validate.isInstanceOf(ServiceRegistryGetRequest.class, message.getContent());
			response = onGet((ServiceRegistryGetRequest) message.getContent());
			break;
		case LIST:
			Validate.notNull(message.getContent(), "Message content is mandatory");
			Validate.isInstanceOf(ServiceRegistryListRequest.class, message.getContent());
			response = onList((ServiceRegistryListRequest) message.getContent());
			break;
		case HEARTBEAT_RESPONSE:
			onHeartbeatResponse();
			break;
		case PUSH_SERVICE_STATISTICS:
			onPushServiceStatistics(((JsonMessage<PushServiceStatistics>) message).getContent());
			break;
		case GET_INFRASTRUCTURE_STATISTICS:
			response = getInfrastructureStatisticsResponse();
			break;
		default:
			LOGGER.error("ServiceRegistry received unrecognized {} message {}", type, message);
			throw new F4MValidationFailedException("Incorrect message type " + type);
		}
		return response;
	}

	private void onHeartbeatResponse() {
		final ServiceRegistrySessionStore sessionStore = getSessionWrapper().getSessionStore();
		final ServiceData serviceData = sessionStore.getServiceConnectionData();
		serviceData.setLastHeartbeatResponse(DateTimeUtil.getCurrentDateTime());
	}

	private void onPushServiceStatistics(PushServiceStatistics message) {
		final ServiceRegistrySessionStore sessionStore = getSessionWrapper().getSessionStore();
		final ServiceData serviceData = sessionStore.getServiceConnectionData();
		if (serviceData != null) {
			serviceData.setLatestStatistics(message.getStatistics());
		} else {
			throw new F4MFatalErrorException(message.getStatistics().getServiceName()
					+ " was trying to push statistics before registering first");
		}
	}

	private InfrastructureStatisticsResponse getInfrastructureStatisticsResponse() {
		List<ServiceStatistics> statisticsList = serviceRegistry.getServiceStatisticsList();
		statisticsList.add(monitoringTimerTask.prepareServiceStatisticsData());
		return new InfrastructureStatisticsResponse(statisticsList);
	}

	private JsonMessageContent onGet(ServiceRegistryGetRequest message) {
		String serviceName = message.getServiceName();
		String serviceNamespace = message.getServiceNamespace();
		boolean serviceNameProvided = StringUtils.isNotBlank(serviceName);
		Validate.isTrue(serviceNameProvided || StringUtils.isNotBlank(serviceNamespace));
		
		if (!serviceNameProvided) {
			serviceName = serviceRegistry.getServiceName(serviceNamespace);
		}
		
		return new ServiceRegistryGetResponse(serviceRegistry.getServiceConnectionInformation(serviceName));
	}

	private JsonMessageContent onList(ServiceRegistryListRequest message) {
		String serviceName = message.getServiceName();
		String serviceNamespace = message.getServiceNamespace();
		ServiceRegistryListResponse response = new ServiceRegistryListResponse();
		if (StringUtils.isBlank(serviceName) && StringUtils.isNotBlank(serviceNamespace)) {
			serviceName = serviceRegistry.getServiceName(serviceNamespace);
			if (serviceName == null) {
				response.setServices(Collections.emptyList());
				return response;
			}
		}
		response.setServices(serviceRegistry.list(serviceName));
		return response;
	}

	private JsonMessageContent onRegister(ServiceRegistryRegisterRequest message)
			throws F4MServiceConnectionInformationNotFoundException, F4MNoServiceRegistrySpecifiedException {
		try {
			ServiceData data = serviceRegistry.register(message.getServiceName(), message.getUri(), 
					message.getServiceNamespaces(), getSessionWrapper());
	        // set so that a heartbeat check right after creation, before the first actual heartbeat, doesn't fail
	        data.setLastHeartbeatResponse(DateTimeUtil.getCurrentDateTime());
            getSessionStore().setServiceConnectionData(data);
			LOGGER.debug("Registered service {} with namespaces {} at uri {}", 
					message.getServiceName(), message.getServiceNamespaces(), message.getUri());
	
			serviceRegistryEventServiceUtil.publishRegisterEvent(message.getServiceName(), message);
	
			performFirstHeartbeat();
	
			return new EmptyJsonMessageContent();
		} catch (F4MIOException e) {
			throw new F4MFatalErrorException("Failed to register service", e);
		}
	}

	private void performFirstHeartbeat() {
		final JsonMessage<JsonMessageContent> heartbeat = jsonMessageUtil
				.createNewMessage(ServiceRegistryMessageTypes.HEARTBEAT);
		sendAsyncMessage(heartbeat);
	}

	protected JsonMessageContent onUnregister()
			throws F4MServiceConnectionInformationNotFoundException, F4MNoServiceRegistrySpecifiedException {
		try {
			ServiceRegistrySessionStore sessionStore = getSessionStore();
			ServiceData serviceData = sessionStore.getServiceConnectionData();
			ServiceRegistryRegistrationHelper.unregister(serviceData, serviceRegistry, serviceRegistryEventServiceUtil);
			return new EmptyJsonMessageContent();
		} catch (F4MIOException e) {
			throw new F4MFatalErrorException("Failed to unregister service", e);
		}
	}

	private ServiceRegistrySessionStore getSessionStore() {
		return (ServiceRegistrySessionStore) getSessionWrapper().getSessionStore();
	}

	@Override
	public ClientInfo onAuthentication(RequestContext context) {
		return null;
	}

}
