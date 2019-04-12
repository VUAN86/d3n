package de.ascendro.f4m.service.payment.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypeMapper;

public class PaymentDefaultMessageTypeMapper extends DefaultJsonMessageMapper {
	private static final long serialVersionUID = -2318935508203696307L;

	@Inject
	public PaymentDefaultMessageTypeMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper, PaymentMessageTypeMapper paymentMessageTypeMapper,
			EventMessageTypeMapper eventMessageTypeMapper, ProfileMessageTypeMapper profileMessageTypeMapper,
			UserMessageMessageTypeMapper userMessageMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(paymentMessageTypeMapper);
		init(eventMessageTypeMapper);
		init(profileMessageTypeMapper);
		init(userMessageMessageTypeMapper);
	}

}
