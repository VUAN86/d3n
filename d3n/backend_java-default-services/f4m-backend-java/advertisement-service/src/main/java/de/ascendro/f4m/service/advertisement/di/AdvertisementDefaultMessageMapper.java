package de.ascendro.f4m.service.advertisement.di;

import com.google.inject.Inject;

import de.ascendro.f4m.advertisement.AdvertisementMessageTypeMapper;
import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;

public class AdvertisementDefaultMessageMapper extends DefaultJsonMessageMapper {
	private static final long serialVersionUID = -478698614002483726L;

	@Inject
	public AdvertisementDefaultMessageMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper, PaymentMessageTypeMapper paymentMessageTypeMapper,
			AdvertisementMessageTypeMapper advertisementMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(advertisementMessageTypeMapper);
		init(paymentMessageTypeMapper);
	}
}
