package de.ascendro.f4m.service.profile.client;

import java.util.concurrent.atomic.AtomicInteger;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.profile.model.get.app.GetAppConfigurationResponse;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;

public class PaymentUserRequestInfo extends RequestInfoImpl {
	private AtomicInteger numberOfExpectedResponses;
	private GetAppConfigurationResponse responseToForward;
	

	public PaymentUserRequestInfo(JsonMessage<?> sourceMessage, SessionWrapper sourceSession) {
		super(sourceMessage, sourceSession);
	}
	
	public AtomicInteger getNumberOfExpectedResponses() {
		return numberOfExpectedResponses;
	}

	public void setNumberOfExpectedResponses(AtomicInteger numberOfExpectedResponses) {
		this.numberOfExpectedResponses = numberOfExpectedResponses;
	}

	public GetAppConfigurationResponse getResponseToForward() {
		return responseToForward;
	}

	public void setResponseToForward(GetAppConfigurationResponse responseToForward) {
		this.responseToForward = responseToForward;
	}
}
