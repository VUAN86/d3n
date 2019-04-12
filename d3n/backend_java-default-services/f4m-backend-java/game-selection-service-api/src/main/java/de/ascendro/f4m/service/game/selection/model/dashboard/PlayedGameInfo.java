package de.ascendro.f4m.service.game.selection.model.dashboard;

import java.time.ZonedDateTime;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class PlayedGameInfo {

	private String gameId;
	private GameType type;
	private String title;
	private ZonedDateTime dateTime;
	private PlayedDuelInfo duelInfo;
	private PlayedTournamentInfo tournamentInfo;
	private boolean rematch;

	public PlayedGameInfo() {
		// initialize empty object
	}

	public PlayedGameInfo(String gameId, GameType type, String title, boolean rematch) {
		this.dateTime = DateTimeUtil.getCurrentDateTime();
		this.gameId = gameId;
		this.type = type;
		this.title = title;
		this.rematch = rematch;
	}

	public PlayedGameInfo(String gameId, GameType type, String title, PlayedDuelInfo duelInfo, boolean rematch) {
		this(gameId, type, title, rematch);
		this.duelInfo = duelInfo;
	}
	
	public PlayedGameInfo(String gameId, GameType type, String title, PlayedTournamentInfo tournamentInfo) {
		this(gameId, type, title, false);
		this.tournamentInfo = tournamentInfo;
	}
	
	public boolean tournamentHasPlacement() {
		return tournamentInfo != null && tournamentInfo.getPlacement() != 0;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public GameType getType() {
		return type;
	}

	public void setType(GameType type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ZonedDateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(ZonedDateTime dateTime) {
		this.dateTime = dateTime;
	}

	public PlayedDuelInfo getDuelInfo() {
		return duelInfo;
	}

	public void setDuelInfo(PlayedDuelInfo duelInfo) {
		this.duelInfo = duelInfo;
	}

	public PlayedTournamentInfo getTournamentInfo() {
		return tournamentInfo;
	}

	public void setTournamentInfo(PlayedTournamentInfo tournamentInfo) {
		this.tournamentInfo = tournamentInfo;
	}

	public void setRematch(boolean rematch) {
		this.rematch = rematch;
	}
	
	public boolean isRematch() {
		return rematch;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlayedGameInfo [gameId=");
		builder.append(gameId);
		builder.append(", type=");
		builder.append(type);
		builder.append(", title=");
		builder.append(title);
		builder.append(", dateTime=");
		builder.append(dateTime);
		builder.append(", duelInfo=");
		builder.append(duelInfo);
		builder.append(", tournamentInfo=");
		builder.append(tournamentInfo);
		builder.append(", rematch=");
		builder.append(rematch);
		builder.append("]");
		return builder.toString();
	}

}
