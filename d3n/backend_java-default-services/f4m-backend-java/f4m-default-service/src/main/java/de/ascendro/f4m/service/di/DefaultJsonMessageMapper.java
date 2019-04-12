package de.ascendro.f4m.service.di;

import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;

public abstract class DefaultJsonMessageMapper extends JsonMessageTypeMapImpl {

	private static final long serialVersionUID = -5328805275563892363L;

	public DefaultJsonMessageMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper) {
		init(serviceRegistryMessageTypeMapper);
		init(gatewayMessageTypeMapper);
	}

}
