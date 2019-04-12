package de.ascendro.f4m.service.di.handler;

import javax.inject.Inject;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.util.register.HeartbeatMonitoringProvider;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public abstract class DefaultJsonMessageHandlerProviderImpl extends JsonMessageHandlerProviderImpl {

	@Inject
	protected ServiceRegistryClient serviceRegistryClient;

	@Inject
	protected Config config;

	@Inject
	protected LoggingUtil loggedMessageUtil;
	@Inject
	protected HeartbeatMonitoringProvider serviceMonitoringStatus;

	@Override
	public JsonMessageHandler get() {
		final JsonMessageHandler jsonMessageHandler = createServiceMessageHandler();

		if (jsonMessageHandler instanceof DefaultJsonMessageHandler) {
			final DefaultJsonMessageHandler defaultJsonMessageHandler = (DefaultJsonMessageHandler) jsonMessageHandler;

			//DefaultJsonMessageHandler
			defaultJsonMessageHandler.setServiceRegistryClient(serviceRegistryClient);
			defaultJsonMessageHandler.setServiceMonitoringStatus(serviceMonitoringStatus);

			//JsonMessageHandler
			defaultJsonMessageHandler.setJsonMessageUtil(jsonMessageUtil);
			
			//ServiceMessageHandler
			defaultJsonMessageHandler.setConfig(config);
			defaultJsonMessageHandler.setLoggingUtil(loggedMessageUtil);
			defaultJsonMessageHandler.setSessionWrapperFactory(sessionWrapperFactory);
		}

		return jsonMessageHandler;
	}
}
