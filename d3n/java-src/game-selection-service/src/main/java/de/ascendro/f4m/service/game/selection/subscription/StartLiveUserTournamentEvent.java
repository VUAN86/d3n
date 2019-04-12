package de.ascendro.f4m.service.game.selection.subscription;

import java.time.ZonedDateTime;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class StartLiveUserTournamentEvent implements JsonMessageContent {
	private String mgiId;
	private ZonedDateTime playDateTime;

	public StartLiveUserTournamentEvent(String mgiId, ZonedDateTime playDateTime) {
		this.mgiId = mgiId;
		this.playDateTime = playDateTime;
	}

	public String getMgiId() {
		return mgiId;
	}

	public void setMgiId(String mgiId) {
		this.mgiId = mgiId;
	}

	public ZonedDateTime getPlayDateTime() {
		return playDateTime;
	}

	public void setPlayDateTime(ZonedDateTime playDateTime) {
		this.playDateTime = playDateTime;
	}

}
