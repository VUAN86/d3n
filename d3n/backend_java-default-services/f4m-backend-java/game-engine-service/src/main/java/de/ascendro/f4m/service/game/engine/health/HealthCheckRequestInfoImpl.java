package de.ascendro.f4m.service.game.engine.health;

import de.ascendro.f4m.service.request.RequestInfoImpl;

public class HealthCheckRequestInfoImpl extends RequestInfoImpl {
	private String clientId;
	private long roundtripStartTime;
	
	public HealthCheckRequestInfoImpl(String clientId, long roundtripStartTime) {
		this.setClientId(clientId);
		this.roundtripStartTime = roundtripStartTime;
	}

	public long getRoundtripStartTime() {
		return roundtripStartTime;
	}

	public void setRoundtripStartTime(long roundtripStartTime) {
		this.roundtripStartTime = roundtripStartTime;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String gameInstanceId) {
		this.clientId = gameInstanceId;
	}
}