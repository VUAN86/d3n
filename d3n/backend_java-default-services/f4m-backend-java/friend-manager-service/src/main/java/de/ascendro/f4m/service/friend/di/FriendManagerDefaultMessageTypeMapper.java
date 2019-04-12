package de.ascendro.f4m.service.friend.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.auth.AuthMessageTypeMapper;
import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypeMapper;

public class FriendManagerDefaultMessageTypeMapper extends DefaultJsonMessageMapper {
	private static final long serialVersionUID = 2947536629279814469L;
	
	@Inject
	public FriendManagerDefaultMessageTypeMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper, AuthMessageTypeMapper authMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper, FriendManagerMessageTypeMapper friendManagerMessageTypeMapper,
			PaymentMessageTypeMapper paymentMessageTypeMapper, UserMessageMessageTypeMapper userMessageMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(friendManagerMessageTypeMapper);
		init(authMessageTypeMapper);
		init(paymentMessageTypeMapper);
		init(userMessageMessageTypeMapper);
	}

}
