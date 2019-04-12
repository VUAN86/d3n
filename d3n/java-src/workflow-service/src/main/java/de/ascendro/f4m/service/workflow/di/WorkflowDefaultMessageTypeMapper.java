package de.ascendro.f4m.service.workflow.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.workflow.WorkflowMessageTypeMapper;

public class WorkflowDefaultMessageTypeMapper extends DefaultJsonMessageMapper {

	private static final long serialVersionUID = -8067786875930903448L;

	@Inject
	public WorkflowDefaultMessageTypeMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper, EventMessageTypeMapper eventMessageTypeMapper,
			WorkflowMessageTypeMapper workflowMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(eventMessageTypeMapper);
		init(workflowMessageTypeMapper);
	}

}
