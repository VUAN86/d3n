package de.ascendro.f4m.service.promocode.model;

import java.math.BigDecimal;

public class Promocode {

	private String code;
	private Integer amount;

	private String expirationDate;
	private String generationDate;
	private String promocodeCampaignId;
	private String promocodeClassId;

	private BigDecimal moneyValue;
	private Integer creditValue;
	private Integer bonuspointsValue;
	private Integer numberOfUses;

	public Integer getAmount() {
		return amount;
	}

	public void setAmount(Integer amount) {
		this.amount = amount;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}

	public String getGenerationDate() {
		return generationDate;
	}

	public void setGenerationDate(String generationDate) {
		this.generationDate = generationDate;
	}

	public String getPromocodeCampaignId() {
		return promocodeCampaignId;
	}

	public void setPromocodeCampaignId(String promocodeCampaignId) {
		this.promocodeCampaignId = promocodeCampaignId;
	}

	public String getPromocodeClassId() {
		return promocodeClassId;
	}

	public void setPromocodeClassId(String promocodeClassId) {
		this.promocodeClassId = promocodeClassId;
	}

	public BigDecimal getMoneyValue() {
		return moneyValue;
	}

	public void setMoneyValue(BigDecimal moneyValue) {
		this.moneyValue = moneyValue;
	}

	public Integer getCreditValue() {
		return creditValue;
	}

	public void setCreditValue(Integer creditValue) {
		this.creditValue = creditValue;
	}

	public Integer getBonuspointsValue() {
		return bonuspointsValue;
	}

	public void setBonuspointsValue(Integer bonuspointsValue) {
		this.bonuspointsValue = bonuspointsValue;
	}

	public Integer getNumberOfUses() {
		return numberOfUses;
	}

	public void setNumberOfUses(Integer numberOfUses) {
		this.numberOfUses = numberOfUses;
	}
}