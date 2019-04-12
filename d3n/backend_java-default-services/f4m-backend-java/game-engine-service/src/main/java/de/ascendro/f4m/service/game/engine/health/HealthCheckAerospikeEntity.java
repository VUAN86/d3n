package de.ascendro.f4m.service.game.engine.health;

/**
 * Object representing information retrieved from a single health check round-trip for storage in Aerospike.
 */
public class HealthCheckAerospikeEntity {
	private Long roundtripStartTime;
	private Long roundtripTotalTime;

	/**
	 * @return measured in milliseconds from epoch
	 */
	public Long getRoundtripStartTime() {
		return roundtripStartTime;
	}

	public void setRoundtripStartTime(Long roundtripStartTime) {
		this.roundtripStartTime = roundtripStartTime;
	}

	/**
	 * @return measured in milliseconds
	 */
	public Long getRoundtripTotalTime() {
		return roundtripTotalTime;
	}

	public void setRoundtripTotalTime(Long roundtripTotalTime) {
		this.roundtripTotalTime = roundtripTotalTime;
	}

}
