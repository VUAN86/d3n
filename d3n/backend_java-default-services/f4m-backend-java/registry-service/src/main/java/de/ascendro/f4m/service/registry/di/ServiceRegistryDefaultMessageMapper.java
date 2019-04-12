package de.ascendro.f4m.service.registry.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;

public class ServiceRegistryDefaultMessageMapper extends DefaultJsonMessageMapper {

	private static final long serialVersionUID = -1230019587075456942L;

	@Inject
	public ServiceRegistryDefaultMessageMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
	}

}
