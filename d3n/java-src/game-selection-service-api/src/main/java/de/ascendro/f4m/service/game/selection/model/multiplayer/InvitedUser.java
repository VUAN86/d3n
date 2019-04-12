package de.ascendro.f4m.service.game.selection.model.multiplayer;

public class InvitedUser {

	private String userId;
	private String status;

	public InvitedUser(String userId, String status) {
		this.userId = userId;
		this.status = status;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
