package de.ascendro.f4m.service.game.engine.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypeMapper;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypeMapper;
import de.ascendro.f4m.service.voucher.VoucherMessageTypeMapper;
import de.ascendro.f4m.service.winning.WinningMessageTypeMapper;

public class GameEngineDefaultMessageTypeMapper extends DefaultJsonMessageMapper {

	private static final long serialVersionUID = 7275119994526226690L;

	@Inject
	public GameEngineDefaultMessageTypeMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
											  GatewayMessageTypeMapper gatewayMessageTypeMapper, GameEngineMessageTypeMapper gameEngineMessageTypeMapper,
											  EventMessageTypeMapper eventMessageTypeMapper, ResultEngineMessageTypeMapper resultEngineMessageTypeMapper,
											  PaymentMessageTypeMapper paymentMessageTypeMapper, WinningMessageTypeMapper winningMessageTypeMapper,
											  VoucherMessageTypeMapper voucherMessageTypeMapper,
											  GameSelectionMessageTypeMapper gameSelectionMessageTypeMapper,
											  ProfileMessageTypeMapper profileMessageTypeMapper) {

		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(eventMessageTypeMapper);
		init(gameEngineMessageTypeMapper);
		init(resultEngineMessageTypeMapper);
		init(paymentMessageTypeMapper);
		init(winningMessageTypeMapper);
		init(voucherMessageTypeMapper);
		init(gameSelectionMessageTypeMapper);
		init(profileMessageTypeMapper);
	}

}
