package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.util.F4MEnumUtils;
import de.ascendro.f4m.service.winning.model.WinningComponentType;

public class UserWinningComponentAssignRequest implements JsonMessageContent {

	private String gameInstanceId;
	private String type;
	private String winningComponentId;

	public UserWinningComponentAssignRequest() {
		// Initialize empty object
	}

	public UserWinningComponentAssignRequest(String gameInstanceId, WinningComponentType type) {
		this.gameInstanceId = gameInstanceId;
		this.type = type == null ? null : type.name();
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public WinningComponentType getType() {
		return F4MEnumUtils.getEnum(WinningComponentType.class, type);
	}

	public String getWinningComponentId() {
		return winningComponentId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("UserWinningComponentAssignRequest [");
		builder.append("gameInstanceId=").append(gameInstanceId);
		builder.append(", type=").append(type);
		builder.append(", winningComponentId=").append(winningComponentId);
		builder.append("]");

		return builder.toString();
	}

}
