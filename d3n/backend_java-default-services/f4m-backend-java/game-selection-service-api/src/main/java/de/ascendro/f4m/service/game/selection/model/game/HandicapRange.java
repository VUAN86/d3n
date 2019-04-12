package de.ascendro.f4m.service.game.selection.model.game;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class HandicapRange extends JsonObjectWrapper {

	public static final String PROPERTY_HANDICAP_RANGE_ID = "handicapRangeId";
	public static final String PROPERTY_HANDICAP_FROM = "handicapFrom";
	public static final String PROPERTY_HANDICAP_TO = "handicapTo";

	private static final int DEFAULT_HANDICAP_RANGE_ID = 0;
	
	public static final HandicapRange DEFAULT_HANDICAP_RANGE = new HandicapRange(DEFAULT_HANDICAP_RANGE_ID, 0, 100);
	
	public HandicapRange() {
		// Initialize with empty Json object
	}
	
	public HandicapRange(int handicapRangeId, int handicapFrom, int handicapTo) {
		setHandicapRangeId(handicapRangeId);
		setHandicapFrom(handicapFrom);
		setHandicapTo(handicapTo);
	}
	
	public HandicapRange(JsonObject handicapRange) {
		super(handicapRange);
	}
	
	public int getHandicapRangeId() {
		return getPropertyAsInt(PROPERTY_HANDICAP_RANGE_ID);
	}
	
	public void setHandicapRangeId(int handicapRangeId) {
		setProperty(PROPERTY_HANDICAP_RANGE_ID, handicapRangeId);
	}
	
	public int getHandicapFrom() {
		return getPropertyAsInt(PROPERTY_HANDICAP_FROM);
	}
	
	public void setHandicapFrom(int handicapFrom) {
		setProperty(PROPERTY_HANDICAP_FROM, handicapFrom);
	}
	
	public int getHandicapTo() {
		return getPropertyAsInt(PROPERTY_HANDICAP_TO);
	}
	
	public void setHandicapTo(int handicapTo) {
		setProperty(PROPERTY_HANDICAP_TO, handicapTo);
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof HandicapRange) && ((HandicapRange) other).getHandicapRangeId() == getHandicapRangeId();
	}
	
	@Override
	public int hashCode() {
		return getHandicapRangeId();
	}
	
}
