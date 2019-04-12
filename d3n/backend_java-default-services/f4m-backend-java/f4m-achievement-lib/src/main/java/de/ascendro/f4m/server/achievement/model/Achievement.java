package de.ascendro.f4m.server.achievement.model;

import java.util.List;

public class Achievement {

	private String id;
	private String tenantId;
	private String name;
	private String description;
	private AchievementStatus status;
	private String image;
	private boolean timeLimit;
	private String timePeriod;
	private boolean reward;
	private Integer bonusPointsReward;
	private Integer creditReward;
	private Float paymentMultiplier;
	private List<String> accessRules;
	private List<String> badges;
	private List<MessagingType> messaging;
	private String createdOn;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public AchievementStatus getStatus() {
		return status;
	}

	public void setStatus(AchievementStatus status) {
		this.status = status;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public boolean isTimeLimit() {
		return timeLimit;
	}

	public void setTimeLimit(boolean timeLimit) {
		this.timeLimit = timeLimit;
	}

	public String getTimePeriod() {
		return timePeriod;
	}

	public void setTimePeriod(String timePeriod) {
		this.timePeriod = timePeriod;
	}

	public boolean isReward() {
		return reward;
	}

	public void setReward(boolean reward) {
		this.reward = reward;
	}

	public Integer getBonusPointsReward() {
		return bonusPointsReward;
	}

	public void setBonusPointsReward(Integer bonusPointsReward) {
		this.bonusPointsReward = bonusPointsReward;
	}

	public Integer getCreditReward() {
		return creditReward;
	}

	public void setCreditReward(Integer creditReward) {
		this.creditReward = creditReward;
	}

	public Float getPaymentMultiplier() {
		return paymentMultiplier;
	}

	public void setPaymentMultiplier(Float paymentMultiplier) {
		this.paymentMultiplier = paymentMultiplier;
	}

	public List<String> getAccessRules() {
		return accessRules;
	}

	public void setAccessRules(List<String> accessRules) {
		this.accessRules = accessRules;
	}

	public List<String> getBadges() {
		return badges;
	}

	public void setBadges(List<String> badges) {
		this.badges = badges;
	}

	public List<MessagingType> getMessaging() {
		return messaging;
	}

	public void setMessaging(List<MessagingType> messaging) {
		this.messaging = messaging;
	}

	public String getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(String createdOn) {
		this.createdOn = createdOn;
	}

}
