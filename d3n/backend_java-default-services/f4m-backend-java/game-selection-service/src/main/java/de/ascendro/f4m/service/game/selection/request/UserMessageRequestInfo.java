package de.ascendro.f4m.service.game.selection.request;

import de.ascendro.f4m.service.request.RequestInfoImpl;

public class UserMessageRequestInfo extends RequestInfoImpl {

	private String userId;
	private String mgiId;

	public UserMessageRequestInfo(String userId, String mgiId) {
		super();
		this.userId = userId;
		this.mgiId = mgiId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getMgiId() {
		return mgiId;
	}

	public void setMgiId(String mgiId) {
		this.mgiId = mgiId;
	}
}
