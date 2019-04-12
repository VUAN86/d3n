package de.ascendro.f4m.service.payment.model.internal;

public class PaymentDetails {
	private String appId;
	private String gameId;
	private String multiplayerGameInstanceId;
	private String gameInstanceId;
	private String winningComponentId;
	private String userWinningComponentId;
	private String promocodeId;
	private String additionalInfo;
	private String voucherId;
	private String tombolaId;
	private String fyberTransactionId;
	private Integer tombolaTicketsAmount; // equal to the number of tickets


	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public String getWinningComponentId() {
		return winningComponentId;
	}

	public void setWinningComponentId(String winningComponentId) {
		this.winningComponentId = winningComponentId;
	}

	public String getUserWinningComponentId() {
		return userWinningComponentId;
	}

	public void setUserWinningComponentId(String userWinningComponentId) {
		this.userWinningComponentId = userWinningComponentId;
	}

	public String getPromocodeId() {
		return promocodeId;
	}

	public void setPromocodeId(String promocodeId) {
		this.promocodeId = promocodeId;
	}

	public String getVoucherId() {
		return voucherId;
	}

	public void setVoucherId(String voucherId) {
		this.voucherId = voucherId;
	}

	public String getTombolaId() {
		return tombolaId;
	}

	public void setTombolaId(String tombolaId) {
		this.tombolaId = tombolaId;
	}

	public Integer getTombolaTicketsAmount() {
		return tombolaTicketsAmount;
	}

	public void setTombolaTicketsAmount(Integer tombolaTicketsAmount) {
		this.tombolaTicketsAmount = tombolaTicketsAmount;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public String getFyberTransactionId() {
		return fyberTransactionId;
	}

	public void setFyberTransactionId(String fyberTransactionId) {
		this.fyberTransactionId = fyberTransactionId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PaymentDetails [appId=").append(appId);
		builder.append(", gameId=").append(gameId);
		builder.append(", multiplayerGameInstanceId=").append(multiplayerGameInstanceId);
		builder.append(", gameInstanceId=").append(gameInstanceId);
		builder.append(", winningComponentId=").append(winningComponentId);
		builder.append(", voucherId=").append(voucherId);
		builder.append(", tombolaId=").append(tombolaId);
		builder.append(", fyberTransactionId=").append(fyberTransactionId);
		builder.append(", tombolaTicketsAmount=").append(tombolaTicketsAmount);
		builder.append(", additionalInfo=").append(additionalInfo);
		builder.append("]");
		return builder.toString();
	}
}
