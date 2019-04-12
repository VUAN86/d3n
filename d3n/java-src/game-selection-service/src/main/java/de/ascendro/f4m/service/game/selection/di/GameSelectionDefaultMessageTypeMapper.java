package de.ascendro.f4m.service.game.selection.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.auth.AuthMessageTypeMapper;
import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypeMapper;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypeMapper;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.payment.PaymentMessageTypeMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypeMapper;

public class GameSelectionDefaultMessageTypeMapper extends DefaultJsonMessageMapper {

	private static final long serialVersionUID = -8101372091358891599L;

	@Inject
	public GameSelectionDefaultMessageTypeMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper,
			AuthMessageTypeMapper authMessageTypeMapper,
			GameSelectionMessageTypeMapper gameSelectionMessageTypeMapper,
			FriendManagerMessageTypeMapper friendManagerMessageTypeMapper,
			UserMessageMessageTypeMapper userMessageMessageTypeMapper,
			PaymentMessageTypeMapper paymentMessageTypeMapper, ProfileMessageTypeMapper profileMessageTypeMapper,
			EventMessageTypeMapper eventMessageTypeMapper, GameEngineMessageTypeMapper gameEngineMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(gameSelectionMessageTypeMapper);
		init(friendManagerMessageTypeMapper);
		init(userMessageMessageTypeMapper);
		init(paymentMessageTypeMapper);
		init(profileMessageTypeMapper);
		init(eventMessageTypeMapper);
		init(gameEngineMessageTypeMapper);
		init(authMessageTypeMapper);
	}

}
