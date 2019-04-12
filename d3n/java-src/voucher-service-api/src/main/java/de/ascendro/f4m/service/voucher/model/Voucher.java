package de.ascendro.f4m.service.voucher.model;

public class Voucher {

	private String id;
	private String title;
	private String shortTitle;
	private String company;
	private String description;
	private boolean isQRCode;
	private boolean isSpecialPrize;
	private Integer bonuspointsCosts;
	private String expirationDate;
	private String miniImageId;
	private String normalImageId;
	private String bigImageId;
	private String redemptionURL;
	private String regionalSettings;
	private boolean isExchange;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getShortTitle() {
		return shortTitle;
	}

	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isQRCode() {
		return isQRCode;
	}

	public void setQRCode(boolean QRCode) {
		isQRCode = QRCode;
	}

	public boolean isSpecialPrize() {
		return isSpecialPrize;
	}

	public void setSpecialPrize(boolean specialPrize) {
		isSpecialPrize = specialPrize;
	}

	public Integer getBonuspointsCosts() {
		return bonuspointsCosts;
	}

	public void setBonuspointsCosts(Integer bonuspointsCosts) {
		this.bonuspointsCosts = bonuspointsCosts;
	}

	public String getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}

	public String getMiniImageId() {
		return miniImageId;
	}

	public void setMiniImageId(String miniImageId) {
		this.miniImageId = miniImageId;
	}

	public String getNormalImageId() {
		return normalImageId;
	}

	public void setNormalImageId(String normalImageId) {
		this.normalImageId = normalImageId;
	}

	public String getBigImageId() {
		return bigImageId;
	}

	public void setBigImageId(String bigImageId) {
		this.bigImageId = bigImageId;
	}

	public String getRedemptionURL() {
		return redemptionURL;
	}

	public void setRedemptionURL(String redemptionURL) {
		this.redemptionURL = redemptionURL;
	}

	public String getRegionalSettings() {
		return regionalSettings;
	}

	public void setRegionalSettings(String regionalSettings) {
		this.regionalSettings = regionalSettings;
	}

	public boolean isExchange() {
		return isExchange;
	}

	public void setExchange(boolean exchange) {
		isExchange = exchange;
	}
}