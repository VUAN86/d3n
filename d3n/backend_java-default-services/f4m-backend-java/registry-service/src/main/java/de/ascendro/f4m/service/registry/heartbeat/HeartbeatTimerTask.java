package de.ascendro.f4m.service.registry.heartbeat;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;

import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.validation.F4MValidationException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.config.ServiceRegistryConfig;
import de.ascendro.f4m.service.registry.store.ServiceData;
import de.ascendro.f4m.service.registry.store.ServiceRegistry;
import de.ascendro.f4m.service.registry.util.ServiceRegistryEventServiceUtil;
import de.ascendro.f4m.service.registry.util.ServiceRegistryRegistrationHelper;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class HeartbeatTimerTask extends TimerTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatTimerTask.class);

	private final ServiceRegistry serviceRegistry;
	private final JsonMessageUtil jsonUtil;
	private final Config config;
	private final ServiceRegistryEventServiceUtil eventServiceUtil;
	private final LoggingUtil loggingUtil;

	@Inject
	public HeartbeatTimerTask(ServiceRegistry serviceRegistry, JsonMessageUtil jsonUtil,
			Config config, ServiceRegistryEventServiceUtil eventServiceUtil, LoggingUtil loggingUtil) {
		this.serviceRegistry = serviceRegistry;
		this.jsonUtil = jsonUtil;
		this.config = config;
		this.eventServiceUtil = eventServiceUtil;
		this.loggingUtil = loggingUtil;
	}

	@Override
	public void run() {
		loggingUtil.saveBasicInformationInThreadContext();
		LOGGER.trace("Running hearbeat timer task...");

		synchronized (serviceRegistry) {
			final Map<String, Map<String, ServiceData>> registeredServices = serviceRegistry.getRegisteredServices();
			for (Entry<String, Map<String, ServiceData>> entry : registeredServices.entrySet()) {
				final Map<String, ServiceData> serviceInstanceData = entry.getValue();
				if (serviceInstanceData != null) {
					checkServiceInstancesData(serviceInstanceData);
				}
			}
		}
	}

	protected void checkServiceInstancesData(Map<String, ServiceData> serviceInstanceData) {
		for (ServiceData serviceData : serviceInstanceData.values()) {
			SessionWrapper sessionWrapper = serviceData.getSession();
			if (sessionWrapper != null && sessionWrapper.isOpen()) {
				if (isReceivedHeartbeatResponseInLastHeartbeatInterval(serviceData)) {
					final JsonMessage<JsonMessageContent> heartbeat = jsonUtil
							.createNewMessage(ServiceRegistryMessageTypes.HEARTBEAT);
					try {
						sessionWrapper.sendAsynMessage(heartbeat);
					} catch (F4MValidationException e) {
						LOGGER.error("Error sending heartbeat", e);
					}
				} else {	
					try {
						// Closing will also trigger the unregister event
						sessionWrapper.getSession().close(new CloseReason(CloseCodes.GOING_AWAY, "Did not receive heartbeat"));
					} catch (IOException e) {
						LOGGER.error("Error closing session", e);
					}
				}
			} else {
				ServiceRegistryRegistrationHelper.unregister(serviceData, serviceRegistry, eventServiceUtil);
			}
		}
	}

	public boolean isReceivedHeartbeatResponseInLastHeartbeatInterval(ServiceData serviceInstanceData) {
		boolean result = false;
		final Integer heartbeatDisconnectInterval = config
				.getPropertyAsInteger(ServiceRegistryConfig.HEARTBEAT_DISCONNECT_INTERVAL);
		if (heartbeatDisconnectInterval != null) {
			final ZonedDateTime lastHeatbeatResponse = serviceInstanceData.getLastHeartbeatResponse();
			if (lastHeatbeatResponse != null) {
				final ZonedDateTime lastExpectedHeartbeat = DateTimeUtil.getCurrentDateTime()
						.minusNanos(heartbeatDisconnectInterval * DateTimeUtil.MILLI_TO_NANOSECONDS);
				result = lastHeatbeatResponse.isAfter(lastExpectedHeartbeat);
			} else {
				LOGGER.debug("lastHeartbeatResponse is null, returning false");
			}
		} else {
			LOGGER.debug("Auto-unregistering disabled, no unregistering will be done in case of heartbeat timeout");
			result = true; //provides a way how to disable heartbeat timeouts
		}

		return result;
	}

}
