package de.ascendro.f4m.service.game.selection.request;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;

public class InviteRequestInfoImpl extends RequestInfoImpl {

	private String mgiId;
	private String gameInstanceId;

	public InviteRequestInfoImpl(JsonMessage<?> sourceMessage, SessionWrapper sourceSession,
			String mgiId) {
		super(sourceMessage, sourceSession);
		this.mgiId = mgiId;
	}

	public String getMgiId() {
		return mgiId;
	}

	public void setMgiId(String mgiId) {
		this.mgiId = mgiId;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}
	
	public String getUserId(){
		return getSourceMessage().getClientInfo().getUserId();
	}

}
