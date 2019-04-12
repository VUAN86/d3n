package de.ascendro.f4m.service.analytics.module.achievement.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.service.analytics.module.achievement.IEventProcessor;

/**
 * This class is not immutable. A ReadWriteLock is used to prevent concurrency
 * issues. The class allow finding of relevant badges and achievements based on
 * incoming event. The in-memory mapping is supplied by Loader and the data set
 *
 */
public class EventProcessor implements IEventProcessor {

	private final List<AchievementTenantData> tenantList = new ArrayList<>();

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
	private final Lock read = readWriteLock.readLock();
	private final Lock write = readWriteLock.writeLock();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.ascendro.f4m.service.analytics.module.achievement.IEventProcessor#
	 * getAssociatedBadges(de.ascendro.f4m.server.analytics.EventContent)
	 */
	@Override
	public List<Badge> getAssociatedBadges(EventContent eventContent) {
		String eventType = eventContent.getEventType();
		List<AchievementRule> matchingRules = Optional.ofNullable(AchievementRuleToEventMapper
				.getAchievementsForEvent(eventType)).orElse(new ArrayList<>());
		String tenantId = eventContent.getTenantId();
		read.lock();
		try {
			Function<AchievementTenantData, List<Badge>> lookupBadges = item -> item.getBadgesFor(matchingRules);
			return getData(tenantId, lookupBadges);
		} finally {
			read.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.ascendro.f4m.service.analytics.module.achievement.IEventProcessor#
	 * getAssociatedAchievements(java.lang.String,
	 * de.ascendro.f4m.server.achievement.model.Badge)
	 */
	@Override
	public List<Achievement> getAssociatedAchievements(String tenantId, Badge badge) {
		read.lock();
		try {
			Function<AchievementTenantData, List<Achievement>> lookupBadges = item -> item.getAchievementsFor(badge);
			return getData(tenantId, lookupBadges);
		} finally {
			read.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.ascendro.f4m.service.analytics.module.achievement.IEventProcessor#
	 * getAssociatedAchievements(java.lang.String, java.util.List)
	 */
	@Override
	public List<Achievement> getAssociatedAchievements(String tenantId, List<Badge> badges) {
		read.lock();
		try {
			Function<AchievementTenantData, List<Achievement>> lookupBadges = item -> item.getAchievementsFor(badges);
			return getData(tenantId, lookupBadges);
		} finally {
			read.unlock();
		}
	}

	void setTenantsData(List<AchievementTenantData> tenantList) {
		write.lock();
		try {
			this.tenantList.clear();
			this.tenantList.addAll(tenantList);
		} finally {
			write.unlock();
		}
	}

	private AchievementTenantData getTenantData(String tenantId) {
		AchievementTenantData tenantData = tenantList.parallelStream()
				.filter(tenant -> 
				tenant.getTenantId().equals(tenantId))
				.findFirst().orElse(null);
		return tenantData;
	}
	
	private <T> List<T> getData(String tenantId, Function<AchievementTenantData, List<T>> lookup) {
		AchievementTenantData tenantData = getTenantData(tenantId);
		if (null == tenantData) {
			return Collections.emptyList();
		}
		return lookup.apply(tenantData);		
	}

}
