package de.ascendro.f4m.service.achievement.di;

import javax.inject.Inject;

import de.ascendro.f4m.service.achievement.AchievementMessageTypeMapper;
import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;

public class AchievementDefaultMessageMapper extends DefaultJsonMessageMapper {
	private static final long serialVersionUID = -478698614002483725L;

	@Inject
	public AchievementDefaultMessageMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
			GatewayMessageTypeMapper gatewayMessageTypeMapper,
			AchievementMessageTypeMapper achievementMessageTypeMapper, EventMessageTypeMapper eventMessageTypeMapper) {
		super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
		init(eventMessageTypeMapper);
		init(achievementMessageTypeMapper);
	}

}