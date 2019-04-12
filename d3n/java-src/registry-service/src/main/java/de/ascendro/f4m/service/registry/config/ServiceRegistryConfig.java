package de.ascendro.f4m.service.registry.config;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class ServiceRegistryConfig extends F4MConfigImpl implements Config {
	private static final String REGISTER_PROPERTY_BASE = "register.";

	public static final String HEARTBEAT_START_DELAY = REGISTER_PROPERTY_BASE + "heartbeat.start.delay";
	public static final String HEARTBEAT_START_INTERVAL = REGISTER_PROPERTY_BASE + "heartbeat.start.interval";
	public static final String MONITOR_START_DELAY = REGISTER_PROPERTY_BASE + "monitor.start.delay";
	public static final String MONITOR_START_INTERVAL = REGISTER_PROPERTY_BASE + "monitor.start.interval";
	public static final String HEARTBEAT_DISCONNECT_INTERVAL = REGISTER_PROPERTY_BASE + "heartbeat.disconnect.interval";

	public ServiceRegistryConfig() {
		setProperty(SERVICE_REGISTRY_URI, "");
		
		setProperty(HEARTBEAT_START_DELAY, 1000);
		setProperty(HEARTBEAT_START_INTERVAL, 2000);
		setProperty(MONITOR_START_DELAY, 1000);
		setProperty(MONITOR_START_INTERVAL, 5000);
		setProperty(HEARTBEAT_DISCONNECT_INTERVAL, getPropertyAsInteger(HEARTBEAT_START_INTERVAL) * 2);//missed two heart beats

		loadProperties();
	}
}
