package de.ascendro.f4m.service.usermessage.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypeMapper;

public class UserMessageDefaultMessageTypeMapper extends DefaultJsonMessageMapper {
	private static final long serialVersionUID = 2947536629279814470L;

	@Inject
	public UserMessageDefaultMessageTypeMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper,
			UserMessageMessageTypeMapper userMessageMessageTypeMapper,
			ProfileMessageTypeMapper profileMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(userMessageMessageTypeMapper);
		init(profileMessageTypeMapper);
	}
}
