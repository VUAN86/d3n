package de.ascendro.f4m.service.result.engine.model.get;

import java.util.List;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

/** Request for getting game history list for a user. */
public class CompletedGameListForUserRequest extends CompletedGameListRequest implements JsonMessageContent {

	@JsonRequiredNullable
	private String userId;

	public CompletedGameListForUserRequest(String userId, int limit, long offset, List<GameType> gameTypes, String dateFrom, String dateTo) {
		setUserId(userId);
		setLimit(limit);
		setOffset(offset);
		setGameTypes(gameTypes);
		setDateFrom(dateFrom);
		setDateTo(dateTo);
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CompletedGameListForUserRequest [userId=");
		builder.append(userId);
		builder.append(", CompletedGameListRequest=");
		builder.append(super.toString());
		builder.append("]");
		return builder.toString();
	}

}
