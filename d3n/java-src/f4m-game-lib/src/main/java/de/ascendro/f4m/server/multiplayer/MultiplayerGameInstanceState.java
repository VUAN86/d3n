package de.ascendro.f4m.server.multiplayer;

import java.util.stream.Stream;

public enum MultiplayerGameInstanceState {
	PENDING("pending"),
	INVITED("invited"), 
	REGISTERED("registered"), 
	STARTED("started"), 
	CALCULATED("calculated"),
	CANCELLED("cancelled"),
	DECLINED("declined"),
	DELETED("deleted"),
	EXPIRED("expired");

	private final String binName;

	private MultiplayerGameInstanceState(String binName) {
		this.binName = binName;
	}

	public String getBinName() {
		return binName;
	}
	
	public boolean isOpponent(MultiplayerGameInstanceState state) {
        return Stream.of(REGISTERED, STARTED, CALCULATED).anyMatch(s -> s == state);

	}

}
