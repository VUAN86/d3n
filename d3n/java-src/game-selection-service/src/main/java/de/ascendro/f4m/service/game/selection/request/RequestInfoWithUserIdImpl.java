package de.ascendro.f4m.service.game.selection.request;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;

public class RequestInfoWithUserIdImpl extends RequestInfoImpl {

	private String userId;

	public RequestInfoWithUserIdImpl(JsonMessage<?> sourceMessage, SessionWrapper sourceSession, String userId) {
		super(sourceMessage, sourceSession);
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

}
