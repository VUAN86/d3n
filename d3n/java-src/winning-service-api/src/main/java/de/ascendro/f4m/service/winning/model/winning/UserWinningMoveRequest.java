package de.ascendro.f4m.service.winning.model.winning;

import de.ascendro.f4m.service.winning.model.MoveRequest;

public class UserWinningMoveRequest extends MoveRequest {

	public UserWinningMoveRequest() {
	}

	public UserWinningMoveRequest(String sourceUserId, String targetUserId) {
		super(sourceUserId, targetUserId);
	}

}
