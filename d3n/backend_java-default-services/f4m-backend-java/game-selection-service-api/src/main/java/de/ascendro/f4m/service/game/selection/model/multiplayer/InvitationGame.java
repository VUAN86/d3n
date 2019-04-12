package de.ascendro.f4m.service.game.selection.model.multiplayer;

import java.util.Arrays;

import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.Jackpot;

public class InvitationGame {

	private String id;
	private String title;
	private String description;
	private String endDateTime;
	private GameType type;
	private String[] assignedPools;
	private String[] assignedPoolsColors;
	private String[] assignedPoolsNames;
	private String[] assignedPoolsIcons;
	private Jackpot jackpot;

	public InvitationGame() {
		// initialize empty object
	}

	public InvitationGame(String id, String title, String description, GameType type) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.type = type;
	}
	
	public InvitationGame(Game game) {
		id = game.getGameId();
		title = game.getTitle();
		description = game.getDescription();
		endDateTime = game.getEndDateTime().toString();
		type = game.getType();
		assignedPools = game.getAssignedPools();
		assignedPoolsColors = game.getAssignedPoolsColors();
		assignedPoolsNames = game.getAssignedPoolsNames();
		assignedPoolsIcons = game.getAssignedPoolsIcons();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(String endDateTime) {
		this.description = endDateTime;
	}

	public GameType getType() {
		return type;
	}

	public void setType(GameType type) {
		this.type = type;
	}

	public String[] getAssignedPools() {
		return assignedPools;
	}

	public void setAssignedPools(String[] assignedPools) {
		this.assignedPools = assignedPools;
	}

	public String[] getAssignedPoolsColors() {
		return assignedPoolsColors;
	}

	public void setAssignedPoolsColors(String[] assignedPoolsColors) {
		this.assignedPoolsColors = assignedPoolsColors;
	}

	public String[] getAssignedPoolsNames() {
		return assignedPoolsNames;
	}

	public void setAssignedPoolsNames(String[] assignedPoolsNames) {
		this.assignedPoolsNames = assignedPoolsNames;
	}

	public String[] getAssignedPoolsIcons() {
		return assignedPoolsIcons;
	}

	public void setAssignedPoolsIcons(String[] assignedPoolsIcons) {
		this.assignedPoolsIcons = assignedPoolsIcons;
	}

	@Override
	public String toString() {
		return "InvitationGame [id=" +
				id +
				", title=" +
				title +
				", description=" +
				description +
				", type=" +
				type +
				", assignedPools=" +
				Arrays.toString(assignedPools) +
				", assignedPoolsColors=" +
				Arrays.toString(assignedPoolsColors) +
				", assignedPoolsNames=" +
				Arrays.toString(assignedPoolsNames) +
				", assignedPoolsIcons=" +
				Arrays.toString(assignedPoolsIcons) +
				", endDateTime=" +
				endDateTime +
				", jackpot=" +
				(jackpot != null ? jackpot.toString() : "null")+
				"]";
	}

	public Jackpot getJackpot() {
		return jackpot;
	}

	public void setJackpot(Jackpot jackpot) {
		this.jackpot = jackpot;
	}

}
