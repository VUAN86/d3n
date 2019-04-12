package de.ascendro.f4m.service.result.engine.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypeMapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypeMapper;
import de.ascendro.f4m.service.voucher.VoucherMessageTypeMapper;

public class ResultEngineDefaultMessageMapper extends DefaultJsonMessageMapper {

    private static final long serialVersionUID = -478698614002483725L;

	@Inject
	public ResultEngineDefaultMessageMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper, ProfileMessageTypeMapper profileMessageTypeMapper,
			VoucherMessageTypeMapper voucherMessageTypeMapper, ResultEngineMessageTypeMapper resultMessageTypeMapper,
			PaymentMessageTypeMapper paymentMessageTypeMapper, UserMessageMessageTypeMapper userMessageMessageTypeMapper,
			EventMessageTypeMapper eventMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(resultMessageTypeMapper);
		init(profileMessageTypeMapper);
		init(userMessageMessageTypeMapper);
		init(voucherMessageTypeMapper);
		init(paymentMessageTypeMapper);
		init(eventMessageTypeMapper);
	}

}
