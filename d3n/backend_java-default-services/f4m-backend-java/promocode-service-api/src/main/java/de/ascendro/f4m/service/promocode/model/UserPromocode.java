package de.ascendro.f4m.service.promocode.model;

import java.math.BigDecimal;

public class UserPromocode {

	private String id;
	private String code;
	private String userId;
	private String expirationDate;
	private String usedOnDate;

	private String moneyTransactionId;
	private String creditTransactionId;
	private String bonuspointsTransactionId;


	private BigDecimal moneyValue;
	private Integer creditValue;
	private Integer bonuspointsValue;



	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}

	public String getUsedOnDate() {
		return usedOnDate;
	}

	public void setUsedOnDate(String usedOnDate) {
		this.usedOnDate = usedOnDate;
	}

	public String getMoneyTransactionId() {
		return moneyTransactionId;
	}

	public void setMoneyTransactionId(String moneyTransactionId) {
		this.moneyTransactionId = moneyTransactionId;
	}

	public String getCreditTransactionId() {
		return creditTransactionId;
	}

	public void setCreditTransactionId(String creditTransactionId) {
		this.creditTransactionId = creditTransactionId;
	}

	public String getBonuspointsTransactionId() {
		return bonuspointsTransactionId;
	}

	public void setBonuspointsTransactionId(String bonuspointsTransactionId) {
		this.bonuspointsTransactionId = bonuspointsTransactionId;
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
}
