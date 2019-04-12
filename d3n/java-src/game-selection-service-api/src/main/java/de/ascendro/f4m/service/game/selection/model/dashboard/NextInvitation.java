package de.ascendro.f4m.service.game.selection.model.dashboard;

import java.time.ZonedDateTime;

import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;

public class NextInvitation {
	private String gameId;
	private String multiplayerGameInstanceId;
	private String gameTitle;
	private ZonedDateTime startDateTime;
	private ZonedDateTime playDateTime;
	private ZonedDateTime expiryDateTime;
	private String inviterId;
	private ApiProfileBasicInfo inviterInfo;

	public NextInvitation() {
		// initialize empty NextInvitation
	}

	public NextInvitation(Invitation invitation) {
		this.multiplayerGameInstanceId = invitation.getMultiplayerGameInstanceId();
		this.startDateTime = invitation.getStartDateTime();
		this.playDateTime = invitation.getPlayDateTime();
		this.expiryDateTime = invitation.getExpiryDateTime();
		this.inviterInfo = invitation.getInviter();
		
		if (invitation.getGame() != null) {
			this.gameId = invitation.getGame().getId();
			this.gameTitle = invitation.getGame().getTitle();
		}
		if (invitation.getInviter() != null) {
			this.inviterId = invitation.getInviter().getUserId();
		}
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	public String getGameTitle() {
		return gameTitle;
	}

	public void setGameTitle(String gameTitle) {
		this.gameTitle = gameTitle;
	}

	public ZonedDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(ZonedDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	public ZonedDateTime getPlayDateTime() {
		return playDateTime;
	}

	public void setPlayDateTime(ZonedDateTime playDateTime) {
		this.playDateTime = playDateTime;
	}

	public ZonedDateTime getExpiryDateTime() {
		return expiryDateTime;
	}

	public void setExpiryDateTime(ZonedDateTime expiryDateTime) {
		this.expiryDateTime = expiryDateTime;
	}

	public String getInviterId() {
		return inviterId;
	}

	public void setInviterId(String inviterId) {
		this.inviterId = inviterId;
	}

	public ApiProfileBasicInfo getInviterInfo() {
		return inviterInfo;
	}

	public void setInviterInfo(ApiProfileBasicInfo inviterInfo) {
		this.inviterInfo = inviterInfo;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NextInvitation [gameId=");
		builder.append(gameId);
		builder.append(", multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId);
		builder.append(", gameTitle=");
		builder.append(gameTitle);
		builder.append(", startDateTime=");
		builder.append(startDateTime);
		builder.append(", playDateTime=");
		builder.append(playDateTime);
		builder.append(", expiryDateTime=");
		builder.append(expiryDateTime);
		builder.append(", inviterId=");
		builder.append(inviterId);
		builder.append(", inviterInfo=");
		builder.append(inviterInfo);
		builder.append("]");
		return builder.toString();
	}

}
