package de.ascendro.f4m.server.achievement.model;

public class UserProgressES {

	private String userId;
	private String tenantId;
	private Integer numOfGameBadges;
	private Integer numOfCommunityBadges;

	public UserProgressES() {
		super();
	}

	public UserProgressES(String userId, String tenantId, Integer numOfGameBadges,
			Integer numOfCommunityBadges) {
		super();
		this.userId = userId;
		this.tenantId = tenantId;
		this.numOfGameBadges = numOfGameBadges;
		this.numOfCommunityBadges = numOfCommunityBadges;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Integer getNumOfGameBadges() {
		return numOfGameBadges;
	}

	public void setNumOfGameBadges(Integer numOfGameBadges) {
		this.numOfGameBadges = numOfGameBadges;
	}

	public Integer getNumOfCommunityBadges() {
		return numOfCommunityBadges;
	}

	public void setNumOfCommunityBadges(Integer numOfCommunityBadges) {
		this.numOfCommunityBadges = numOfCommunityBadges;
	}
}
