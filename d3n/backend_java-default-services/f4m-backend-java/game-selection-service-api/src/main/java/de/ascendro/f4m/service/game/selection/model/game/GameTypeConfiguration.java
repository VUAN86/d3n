package de.ascendro.f4m.service.game.selection.model.game;

public class GameTypeConfiguration {
	
	private MultiplayerGameTypeConfigurationData gameTournament;

	private MultiplayerGameTypeConfigurationData gameDuel;
	
	private SingleplayerGameTypeConfigurationData gameQuickQuiz;

	private static final SingleplayerGameTypeConfigurationData DEFAULT_SINGLEPLAYER_GAME_TYPE_CONFIGURATION_DATA_FOR_MULTIPLAYER_GAMES = new SingleplayerGameTypeConfigurationData();

	private static final int SINGLE_PLAYER_GAME_MINIMUM_PLAYER_NEEDED = 1;

	private static final MultiplayerGameTypeConfigurationData DEFAULT_MULTIPLAYER_GAME_TYPE_CONFIGURATION_DATA_FOR_SINGLEPLAYER_GAMES = new MultiplayerGameTypeConfigurationData(
			SINGLE_PLAYER_GAME_MINIMUM_PLAYER_NEEDED, null, null, null, null, null);

	public GameTypeConfiguration() {
		// init empty GameTypeConfiguration
	}

	public GameTypeConfiguration(MultiplayerGameTypeConfigurationData gameTournament,
			MultiplayerGameTypeConfigurationData gameDuel, SingleplayerGameTypeConfigurationData gameQuiz24) {
		super();
		this.gameTournament = gameTournament;
		this.gameDuel = gameDuel;
		this.gameQuickQuiz = gameQuiz24;
	}

	public Integer getMinimumPlayerNeeded(GameType type) {
		return getMultiplayerData(type).getMinimumPlayerNeeded();
	}
	
	public Integer getGameCancellationPriorGameStart(GameType type) {
		return getMultiplayerData(type).getGameCancellationPriorGameStart();
	}

	public Integer getGameStartWarningMessage(GameType type) {
		return getMultiplayerData(type).getGameStartWarningMessage();		
	}
	
	public Integer getEmailNotification(GameType type) {
		return getMultiplayerData(type).getEmailNotification();
	}
	
	public Integer getPlayerGameReadiness(GameType type) {
		return getMultiplayerData(type).getPlayerGameReadiness();
	}
	
	public Integer getMinimumJackpotGarantie(GameType type) {
		return getMultiplayerData(type).getMinimumJackpotGarantie();
	}
	
	public boolean isSpecial(GameType type) {
		if (GameType.QUIZ24 == type) {
			return getSingleplayerData(type).isSpecial();
		}
		return false;
	}
	
	public Double getWeight(GameType type) {
		return getSingleplayerData(type).getWeight();
	}
	
	public String getBannerMediaId(GameType type) {
		return getSingleplayerData(type).getBannerMediaId();
	}

	public MultiplayerGameTypeConfigurationData getGameTournament() {
		return gameTournament;
	}

	public void setGameTournament(MultiplayerGameTypeConfigurationData gameTournament) {
		this.gameTournament = gameTournament;
	}

	public MultiplayerGameTypeConfigurationData getGameDuel() {
		return gameDuel;
	}

	public void setGameDuel(MultiplayerGameTypeConfigurationData gameDuel) {
		this.gameDuel = gameDuel;
	}

	public SingleplayerGameTypeConfigurationData getGameQuickQuiz() {
		return gameQuickQuiz;
	}

	public void setGameQuickQuiz(SingleplayerGameTypeConfigurationData gameQuiz24) {
		this.gameQuickQuiz = gameQuiz24;
	}
	
	private MultiplayerGameTypeConfigurationData getMultiplayerData(GameType type) {
		MultiplayerGameTypeConfigurationData conf = null;
		if (GameType.LIVE_TOURNAMENT == type || GameType.USER_LIVE_TOURNAMENT == type || GameType.TOURNAMENT == type) {
			conf = gameTournament;
		}
		if (GameType.DUEL == type) {
			conf = gameDuel;
		}
		if (conf == null) {
			conf = DEFAULT_MULTIPLAYER_GAME_TYPE_CONFIGURATION_DATA_FOR_SINGLEPLAYER_GAMES;
		}
		return conf;
	}
	
	private SingleplayerGameTypeConfigurationData getSingleplayerData(GameType type) {
		SingleplayerGameTypeConfigurationData conf = null;
		if (GameType.QUIZ24 == type) {
			conf = gameQuickQuiz;
		}
		if (conf == null) {
			conf = DEFAULT_SINGLEPLAYER_GAME_TYPE_CONFIGURATION_DATA_FOR_MULTIPLAYER_GAMES;
		}
		return conf;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameTypeConfiguration [gameTournament=");
		builder.append(gameTournament);
        builder.append(", gameDuel=").append(gameDuel);
        builder.append(", gameQuiz24=").append(gameQuickQuiz);
		builder.append("]");
		return builder.toString();
	}
}
