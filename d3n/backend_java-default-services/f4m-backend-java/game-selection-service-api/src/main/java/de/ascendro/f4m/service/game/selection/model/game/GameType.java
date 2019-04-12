package de.ascendro.f4m.service.game.selection.model.game;

import java.util.stream.Stream;

public enum GameType {
	DUEL("duel", false, false) {
		@Override
		public boolean isDuel() {
			return true;
		}
	},
	QUIZ24("quiz24", false, false) {
		@Override
		public boolean isMultiUser() {
			return false;
		}
	}, BETS("bets", false, false), 
	TV_LIVE_EVENT("tvLiveEvent", false, true), 
	TOURNAMENT("tournament", true, false),
	TIPP_TOURNAMENT("tippTournament", true, false),
	LIVE_TOURNAMENT("liveTournmt", true, true), 
	USER_TOURNAMENT("userTournmt", true, false),
	USER_LIVE_TOURNAMENT("userLiveTrnmt", true, true),
	PLAYOFF_TOURNAMENT("poTournmt", true, false) {
		@Override
		public boolean isPlayoff() {
			return true;
		}
	};
	
	private String code;
	private boolean tournament = false;
	private boolean live = false;
	private boolean multiUser = true;

	private GameType(String code, boolean tournament, boolean live) {
		assert code.length() <= 14 : "Code is also used as Aerospike Bin name, so its length must be up to 14 characters";
		this.code = code;
		this.tournament = tournament;
		this.live = live;
	}
	
	public String getCode() {
		return code;
	}

	public boolean isDuel() {
		return false;
	}

	public boolean isLive() {
		return live;
	}

	public boolean isTournament() {
		return tournament;
	}
	
	public boolean isPlayoff() {
		return false;
	}
	
	public boolean isMultiUser() {
		return multiUser;
	}
	
	public boolean isOneOf(GameType...types){
		return Stream.of(types).anyMatch(t -> t == this);
	}

}
