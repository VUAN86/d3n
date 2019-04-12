package de.ascendro.f4m.service.promocode.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.promocode.PromocodeMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;

public class PromocodeDefaultMessageMapper extends DefaultJsonMessageMapper {
	private static final long serialVersionUID = -478698614002483725L;

	@Inject
	public PromocodeDefaultMessageMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper, PromocodeMessageTypeMapper promocodeMessageTypeMapper,
			PaymentMessageTypeMapper paymentMessageTypeMapper, ProfileMessageTypeMapper profileMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(profileMessageTypeMapper);
		init(promocodeMessageTypeMapper);
		init(paymentMessageTypeMapper);
	}

}
