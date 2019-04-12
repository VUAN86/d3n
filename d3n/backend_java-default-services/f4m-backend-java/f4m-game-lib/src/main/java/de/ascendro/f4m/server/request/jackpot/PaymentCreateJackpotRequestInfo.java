package de.ascendro.f4m.server.request.jackpot;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;

public class PaymentCreateJackpotRequestInfo extends RequestInfoImpl{

	private String mgiId;

	public PaymentCreateJackpotRequestInfo(String mgiId, JsonMessage<?> sourceMessage, SessionWrapper sourceSession) {
		super(sourceMessage, sourceSession);
		this.mgiId = mgiId;
	}
	
	public String getMgiId() {
		return mgiId;
	}

}
