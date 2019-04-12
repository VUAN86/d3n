package de.ascendro.f4m.service.voucher.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypeMapper;
import de.ascendro.f4m.service.voucher.VoucherMessageTypeMapper;

public class VoucherDefaultMessageMapper extends DefaultJsonMessageMapper {
	private static final long serialVersionUID = -478698614002483725L;

	@Inject
	public VoucherDefaultMessageMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper, VoucherMessageTypeMapper voucherMessageTypeMapper,
			ProfileMessageTypeMapper profileMessageTypeMapper,
			UserMessageMessageTypeMapper userMessageMessageTypeMapper,
			PaymentMessageTypeMapper paymentMessageTypeMapper, EventMessageTypeMapper eventMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(eventMessageTypeMapper);
		init(profileMessageTypeMapper);
		init(voucherMessageTypeMapper);
		init(userMessageMessageTypeMapper);
		init(paymentMessageTypeMapper);
	}

}
