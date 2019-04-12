package de.ascendro.f4m.service.winning.client;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;

public class PaymentRequestInfo extends RequestInfoImpl {

	private String winningComponentId;
	private String transactionLogId;
	private boolean isEligibleToWinnings;
	private boolean isEligibleToComponent;
	private String gameId;

	public PaymentRequestInfo(JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper sourceSession, String gameId) {
		super(sourceMessage, sourceSession);
		this.gameId = gameId;
	}

	public String getWinningComponentId() {
		return winningComponentId;
	}

	public void setWinningComponentId(String winningComponentId) {
		this.winningComponentId = winningComponentId;
	}

	public String getTransactionLogId() {
		return transactionLogId;
	}

	public void setTransactionLogId(String transactionLogId) {
		this.transactionLogId = transactionLogId;
	}

	public void setEligibleToComponent(boolean eligibleToComponent) {
		isEligibleToComponent = eligibleToComponent;
	}

	public boolean isEligibleToComponent() {

		return isEligibleToComponent;
	}

	public boolean isEligibleToWinnings() {
		return isEligibleToWinnings;
	}

	public void setEligibleToWinnings(boolean isEligibleToWinnings) {
		this.isEligibleToWinnings = isEligibleToWinnings;
	}
	
	public String getGameId() {
		return gameId;
	}

}
