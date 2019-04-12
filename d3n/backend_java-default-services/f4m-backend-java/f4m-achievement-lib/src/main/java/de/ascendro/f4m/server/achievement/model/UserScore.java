package de.ascendro.f4m.server.achievement.model;

import java.time.ZonedDateTime;

public class UserScore {

	private String userId;
	private String tenantId;
	private String name;
	private String image;
	private Integer numOfGameBadges;
	private Integer numOfCommunityBadges;
	private ZonedDateTime startedOn;	

	public UserScore() {
		super();
	}

	public UserScore(String userId, String tenantId, String name, String image, Integer numOfGameBadges,
			Integer numOfCommunityBadges, ZonedDateTime startedOn) {
		super();
		this.userId = userId;
		this.tenantId = tenantId;
		this.name = name;
		this.image = image;
		this.numOfGameBadges = numOfGameBadges;
		this.numOfCommunityBadges = numOfCommunityBadges;
		this.startedOn = startedOn;
	}

	public UserScore(UserProgressES userES, String name, String image, ZonedDateTime startedOn) {
		super();
		this.userId = userES.getUserId();
		this.tenantId = userES.getTenantId();
		this.numOfGameBadges = userES.getNumOfGameBadges();
		this.numOfCommunityBadges = userES.getNumOfCommunityBadges();
		this.name = name;
		this.image = image;
		this.startedOn = startedOn;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public ZonedDateTime getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(ZonedDateTime startedOn) {
		this.startedOn = startedOn;
	}
}
