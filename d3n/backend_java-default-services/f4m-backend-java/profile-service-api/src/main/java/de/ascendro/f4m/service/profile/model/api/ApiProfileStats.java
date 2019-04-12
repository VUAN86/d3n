package de.ascendro.f4m.service.profile.model.api;

import de.ascendro.f4m.service.profile.model.ProfileStats;

/**
 * Wrapper for wrapper object in a Profile. 
 */
public class ApiProfileStats {

	private int gameLevel;
	private double nextLevelPercentage;
	private int wonGamesCount;
	private int lostGamesCount;
	private int totalGamesCount;
	private int pointsAmount;
	private int creditsAmount;
	private double moneyAmount;

	private int totalQuickQuizGamesCount;
	private int wonDuelGamesCount;
	private int lostDuelGamesCount;
	private int totalDuelGamesCount;
	private int wonTournamentGamesCount;
	private int lostTournamentGamesCount;
	private int totalTournamentGamesCount;

	
	public ApiProfileStats() {
		// Initialize empty object
	}

	public ApiProfileStats(ProfileStats stats) {
		if (stats!=null) {
			wonGamesCount = stats.getPropertyAsInt(ProfileStats.GAMES_WON_PROPERTY);
			lostGamesCount = stats.getPropertyAsInt(ProfileStats.GAMES_LOST_PROPERTY);
			totalGamesCount = stats.getPropertyAsInt(ProfileStats.TOTAL_GAMES_PROPERTY);
			pointsAmount = stats.getPropertyAsInt(ProfileStats.POINTS_WON_PROPERTY);
			creditsAmount = stats.getPropertyAsInt(ProfileStats.CREDIT_WON_PROPERTY);
			moneyAmount = stats.getPropertyAsDouble(ProfileStats.MONEY_WON_PROPERTY);

			totalQuickQuizGamesCount = stats.getPropertyAsInt(ProfileStats.TOTAL_QUICK_QUIZ_GAMES_PROPERTY);
			wonDuelGamesCount = stats.getPropertyAsInt(ProfileStats.WON_DUEL_GAMES_PROPERTY);
			lostDuelGamesCount = stats.getPropertyAsInt(ProfileStats.LOST_DUEL_GAMES_PROPERTY);
			totalDuelGamesCount = stats.getPropertyAsInt(ProfileStats.TOTAL_DUEL_GAMES_PROPERTY);
			wonTournamentGamesCount = stats.getPropertyAsInt(ProfileStats.WON_TOURNAMENT_GAMES_PROPERTY);
			lostTournamentGamesCount = stats.getPropertyAsInt(ProfileStats.LOST_TOURNAMENT_GAMES_PROPERTY);
			totalTournamentGamesCount = stats.getPropertyAsInt(ProfileStats.TOTAL_TOURNAMENT_GAMES_PROPERTY);
		}
	}


	public int getGameLevel() {
		return gameLevel;
	}

	public double getNextLevelPercentage() {
		return nextLevelPercentage;
	}

	public int getWonGamesCount() {
		return wonGamesCount;
	}

	public int getLostGamesCount() {
		return lostGamesCount;
	}

	public int getTotalGamesCount() {
		return totalGamesCount;
	}

	public int getPointsAmount() {
		return pointsAmount;
	}

	public int getCreditsAmount() {
		return creditsAmount;
	}

	public double getMoneyAmount() {
		return moneyAmount;
	}

	public int getTotalQuickQuizGamesCount() {
		return totalQuickQuizGamesCount;
	}

	public int getWonDuelGamesCount() {
		return wonDuelGamesCount;
	}

	public int getLostDuelGamesCount() {
		return lostDuelGamesCount;
	}

	public int getTotalDuelGamesCount() {
		return totalDuelGamesCount;
	}

	public int getWonTournamentGamesCount() {
		return wonTournamentGamesCount;
	}

	public int getLostTournamentGamesCount() {
		return lostTournamentGamesCount;
	}

	public int getTotalTournamentGamesCount() {
		return totalTournamentGamesCount;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ApiProfileStats [");
		builder.append("gameLevel=").append(gameLevel);
		builder.append(", nextLevelPercentage=").append(nextLevelPercentage);
		builder.append(", wonGamesCount=").append(wonGamesCount);
		builder.append(", lostGamesCount=").append(lostGamesCount);
		builder.append(", totalGamesCount=").append(totalGamesCount);
		builder.append(", pointsAmount=").append(pointsAmount);
		builder.append(", creditsAmount=").append(creditsAmount);
		builder.append(", moneyAmount=").append(moneyAmount);
		builder.append(", totalQuickQuizGamesCount=").append(totalQuickQuizGamesCount);
		builder.append(", wonDuelGamesCount=").append(wonDuelGamesCount);
		builder.append(", lostDuelGamesCount=").append(lostDuelGamesCount);
		builder.append(", totalDuelGamesCount=").append(totalDuelGamesCount);
		builder.append(", wonTournamentGamesCount=").append(wonTournamentGamesCount);
		builder.append(", lostTournamentGamesCount=").append(lostTournamentGamesCount);
		builder.append(", totalTournamentGamesCount=").append(totalTournamentGamesCount);
		builder.append("]");
		return builder.toString();
	}

}

