package de.ascendro.f4m.service;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import com.mycila.guice.ext.closeable.CloseableInjector;
import com.mycila.guice.ext.closeable.CloseableModule;
import com.mycila.guice.ext.jsr250.Jsr250Module;

import de.ascendro.f4m.server.EmbeddedJettyServer;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.ServiceModule;
import de.ascendro.f4m.service.di.WebSocketModule;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.registry.exception.F4MNoServiceRegistrySpecifiedException;
import de.ascendro.f4m.service.util.register.MonitoringTimer;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public abstract class ServiceStartup {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceStartup.class);

	public static final Stage DEFAULT_STAGE = Stage.PRODUCTION;
	private final Injector injector;

	public ServiceStartup(Stage stage) {
		this.injector = createInjector(stage);
	}

	public Injector createInjector(Stage stage) {
		return Guice.createInjector(stage, getModules());
	}

	protected Iterable<? extends Module> getModules() {
		return getBaseModules();
	}

	/**
	 * Method for using in integration tests, where parent injections should be overridden, while preserving base module
	 * injects.
	 * 
	 * @return
	 */
	protected Iterable<? extends Module> getBaseModules() {
		return Arrays.asList(new CloseableModule(), new Jsr250Module(), new ServiceModule(), new WebSocketModule() {
		});
	}

	public void startupJetty() throws Exception {
		initServiceName();
		initNamespaces();
		initDependentServices();
		final EmbeddedJettyServer server = getEmbeddedJettyServer();
		server.setThreadName(getServiceName());
		server.startServer(createGuiceServletContextListener());
		LOGGER.info("Started Jetty [{} - {}]", getServiceName(), server.getURI());
	}

	public void stopJetty() throws Exception {
		final EmbeddedJettyServer server = getEmbeddedJettyServer();
		if (server != null && !server.isStopped() && !server.isStopping()) {
			LOGGER.info("Stopping Jetty [{} - {}]", getServiceName(), server.getURI());
			server.stopServer();
		} else {
			LOGGER.info("Skip to stop Jetty with state [{}]", server != null ? server.getState() : "NULL");
		}
	}

	private EmbeddedJettyServer getEmbeddedJettyServer() {
		return injector.getInstance(EmbeddedJettyServer.class);
	}

	public GuiceServletContextListener createGuiceServletContextListener() {
		return new GuiceServletContextListener() {

			@Override
			protected Injector getInjector() {
				return injector;
			}
		};
	}

	public void start() throws Exception {
		LOGGER.info("Starting service [{}]", getServiceName());
		addShutdownHook();//FIXME: Fix test hanging
		startupJetty();
		register();
		discoverDependentServices();
		startMonitoring();
	}

	public void discoverDependentServices() throws F4MNoServiceRegistrySpecifiedException {
		final ServiceRegistryClient serviceRegsitry = injector.getInstance(ServiceRegistryClient.class);
		serviceRegsitry.discoverDependentServices();
	}
	
	public void startMonitoring() {
		getInjector().getInstance(MonitoringTimer.class).startMonitoring();
	}

	public void register() throws F4MException, URISyntaxException {
		final ServiceRegistryClient serviceRegistryClient = injector.getInstance(ServiceRegistryClient.class);
		serviceRegistryClient.register();
	}

	public void stop() throws Exception {
		try {
			LOGGER.info("Stopping service [{}]", getServiceName());
			stopJetty();
		} finally {
			unregister();
		}
		closeInjector();
	}
	
	public void closeInjector() {
		injector.getInstance(Config.class).setProperty(F4MConfigImpl.SERVICE_SHUTTING_DOWN, true);
		try {
			injector.getInstance(CloseableInjector.class).close();
		} catch(ConfigurationException e) {
			LOGGER.warn("Error closing injector", e);
		}
	}

	public void unregister() throws F4MException, URISyntaxException {
		final ServiceRegistryClient serviceRegistryClient = injector.getInstance(ServiceRegistryClient.class);
		if (serviceRegistryClient != null) {
			serviceRegistryClient.unregister();
		}
	}

	public Injector getInjector() {
		return injector;
	}

	protected abstract String getServiceName();

	protected List<String> getDependentServiceNames(){
		return null;
	}
	
	protected List<String> getDefaultDependentServiceNames() {
		return Collections.emptyList();
	}

	protected List<String> getServiceNamespaces() {
		return Collections.singletonList(getServiceName());
	}

	protected void initServiceName() {
		final Config config = injector.getInstance(Config.class);
		config.setProperty(F4MConfig.SERVICE_NAME, getServiceName());
	}

	protected void initNamespaces() {
		final Config config = injector.getInstance(Config.class);
		config.setProperty(F4MConfig.SERVICE_NAMESPACES, getServiceNamespaces());
	}

	/**
	 * This method will be used in order to close the service in a clean manner. Any close signal will be handled
	 * excepting <b>kill -9</b> which does not reach the shutdown hook.
	 */
	protected void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					ServiceStartup.this.stop();
				} catch (Exception e) {
					// Something went wrong while stopping the service
					System.err.println("Stop Service " + getServiceName() + " has thrown an exception");
					e.printStackTrace(System.err);
				}
				System.out.println("Service \"" + getServiceName() + "\" was killed");
			}
		});
	}

	public List<String> initDependentServices() {
		final List<String> dependentServices = new ArrayList<>();

		//Default
		final List<String> defaultDependentServices = getDefaultDependentServiceNames();
		if(!CollectionUtils.isEmpty(defaultDependentServices)){
			dependentServices.addAll(defaultDependentServices);
		}
		
		//Additional
		final List<String> additionalDependentServices = getDependentServiceNames();
		if(!CollectionUtils.isEmpty(additionalDependentServices)){
			dependentServices.addAll(additionalDependentServices);
		}

		final Config config = injector.getInstance(Config.class);
		config.setProperty(F4MConfig.SERVICE_DEPENDENT_SERVICES, dependentServices);
		
		return dependentServices;
	}
}
