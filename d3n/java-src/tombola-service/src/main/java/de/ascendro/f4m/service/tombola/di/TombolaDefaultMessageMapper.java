package de.ascendro.f4m.service.tombola.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.tombola.TombolaMessageTypeMapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypeMapper;
import de.ascendro.f4m.service.voucher.VoucherMessageTypeMapper;

public class TombolaDefaultMessageMapper extends DefaultJsonMessageMapper {

    private static final long serialVersionUID = -478698614002483725L;

	@Inject
	public TombolaDefaultMessageMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper, TombolaMessageTypeMapper tombolaMessageTypeMapper,
			PaymentMessageTypeMapper paymentMessageTypeMapper, VoucherMessageTypeMapper voucherMessageTypeMapper,
			ProfileMessageTypeMapper profileMessageTypeMapper, EventMessageTypeMapper eventMessageTypeMapper,
			UserMessageMessageTypeMapper userMessageMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(tombolaMessageTypeMapper);
		init(paymentMessageTypeMapper);
		init(voucherMessageTypeMapper);
		init(profileMessageTypeMapper);
		init(eventMessageTypeMapper);
		init(userMessageMessageTypeMapper);
	}

}
