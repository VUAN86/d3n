package de.ascendro.f4m.service.integration.rule;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class JettyServerRule extends ExternalResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(JettyServerRule.class);

	private ServiceStartup serverStartup;
	private Integer jettySslPort;

	protected JettyServerRule(int jettySslPort) {
		setJettySslPort(jettySslPort);
	}

	public JettyServerRule(ServiceStartup serverStartup) {
		this.setServerStartup(serverStartup);

		final Config config = serverStartup.getInjector().getInstance(Config.class);
		setJettySslPort(config.getPropertyAsInteger(F4MConfigImpl.JETTY_SSL_PORT));
	}

	public JettyServerRule(ServiceStartup serverStartup, int jettySslPort) {
		this.setServerStartup(serverStartup);
		initJettySslPort(jettySslPort);
		setJettySslPort(jettySslPort);
	}

	@Override
	protected void before() throws Throwable {
		long time = System.currentTimeMillis();
		LOGGER.debug("JettyServerRule - starting Jetty");
		startup();
		LOGGER.debug("JettyServerRule - Jetty started");
		LOGGER.info("Server started up on port {} in {} ms", jettySslPort, System.currentTimeMillis() - time);
	}

	protected void startup() throws Exception {
		getServerStartup().startupJetty();
		getServerStartup().register();
	}

	@Override
	protected void after() {
		try {
			stop();
		} catch (Exception e) {
			LOGGER.warn("Could not stop service server", e);
		}
	}

	protected void stop() throws Exception {
		getServerStartup().stopJetty();
		getServerStartup().closeInjector();
	}

	public ServiceStartup getServerStartup() {
		return serverStartup;
	}

	public void setServerStartup(ServiceStartup serverStartup) {
		this.serverStartup = serverStartup;
		if (jettySslPort != null) {
			initJettySslPort(jettySslPort);
		}
	}

	public int getJettySslPort() {
		return jettySslPort;
	}

	private void setJettySslPort(int jettySslPort) {
		this.jettySslPort = jettySslPort;
	}

	private void initJettySslPort(int jettySslPort) {
		final Config config = serverStartup.getInjector().getInstance(Config.class);
		config.setProperty(F4MConfigImpl.JETTY_SSL_PORT, jettySslPort);
	}

}
