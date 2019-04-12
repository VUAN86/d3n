package de.ascendro.f4m.service.payment.model;

import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;

public class PaymentDetailsBuilder {
	private String appId;
	private String gameId;
	private String winningComponentId;
	private String additionalInfo;
	private String gameInstanceId;
	private String multiplayerGameInstanceId;
	private String promocodeId;
	private String userWinningComponentId;
	private String voucherId;
	private String tombolaId;
	private Integer tombolaTicketsAmount;
	private String fyberTransactionId;

	public PaymentDetailsBuilder appId(String appId) {
		this.appId = appId;
		return this;
	}

	public PaymentDetailsBuilder gameId(String gameId) {
		this.gameId = gameId;
		return this;
	}

	public PaymentDetailsBuilder winningComponentId(String winningComponentId) {
		this.winningComponentId = winningComponentId;
		return this;
	}

	public PaymentDetailsBuilder additionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
		return this;
	}
	
	public PaymentDetailsBuilder gameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
		return this;
	}
	
	public PaymentDetailsBuilder multiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
		return this;
	}
	
	public PaymentDetailsBuilder promocodeId(String promocodeId) {
		this.promocodeId = promocodeId;
		return this;
	}
	
	public PaymentDetailsBuilder userWinningComponentId(String userWinningComponentId) {
		this.userWinningComponentId = userWinningComponentId;
		return this;
	}
	
	public PaymentDetailsBuilder voucherId(String voucherId) {
		this.voucherId = voucherId;
		return this;
	}

	public PaymentDetailsBuilder tombolaId(String tombolaId) {
		this.tombolaId = tombolaId;
		return this;
	}

	public PaymentDetailsBuilder tombolaTicketsAmount(Integer tombolaTicketsAmount) {
		this.tombolaTicketsAmount = tombolaTicketsAmount;
		return this;
	}

	public PaymentDetailsBuilder fyberTransactionId(String fyberTransactionId) {
		this.fyberTransactionId = fyberTransactionId;
		return this;
	}
	
	public PaymentDetails build() {
		PaymentDetails paymentDetails = new PaymentDetails();
		paymentDetails.setAppId(appId);
		paymentDetails.setGameId(gameId);
		paymentDetails.setWinningComponentId(winningComponentId);
		paymentDetails.setAdditionalInfo(additionalInfo);
		paymentDetails.setGameInstanceId(gameInstanceId);
		paymentDetails.setMultiplayerGameInstanceId(multiplayerGameInstanceId);
		paymentDetails.setPromocodeId(promocodeId);
		paymentDetails.setUserWinningComponentId(userWinningComponentId);
		paymentDetails.setVoucherId(voucherId);
		paymentDetails.setTombolaId(tombolaId);
		paymentDetails.setTombolaTicketsAmount(tombolaTicketsAmount);
		paymentDetails.setFyberTransactionId(fyberTransactionId);
		return paymentDetails;
	}
}
