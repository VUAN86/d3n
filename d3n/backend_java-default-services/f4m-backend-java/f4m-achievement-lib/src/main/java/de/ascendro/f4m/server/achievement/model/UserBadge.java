package de.ascendro.f4m.server.achievement.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.util.DateTimeUtil;

public class UserBadge {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserBadge.class);

	private String userId;
	private String badgeId;
	private String tenantId;
	private String creatorId;
	private List<String> history;
	private ZonedDateTime createdOn;
	private ZonedDateTime expiresOn;

	private Integer wonCount;
	private List<RuleProgress> rules;
	private List<String> battleRules;

	public UserBadge() {
		// Empty Constructor
	}

	public UserBadge(Badge badge, String userId, String tenantId) {
		this.setUserId(userId);
		this.setTenantId(tenantId);
		this.setBadgeId(badge.getId());
		this.setCreatedOn(DateTimeUtil.getCurrentDateTime());
		this.setCreatorId(userId);
		this.setRules(this.copyBadgeRules(badge.getRules()));

		if(badge.getPeriod().equals(BadgePeriod.TEMPORARY)) {
			// @Todo : is the time in days or is it something else?
			try {
				final Integer temporaryTime = Integer.parseInt(badge.getTemporaryTime());
				this.setExpiresOn(this.calculateLimitDate(temporaryTime));
			} catch (NumberFormatException e) {
				LOGGER.debug("Badge {} temporary time ({}) is not an integer!", badge.getId(), badge.getTemporaryTime(), e);
			}
		}

		this.setWonCount(0);
	}

	private ZonedDateTime calculateLimitDate(Integer temporaryTime) {
		return this.getCreatedOn().plusDays(temporaryTime);
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getBadgeId() {
		return badgeId;
	}

	public void setBadgeId(String badgeId) {
		this.badgeId = badgeId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}

	public List<String> getHistory() {
		return history;
	}

	public void setHistory(List<String> history) {
		this.history = history;
	}

	public ZonedDateTime getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(ZonedDateTime createdOn) {
		this.createdOn = createdOn;
	}

	public ZonedDateTime getExpiresOn() {
		return expiresOn;
	}

	public void setExpiresOn(ZonedDateTime expiresOn) {
		this.expiresOn = expiresOn;
	}

	public Integer getWonCount() {
		return wonCount;
	}

	public void setWonCount(Integer wonCount) {
		this.wonCount = wonCount;
	}

	public List<RuleProgress> getRules() {
		return rules;
	}

	public void setRules(List<RuleProgress> rules) {
		this.rules = rules;
	}

	public List<String> getBattleRules() {
		return battleRules;
	}

	public void setBattleRules(List<String> battleRules) {
		this.battleRules = battleRules;
	}

	private List<RuleProgress> copyBadgeRules(List<ExecutionRule> rules) {
		List<RuleProgress> progress = new ArrayList<>();
		for(ExecutionRule rule : rules) {
			progress.add(new RuleProgress(rule));
		}

		return progress;
	}

}
