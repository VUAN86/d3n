package de.ascendro.f4m.service.registry;

import javax.inject.Inject;

import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceRegistryHeartbeatResponse;
import de.ascendro.f4m.service.registry.model.ServiceStatus;
import de.ascendro.f4m.service.util.ServiceUtil;

public class ReceivedMessageCollectorWithHeartbeat extends ReceivedMessageCollector {

	private final ServiceUtil serviceUtil;
	
	private boolean heartbeatExecuted;
	
	@Inject
	public ReceivedMessageCollectorWithHeartbeat(ServiceUtil serviceUtil) {
		this.serviceUtil = serviceUtil;
	}

	@Override
	public JsonMessageContent onProcess(RequestContext context) {
		if (ServiceRegistryMessageTypes.HEARTBEAT.equals(context.getMessage()
				.getType(ServiceRegistryMessageTypes.class))) {
			JsonMessage<ServiceRegistryHeartbeatResponse> heartbeat = new JsonMessage<>(
					ServiceRegistryMessageTypes.HEARTBEAT_RESPONSE);
			heartbeat.setSeq(serviceUtil.generateId());
			heartbeat.setContent(new ServiceRegistryHeartbeatResponse());
			heartbeat.getContent().setStatus(ServiceStatus.GOOD);
			sendAsyncMessage(heartbeat);
			setHeartbeatExecuted(true);
		} else {
			super.onProcess(context);
		}
		return null;
	}
	
	@Override
	public void clearReceivedMessageList() {
		super.clearReceivedMessageList();
		setHeartbeatExecuted(false);
	}

	public boolean isHeartbeatExecuted() {
		return heartbeatExecuted;
	}

	public void setHeartbeatExecuted(boolean heartbeatExecuted) {
		this.heartbeatExecuted = heartbeatExecuted;
	}
}
