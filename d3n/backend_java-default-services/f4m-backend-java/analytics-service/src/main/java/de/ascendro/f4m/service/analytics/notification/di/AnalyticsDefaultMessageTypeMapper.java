package de.ascendro.f4m.service.analytics.notification.di;


import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypeMapper;

public class AnalyticsDefaultMessageTypeMapper extends DefaultJsonMessageMapper {
    private static final long serialVersionUID = 2947536629279814469L;

    @Inject
	public AnalyticsDefaultMessageTypeMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
											 PaymentMessageTypeMapper paymentMessageTypeMapper,
											 GatewayMessageTypeMapper gatewayMessageTypeMapper,
											 UserMessageMessageTypeMapper userMessageMessageTypeMapper,
											 EventMessageTypeMapper eventMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(userMessageMessageTypeMapper);
		init(paymentMessageTypeMapper);
		init(eventMessageTypeMapper);
	}
}
