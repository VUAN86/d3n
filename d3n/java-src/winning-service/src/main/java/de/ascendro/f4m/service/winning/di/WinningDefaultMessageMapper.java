package de.ascendro.f4m.service.winning.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypeMapper;
import de.ascendro.f4m.service.voucher.VoucherMessageTypeMapper;
import de.ascendro.f4m.service.winning.WinningMessageTypeMapper;

public class WinningDefaultMessageMapper extends DefaultJsonMessageMapper {
	private static final long serialVersionUID = -478698614002483725L;

	@Inject
	public WinningDefaultMessageMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper, WinningMessageTypeMapper winningMessageTypeMapper,
			VoucherMessageTypeMapper voucherMessageTypeMapper,
			ResultEngineMessageTypeMapper resultEngineMessageTypeMapper,
			PaymentMessageTypeMapper paymentMessageTypeMapper,
			EventMessageTypeMapper eventMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(eventMessageTypeMapper);
		init(resultEngineMessageTypeMapper);
		init(paymentMessageTypeMapper);
		init(winningMessageTypeMapper);
		init(voucherMessageTypeMapper);
	}

}
