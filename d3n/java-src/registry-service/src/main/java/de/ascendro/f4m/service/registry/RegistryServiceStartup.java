package de.ascendro.f4m.service.registry;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.registry.di.ServiceRegistryServiceModule;
import de.ascendro.f4m.service.registry.di.ServiceRegistryWebSocketModule;
import de.ascendro.f4m.service.registry.heartbeat.HeartbeatTimer;
import de.ascendro.f4m.service.registry.monitor.MonitorTimer;

public class RegistryServiceStartup extends ServiceStartup {
	public RegistryServiceStartup(Stage stage) {
		super(stage);
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new ServiceRegistryServiceModule(), new ServiceRegistryWebSocketModule());
	}

	public static void main(String... args) throws Exception {
		new RegistryServiceStartup(DEFAULT_STAGE).start();
	}

	@Override
	public void start() throws Exception {
		super.start();
		startHeartbeat();
	}

	protected void startHeartbeat() {
		getInjector().getInstance(HeartbeatTimer.class).startHeartbeat();
		getInjector().getInstance(MonitorTimer.class).startMonitor();
	}
	
	@Override
	public void startMonitoring() {
		//do not push monitoring information from service registry to service registry
	}

	@Override
	public void register() throws F4MException, URISyntaxException {
		//No registration required
	}

	@Override
	public void unregister() throws F4MException, URISyntaxException {
		//No unregistration required
	}

	@Override
	protected List<String> getDefaultDependentServiceNames() {
		return Collections.<String> emptyList(); //No authentication service needed
	}

	@Override
	protected String getServiceName() {
		return ServiceRegistryMessageTypes.SERVICE_NAME;
	}
}
