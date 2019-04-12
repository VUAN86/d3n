package de.ascendro.f4m.service.game.selection.model.game;

import com.google.gson.annotations.SerializedName;

public class SingleplayerGameTypeConfigurationData implements GameTypeConfigurationData {
	
	private static final double DEFAULT_WEIGHT = 0.0;
	
	@SerializedName(value="isSpecialGame")
	private Boolean special = Boolean.FALSE;
	private Double weight = DEFAULT_WEIGHT; //only for special games
	private String bannerMediaId; //only for special games

	public SingleplayerGameTypeConfigurationData() {
		// init empty SingleplayerGameTypeConfigurationData
	}
	
	
	public SingleplayerGameTypeConfigurationData(boolean special, double weight, String bannerMediaId) {
		super();
		this.special = special;
		this.weight = weight;
		this.bannerMediaId = bannerMediaId;
	}


	public Boolean isSpecial() {
		return special != null ? special : Boolean.FALSE;
	}


	public void setSpecial(Boolean special) {
		this.special = special;
	}


	public Double getWeight() {
		return weight != null ? weight : DEFAULT_WEIGHT;
	}


	public void setWeight(Double weight) {
		this.weight = weight;
	}


	public String getBannerMediaId() {
		return bannerMediaId;
	}


	public void setBannerMediaId(String bannerId) {
		this.bannerMediaId = bannerId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SingleplayerGameTypeConfigurationData [special=");
		builder.append(special);
        builder.append(", weight=").append(weight);
        builder.append(", bannerMediaId=").append(bannerMediaId);
		builder.append("]");
		return builder.toString();
	}

}
