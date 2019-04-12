package de.ascendro.f4m.service.registry;

import javax.inject.Inject;

import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollectorProvider;
import de.ascendro.f4m.service.util.ServiceUtil;

public class ReceivedMessageCollectorWithHeartbeatProvider extends ReceivedMessageCollectorProvider {

	@Inject
	private ServiceUtil serviceUtil;

	@Override
	protected ReceivedMessageCollector createJsonMessageHandler() {
		return new ReceivedMessageCollectorWithHeartbeat(serviceUtil);
	}

}
