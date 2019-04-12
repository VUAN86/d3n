package de.ascendro.f4m.service.voucher.model;

public class UserVoucherResponseModel {

    private String userVoucherId;
    private String title;
    private String shortTitle;
    private String company;
    private String expirationDate;
    private String emailSentDate;
    private boolean isPurchased;
    private boolean isUsed;
    private String usedDate;
    private boolean isWon;
    private Integer bonuspointsCosts;
    private String obtainDate;
    private boolean isSpecialPrize;
    private boolean isQRCode;
    private String description;
    private String code;
    private String miniImageId;
    private String normalImageId;
    private String bigImageId;
	private String redemptionURL;

    public UserVoucherResponseModel() {
        // Empty constructor for user voucher model
    }

    public UserVoucherResponseModel(String userVoucherId, Voucher voucher, UserVoucher userVoucher) {
        this.userVoucherId = userVoucherId;
        this.title = voucher.getTitle();
        this.shortTitle = voucher.getShortTitle();
        this.company = voucher.getCompany();
        this.expirationDate = voucher.getExpirationDate();
        this.emailSentDate = userVoucher.getEmailSentDate();
        this.isPurchased = userVoucher.isPurchased();
        this.isUsed = userVoucher.isUsed();
        this.usedDate = userVoucher.getUsedDate();
        this.isWon = !userVoucher.isPurchased();
        this.bonuspointsCosts = voucher.getBonuspointsCosts();
        this.obtainDate = userVoucher.getObtainDate();
        this.isSpecialPrize = voucher.isSpecialPrize();
        this.isQRCode = voucher.isQRCode();
        this.description = voucher.getDescription();
        this.code = userVoucher.getCode();
        this.miniImageId = voucher.getMiniImageId();
        this.normalImageId = voucher.getNormalImageId();
        this.bigImageId = voucher.getBigImageId();
		this.redemptionURL = voucher.getRedemptionURL();
    }

    public String getUserVoucherId() {
        return userVoucherId;
    }

    public void setUserVoucherId(String userVoucherId) {
        this.userVoucherId = userVoucherId;
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

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getEmailSentDate() {
        return emailSentDate;
    }

    public void setEmailSentDate(String emailSentDate) {
        this.emailSentDate = emailSentDate;
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public void setPurchased(boolean purchased) {
        isPurchased = purchased;
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

	public void setUsedDate(String usedDate) {
		this.usedDate = usedDate;
	}

	public boolean isWon() {
		return isWon;
	}

	public void setWon(boolean isWon) {
		this.isWon = isWon;
	}

	public Integer getBonuspointsCosts() {
        return bonuspointsCosts;
    }

    public void setBonuspointsCosts(Integer bonuspointsCosts) {
        this.bonuspointsCosts = bonuspointsCosts;
    }

    public String getObtainDate() {
        return obtainDate;
    }

    public void setObtainDate(String obtainDate) {
        this.obtainDate = obtainDate;
    }

    public boolean isSpecialPrize() {
        return isSpecialPrize;
    }

    public void setSpecialPrize(boolean specialPrize) {
        isSpecialPrize = specialPrize;
    }

    public boolean isQRCode() {
        return isQRCode;
    }

    public void setQRCode(boolean QRCode) {
        isQRCode = QRCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserVoucherResponseModel [userVoucherId=");
        builder.append(userVoucherId);
        builder.append(", title=");
        builder.append(title);
        builder.append(", shortTitle=");
        builder.append(shortTitle);
        builder.append(", company=");
        builder.append(company);
        builder.append(", expirationDate=[");
        builder.append(expirationDate);
        builder.append(", emailSentDate=[");
        builder.append(emailSentDate);
        builder.append(", isPurchased=");
        builder.append(isPurchased);
        builder.append(", isUsed=");
        builder.append(isUsed);
        builder.append(", usedDate=[");
        builder.append(usedDate);
        builder.append(", isWon=");
        builder.append(isWon);
        builder.append("], bonuspointsCosts=[");
        builder.append(bonuspointsCosts);
        builder.append(", obtainDate=[");
        builder.append(obtainDate);
        builder.append(", isSpecialPrize=");
        builder.append(isSpecialPrize);
        builder.append(", isQRCode=");
        builder.append(isQRCode);
        builder.append(", description=");
        builder.append(description);
        builder.append(", code=");
        builder.append(code);
        builder.append(", miniImageId=[");
        builder.append(miniImageId);
        builder.append(", normalImageId=[");
        builder.append(normalImageId);
        builder.append(", bigImageId=[");
        builder.append(bigImageId);
		builder.append(", redemptionURL=[");
		builder.append(redemptionURL);
        builder.append("]]");
        return builder.toString();
    }
}
