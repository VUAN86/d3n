package de.ascendro.f4m.service.registry;

import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.info.InfoResponse;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.store.ServiceStoreImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Extension of Services Store for Event Service, which accumulates all Event Services data within
 * EventServiceInformation entry. All other Services arr stored within regular ServiceConnectionInformation entry.
 */
public class EventServiceStore extends ServiceStoreImpl {

	private static final long serialVersionUID = 4589303632863511297L;

	private static final Logger LOGGER = LoggerFactory.getLogger(EventServiceStore.class);
	private boolean allKnownEventServicesAreConnected = false;

	public EventServiceListInformation addEventServiceInfo(String serviceURI, InfoResponse infoResponse) {
		EventServiceListInformation eventServiceInfo = (EventServiceListInformation) super.get(
				EventMessageTypes.SERVICE_NAME);

		if (eventServiceInfo == null) {
			eventServiceInfo = new EventServiceListInformation(EventMessageTypes.SERVICE_NAME, serviceURI, 
					Collections.singletonList(EventMessageTypes.NAMESPACE));
			put(EventMessageTypes.SERVICE_NAME, eventServiceInfo);
		}
		eventServiceInfo.setJmsPort(serviceURI, infoResponse.getJmsPort());

		return eventServiceInfo;
	}

	@Override
	public ServiceConnectionInformation addService(String serviceName, String uri, List<String> serviceNamespaces) {
		ServiceConnectionInformation serviceConnectionInformation;
		if (serviceName.equalsIgnoreCase(EventMessageTypes.SERVICE_NAME)) {
			if (!containsKey(EventMessageTypes.SERVICE_NAME)) {
				serviceConnectionInformation = new EventServiceListInformation(serviceName, uri, serviceNamespaces);
				put(serviceName, serviceConnectionInformation);
			} else {
				serviceConnectionInformation = getService(serviceName);
			}
			((EventServiceListInformation)serviceConnectionInformation).addUri(uri);
		} else {
			serviceConnectionInformation = super.addService(serviceName, uri, serviceNamespaces);
		}
		return serviceConnectionInformation;
	}
	
	
	/**
	 * Returns true, if MQ connections with all known other event service instances are made.
	 * 
	 * @return
	 */
	public boolean allKnownEventServicesAreConnected() {
		if (!isAllKnownEventServicesAreConnected()) {
			boolean allJmsPortsKnown = checkIfJmsPortsAreKnownForAllEventServices();
			setAllKnownEventServicesAreConnected(allJmsPortsKnown);
		}
		return isAllKnownEventServicesAreConnected();
	}

	private boolean checkIfJmsPortsAreKnownForAllEventServices() {
		EventServiceListInformation eventServiceListInformation = (EventServiceListInformation) getService(
				EventMessageTypes.SERVICE_NAME);
		StringBuilder serviceUris = new StringBuilder();
		boolean allJmsPortsKnown = false;
		if (eventServiceListInformation != null) {
			allJmsPortsKnown = true;
			for (String uri : eventServiceListInformation.getUris()) {
				Long jmsPort = eventServiceListInformation.getJmsPort(uri);
				if (jmsPort == null) {
					allJmsPortsKnown = false;
					serviceUris.append(uri).append(",");
				}
			}
			if (!allJmsPortsKnown)
				LOGGER.debug("jmsPort for uris:{} is NULL",serviceUris.toString());
		}
		return allJmsPortsKnown;
	}

	public boolean isAllKnownEventServicesAreConnected() {
		return allKnownEventServicesAreConnected;
	}

	public void setAllKnownEventServicesAreConnected(boolean allKnownEventServicesAreConnected) {
		this.allKnownEventServicesAreConnected = allKnownEventServicesAreConnected;
	}
}
