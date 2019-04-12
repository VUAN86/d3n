package de.ascendro.f4m.server.request.jackpot;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.MessageSource;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;

public class PaymentCreateJackpotRequestInfo extends RequestInfoImpl{

	private String mgiId;

	public PaymentCreateJackpotRequestInfo(String mgiId, JsonMessage<?> sourceMessage, MessageSource messageSource) {
		super(sourceMessage, messageSource);
		this.mgiId = mgiId;
	}
	
	public String getMgiId() {
		return mgiId;
	}

}
