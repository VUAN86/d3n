package de.ascendro.f4m.server.achievement.model;

import java.util.List;

public class Badge {

	private String id;
	private String tenantId;
	private String name;
	private String description;
	private BadgeType type;
	private AchievementStatus status;
	private BadgePeriod period;
	private String temporaryTime;
	private String image;
	private boolean reward;
	private Integer bonusPointsReward;
	private Integer creditReward;
	private Float paymentMultiplier;
	private List<String> accessRules;
	private List<ExecutionRule> rules;
	private List<String> battleRules;
	private List<MessagingType> messaging;
	private boolean uniquePerUser;
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

	public BadgeType getType() {
		return type;
	}

	public void setType(BadgeType type) {
		this.type = type;
	}

	public AchievementStatus getStatus() {
		return status;
	}

	public void setStatus(AchievementStatus status) {
		this.status = status;
	}

	public BadgePeriod getPeriod() {
		return period;
	}

	public void setPeriod(BadgePeriod period) {
		this.period = period;
	}

	public String getTemporaryTime() {
		return temporaryTime;
	}

	public void setTemporaryTime(String temporaryTime) {
		this.temporaryTime = temporaryTime;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
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

	public List<ExecutionRule> getRules() {
		return rules;
	}

	public void setRules(List<ExecutionRule> rules) {
		this.rules = rules;
	}

	public List<String> getBattleRules() {
		return battleRules;
	}

	public void setBattleRules(List<String> battleRules) {
		this.battleRules = battleRules;
	}

	public List<MessagingType> getMessaging() {
		return messaging;
	}

	public void setMessaging(List<MessagingType> messaging) {
		this.messaging = messaging;
	}

	public boolean isUniquePerUser() {
		return uniquePerUser;
	}

	public void setUniquePerUser(boolean uniquePerUser) {
		this.uniquePerUser = uniquePerUser;
	}

	public String getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(String createdOn) {
		this.createdOn = createdOn;
	}
}
