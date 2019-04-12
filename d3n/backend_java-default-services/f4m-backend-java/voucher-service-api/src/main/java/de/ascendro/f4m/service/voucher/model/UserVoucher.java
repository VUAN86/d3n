package de.ascendro.f4m.service.voucher.model;

public class UserVoucher {

	private String id;
	private String code;
	private String userId;
	private String emailSentDate;
	private String obtainDate;
	private boolean isPurchased;
	private boolean isUsed;
	private String usedDate;
	private String gameInstanceId;
	private String tombolaId;

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

	public String getEmailSentDate() {
		return emailSentDate;
	}

	public void setEmailSentDate(String emailSentDate) {
		this.emailSentDate = emailSentDate;
	}

	public String getObtainDate() {
		return obtainDate;
	}

	public void setObtainDate(String obtainDate) {
		this.obtainDate = obtainDate;
	}

	public boolean isPurchased() {
		return isPurchased;
	}

	public void setPurchased(boolean purchased) {
		isPurchased = purchased;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public boolean isUsed() {
		return isUsed;
	}

	public void setUsed(boolean used) {
		isUsed = used;
	}

	public String getUsedDate() {
		return usedDate;
	}

	public void setUsedDate(String useDate) {
		this.usedDate = useDate;
	}

	public String getTombolaId() {
		return tombolaId;
	}

	public void setTombolaId(String tombolaId) {
		this.tombolaId = tombolaId;
	}
}
