package de.ascendro.f4m.service.event.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;

/**
 * Accumulates all Event Service Json Message types: default + events
 *
 */
public class EventDefaultJsonMessageMapper extends DefaultJsonMessageMapper {

	private static final long serialVersionUID = -1328805275763892363L;

	@Inject
	public EventDefaultJsonMessageMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper, EventMessageTypeMapper eventMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(eventMessageTypeMapper);
	}
}
