package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.winning.model.MoveRequest;

public class UserWinningComponentMoveRequest extends MoveRequest {

	public UserWinningComponentMoveRequest() {
	}

	public UserWinningComponentMoveRequest(String sourceUserId, String targetUserId) {
		super(sourceUserId, targetUserId);
	}

}
