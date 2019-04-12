package de.ascendro.f4m.service.event.config;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;

/*
 * Event Service Configuration store
 */
public class EventConfig extends F4MConfigImpl implements Config {
	private static final String EVENT_PROPERTY_BASE = "event.";
	private static final String ACTIVE_MQ_PROPERTY_NAME_PREFIX = EVENT_PROPERTY_BASE + "activemq.";

	public static final String ACTIVE_MQ_JMX_ENABLED = ACTIVE_MQ_PROPERTY_NAME_PREFIX + "jmxEnabled";
	
	public static final String ACTIVE_MQ_PORT = ACTIVE_MQ_PROPERTY_NAME_PREFIX + "port";
	public static final String ACTIVE_MQ_NETWORK_DISCOVERY_PROTOCOL = ACTIVE_MQ_PROPERTY_NAME_PREFIX
			+ "network.discovery.protocol";

	public static final String ACTIVE_MQ_EXTERNAL_COMMUNICATION_PROTOCOL = ACTIVE_MQ_PROPERTY_NAME_PREFIX
			+ "transport.external.protocol";
	public static final String ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST = ACTIVE_MQ_PROPERTY_NAME_PREFIX
			+ "transport.external.host";
	public static final String ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST_DEFAULT_VALUE = "127.0.0.1";
	public static final String ACTIVE_MQ_INTERNAL_COMMUNICATION_PROTOCOL = ACTIVE_MQ_PROPERTY_NAME_PREFIX
			+ "transport.internal.protocol";

	public static final String MESSAGE_PRODUCER_MESSAGE_TIME_TO_LIVE = ACTIVE_MQ_PROPERTY_NAME_PREFIX
			+ "producer.message.timeToLive";
	public static final String MESSAGE_DURABLE_SUBSCRIBEE_OFLINE_TIMEOUT = ACTIVE_MQ_PROPERTY_NAME_PREFIX
			+ "subscriber.durable.offlineDurableSubscriberTimeout";

	public static final String WILDCARD_SEPARATOR = ACTIVE_MQ_PROPERTY_NAME_PREFIX + "wildcard.separator.actual";

	public EventConfig() {
		final String jettyPort = getProperty(F4MConfigImpl.JETTY_SSL_PORT);
		Validate.notNull(jettyPort, "Jetty Port must not be null");

		initDefaultActiveMqPort(jettyPort);

		setProperty(ACTIVE_MQ_NETWORK_DISCOVERY_PROTOCOL, "static");

		setProperty(ACTIVE_MQ_EXTERNAL_COMMUNICATION_PROTOCOL, "ssl");
		setProperty(ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST, ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST_DEFAULT_VALUE);

		setProperty(ACTIVE_MQ_INTERNAL_COMMUNICATION_PROTOCOL, "vm");

		setProperty(MESSAGE_PRODUCER_MESSAGE_TIME_TO_LIVE, TimeUnit.DAYS.toMillis(7));
		setProperty(MESSAGE_DURABLE_SUBSCRIBEE_OFLINE_TIMEOUT, 5 * 60 * 1000);

		setProperty(WILDCARD_SEPARATOR, "/");

		setProperty(ACTIVE_MQ_JMX_ENABLED, false);
		
		loadProperties();
	}

	private void initDefaultActiveMqPort(String jettyPort) {
		final int firstJettyPortDigit = Integer.parseInt(Character.toString(jettyPort.charAt(0)));
		final String activeMqPortFirstDigit = String.valueOf(firstJettyPortDigit + 1);
		final String activeMqPort = activeMqPortFirstDigit + jettyPort.substring(1);

		setProperty(ACTIVE_MQ_PORT, activeMqPort);
	}
}
