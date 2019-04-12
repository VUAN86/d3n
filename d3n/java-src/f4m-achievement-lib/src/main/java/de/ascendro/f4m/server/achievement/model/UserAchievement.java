package de.ascendro.f4m.server.achievement.model;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.util.DateTimeUtil;

public class UserAchievement {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserAchievement.class);

	private String userId;
	private String achievementId;
	private String tenantId;
	private ZonedDateTime limitDate;
	private Map<String, Boolean> badgesWonStatus;
	private ZonedDateTime startDate;
	private ZonedDateTime completedOn;

	public UserAchievement() {
		// Empty constructor
	}

	public UserAchievement(Achievement achievement, String userId) {
		this.setUserId(userId);
		this.setAchievementId(achievement.getId());
		Map<String, Boolean> badgesWonStatus = new HashMap<>();
		for(String badgeId : achievement.getBadges()) {
			badgesWonStatus.put(badgeId, false);
		}
		this.setBadgesWonStatus(badgesWonStatus);
		this.setStartDate(DateTimeUtil.getCurrentDateTime());
		this.setTenantId(achievement.getTenantId());
		// @Todo : is the time in days or is it something else?
		if(achievement.isTimeLimit()) {
			try {
				Integer timePeriod = Integer.parseInt(achievement.getTimePeriod());
				this.setLimitDate(calculateLimitDate(timePeriod));
			} catch (NumberFormatException e) {
				LOGGER.debug("Achievement {} time period ({}) is not an integer!", achievement.getId(),
						achievement.getTimePeriod(), e);
			}
		}
	}

	private ZonedDateTime calculateLimitDate(Integer timePeriod) {
		return this.getStartDate().plusDays(timePeriod);
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getAchievementId() {
		return achievementId;
	}

	public void setAchievementId(String achievementId) {
		this.achievementId = achievementId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public ZonedDateTime getLimitDate() {
		return limitDate;
	}

	public void setLimitDate(ZonedDateTime limitDate) {
		this.limitDate = limitDate;
	}

	public Map<String, Boolean> getBadgesWonStatus() {
		return badgesWonStatus;
	}

	public void setBadgesWonStatus(Map<String, Boolean> badgesWonStatus) {
		this.badgesWonStatus = badgesWonStatus;
	}

	public ZonedDateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(ZonedDateTime startDate) {
		this.startDate = startDate;
	}

	public ZonedDateTime getCompletedOn() {
		return completedOn;
	}

	public void setCompletedOn(ZonedDateTime completedOn) {
		this.completedOn = completedOn;
	}

	public boolean isUserAchievementExpired() {
		return this.getLimitDate() != null
				&& !this.getLimitDate().isAfter(DateTimeUtil.getCurrentDateTime());
	}

	public boolean isAchievementWon() {
		return badgesWonStatus.values().stream().allMatch(won -> won);
	}
}
