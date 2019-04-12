package de.ascendro.f4m.service.analytics.module.achievement;

import java.util.List;

import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.analytics.EventContent;

public interface IEventProcessor {

	List<Badge> getAssociatedBadges(EventContent eventContent);

	List<Achievement> getAssociatedAchievements(String tenantId, Badge badge);

	List<Achievement> getAssociatedAchievements(String tenantId, List<Badge> badges);

}