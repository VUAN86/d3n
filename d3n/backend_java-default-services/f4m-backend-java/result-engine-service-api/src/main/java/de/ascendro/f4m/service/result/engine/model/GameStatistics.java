package de.ascendro.f4m.service.result.engine.model;

public class GameStatistics {

	private long playedCount;
	private long specialPrizeAvailableCount;
	private long specialPrizeWonCount;

	public GameStatistics() {
		// Initialize with zeroes
	}
	
	public GameStatistics(long playedCount, long specialPrizeAvailableCount, long specialPrizeWonCount) {
		this.playedCount = playedCount;
		this.specialPrizeAvailableCount = specialPrizeAvailableCount;
		this.specialPrizeWonCount = specialPrizeWonCount;
	}

	public long getPlayedCount() {
		return playedCount;
	}

	public void setPlayedCount(long playedCount) {
		this.playedCount = playedCount;
	}

	public long getSpecialPrizeAvailableCount() {
		return specialPrizeAvailableCount;
	}

	public void setSpecialPrizeAvailableCount(long specialPrizeAvailableCount) {
		this.specialPrizeAvailableCount = specialPrizeAvailableCount;
	}

	public long getSpecialPrizeWonCount() {
		return specialPrizeWonCount;
	}

	public void setSpecialPrizeWonCount(long specialPrizeWonCount) {
		this.specialPrizeWonCount = specialPrizeWonCount;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameStatistics");
		builder.append(" [playedCount=").append(playedCount).append("]");
		builder.append(" [specialPrizeAvailableCount=").append(specialPrizeAvailableCount).append("]");
		builder.append(" [specialPrizeWonCount=").append(specialPrizeWonCount).append("]");
		return builder.toString();
	}
}
