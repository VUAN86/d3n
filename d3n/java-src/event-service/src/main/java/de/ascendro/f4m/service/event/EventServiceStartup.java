package de.ascendro.f4m.service.event;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.event.activemq.EmbeddedActiveMQ;
import de.ascendro.f4m.service.event.di.EventServiceModule;
import de.ascendro.f4m.service.event.di.EventWebSocketModule;
import de.ascendro.f4m.service.event.util.EventServiceDiscoveryUtil;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class EventServiceStartup extends ServiceStartup {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventServiceDiscoveryUtil.class);

	public EventServiceStartup(Stage stage) {
		super(stage);
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(stage, Modules.override(super.getModules()).with(getModules()));
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new EventServiceModule(), new EventWebSocketModule());
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		stopActiveMQ();
		unsubscribeFromEventServices();
	}

	@Override
	public void start() throws Exception {
		super.start();
		startActiveMQ();
		discoverEventServices();
	}
	
	@Override
	protected List<String> getDefaultDependentServiceNames() {
		return Collections.<String> emptyList(); //No authentication service needed
	}

	/**
	 * Discover all event services and sign-up for any change events to service registry
	 */
	public void discoverEventServices() {
		final EventServiceDiscoveryUtil discoveryUtil = getInjector().getInstance(EventServiceDiscoveryUtil.class);
		final ServiceRegistryClient serviceRegistryClient = getInjector().getInstance(ServiceRegistryClient.class);

		try {
			serviceRegistryClient.requestServiceList(EventMessageTypes.SERVICE_NAME);
		} catch (Exception e) {
			LOGGER.error("Cannot request Event services list from Service Registry", e);
		}
		try {
			discoveryUtil.subscribeForRegister();
		} catch (JMSException e) {
			LOGGER.error(
					"Failed to subscribe for TOPIC["
							+ serviceRegistryClient.getServiceRegisterTopicName(EventMessageTypes.SERVICE_NAME) + "]", e);
		}

		try {
			discoveryUtil.subscribeForUnregister();
		} catch (JMSException e) {
			LOGGER.error(
					"Failed to subscribe for TOPIC["
							+ serviceRegistryClient.getServiceUnregisterTopicName(EventMessageTypes.SERVICE_NAME) + "]",
					e);
		}
	}

	public void unsubscribeFromEventServices() {
		final EventServiceDiscoveryUtil discoveryUtil = getInjector().getInstance(EventServiceDiscoveryUtil.class);
		final ServiceRegistryClient serviceRegistryClient = getInjector().getInstance(ServiceRegistryClient.class);

		try {
			if (discoveryUtil != null) {
				discoveryUtil.unsubscribeForRegister();
			}
		} catch (JMSException e) {
			LOGGER.error(
					"Failed to unsubscribe for TOPIC["
							+ serviceRegistryClient.getServiceRegisterTopicName(EventMessageTypes.SERVICE_NAME) + "]", e);
		}

		try {
			if (discoveryUtil != null) {
				discoveryUtil.unsubscribeForUnregister();
			}
		} catch (JMSException e) {
			LOGGER.error(
					"Failed to unsubscribe for TOPIC["
							+ serviceRegistryClient.getServiceUnregisterTopicName(EventMessageTypes.SERVICE_NAME) + "]",
					e);
		}
	}

	public void startActiveMQ() throws Exception {
		getInjector().getInstance(EmbeddedActiveMQ.class).start();
	}

	public void stopActiveMQ() throws Exception {
		EmbeddedActiveMQ embeddedActiveMQ = getInjector().getInstance(EmbeddedActiveMQ.class);
		if (embeddedActiveMQ != null) {
			embeddedActiveMQ.stop();
		}
	}

	public static void main(String... args) throws Exception {
		new EventServiceStartup(Stage.PRODUCTION).start();
	}

	@Override
	protected String getServiceName() {
		return EventMessageTypes.SERVICE_NAME;
	}
}
