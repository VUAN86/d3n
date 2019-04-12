package de.ascendro.f4m.service.voucher.model;

public class VoucherResponseModel {

	private String voucherId;
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
	private boolean isExchange;

	public VoucherResponseModel() {
		// empty constructor
	}

	public VoucherResponseModel(Voucher voucher) {
		this.voucherId = voucher.getId();
		this.title = voucher.getTitle();
		this.shortTitle = voucher.getShortTitle();
		this.company = voucher.getCompany();
		this.description = voucher.getDescription();
		this.isQRCode = voucher.isQRCode();
		this.isSpecialPrize = voucher.isSpecialPrize();
		this.bonuspointsCosts = voucher.getBonuspointsCosts();
		this.expirationDate = voucher.getExpirationDate();
		this.miniImageId = voucher.getMiniImageId();
		this.normalImageId = voucher.getNormalImageId();
		this.bigImageId = voucher.getBigImageId();
		this.redemptionURL = voucher.getRedemptionURL();
		this.isExchange = voucher.isExchange();
	}

	public String getVoucherId() {
		return voucherId;
	}

	public void setVoucherId(String voucherId) {
		this.voucherId = voucherId;
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

	public boolean isExchange() {
		return isExchange;
	}

	public void setExchange(boolean exchange) {
		isExchange = exchange;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("VoucherResponseModel{");
		sb.append("voucherId='").append(voucherId).append('\'');
		sb.append(", title='").append(title).append('\'');
		sb.append(", shortTitle='").append(shortTitle).append('\'');
		sb.append(", company='").append(company).append('\'');
		sb.append(", description='").append(description).append('\'');
		sb.append(", isQRCode=").append(isQRCode);
		sb.append(", isSpecialPrize=").append(isSpecialPrize);
		sb.append(", bonuspointsCosts=").append(bonuspointsCosts);
		sb.append(", expirationDate='").append(expirationDate).append('\'');
		sb.append(", miniImageId='").append(miniImageId).append('\'');
		sb.append(", normalImageId='").append(normalImageId).append('\'');
		sb.append(", bigImageId='").append(bigImageId).append('\'');
		sb.append(", redemptionURL='").append(redemptionURL).append('\'');
		sb.append(", isExchange=").append(isExchange);
		sb.append('}');
		return sb.toString();
	}
}
