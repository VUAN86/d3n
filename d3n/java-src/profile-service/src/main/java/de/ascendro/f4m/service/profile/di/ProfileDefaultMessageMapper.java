package de.ascendro.f4m.service.profile.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;

public class ProfileDefaultMessageMapper extends DefaultJsonMessageMapper {

	private static final long serialVersionUID = 7211152586021274615L;

	@Inject
	public ProfileDefaultMessageMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper, EventMessageTypeMapper eventMessageTypeMapper,
			ProfileMessageTypeMapper profileMessageTypeMapper, FriendManagerMessageTypeMapper friendManagerMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(eventMessageTypeMapper);
		init(profileMessageTypeMapper);
		init(friendManagerMessageTypeMapper);
	}

}
