package de.ascendro.f4m.service.game.selection.model.game;

public class MultiplayerGameTypeConfigurationData implements GameTypeConfigurationData {

	private Integer minimumPlayerNeeded;
	private Integer gameCancellationPriorGameStart; //in minutes
	private Integer gameStartWarningMessage; //in minutes
	private Integer emailNotification; // [0,1] values
	private Integer playerGameReadiness; // in seconds
	private Integer minimumJackpotGarantie; //only for live tournaments games [optional]

	public MultiplayerGameTypeConfigurationData() {
		// init empty MultiplayerGameTypeConfigurationData
	}

	public MultiplayerGameTypeConfigurationData(Integer minimumPlayerNeeded, Integer gameCancellationPriorGameStart,
			Integer gameStartWarningMessage, Integer emailNotification, Integer playerGameReadiness,
			Integer minimumJackpotGarantie) {
		super();
		this.minimumPlayerNeeded = minimumPlayerNeeded;
		this.gameCancellationPriorGameStart = gameCancellationPriorGameStart;
		this.gameStartWarningMessage = gameStartWarningMessage;
		this.emailNotification = emailNotification;
		this.playerGameReadiness = playerGameReadiness;
		this.minimumJackpotGarantie = minimumJackpotGarantie;
	}

	public Integer getMinimumPlayerNeeded() {
		return minimumPlayerNeeded;
	}

	public void setMinimumPlayerNeeded(Integer minimumPlayerNeeded) {
		this.minimumPlayerNeeded = minimumPlayerNeeded;
	}

	public Integer getGameCancellationPriorGameStart() {
		return gameCancellationPriorGameStart;
	}

	public void setGameCancellationPriorGameStart(Integer gameCancellationPriorGameStart) {
		this.gameCancellationPriorGameStart = gameCancellationPriorGameStart;
	}

	public Integer getGameStartWarningMessage() {
		return gameStartWarningMessage;
	}

	public void setGameStartWarningMessage(Integer gameStartWarningMessage) {
		this.gameStartWarningMessage = gameStartWarningMessage;
	}

	public Integer getEmailNotification() {
		return emailNotification;
	}

	public void setEmailNotification(Integer emailNotification) {
		this.emailNotification = emailNotification;
	}

	public Integer getPlayerGameReadiness() {
		return playerGameReadiness;
	}

	public void setPlayerGameReadiness(Integer playerGameReadiness) {
		this.playerGameReadiness = playerGameReadiness;
	}

	public Integer getMinimumJackpotGarantie() {
		return minimumJackpotGarantie;
	}

	public void setMinimumJackpotGarantie(Integer minimumJackpotGarantie) {
		this.minimumJackpotGarantie = minimumJackpotGarantie;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MultiplayerGameTypeConfigurationData [minimumPlayerNeeded=");
		builder.append(minimumPlayerNeeded);
        builder.append(", gameCancellationPriorGameStart=").append(getGameCancellationPriorGameStart());
        builder.append(", gameStartWarningMessage=").append(getGameStartWarningMessage());
        builder.append(", emailNotification=").append(getEmailNotification());
        builder.append(", playerGameReadiness=").append(getPlayerGameReadiness());
        builder.append(", minimumJackpotGarantie=").append(getMinimumJackpotGarantie());
		builder.append("]");
		return builder.toString();
	}
}
