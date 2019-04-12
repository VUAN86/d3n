package de.ascendro.f4m.service.payment.session;

import de.ascendro.f4m.service.payment.model.PaymentClientInfo;

public class PaymentClientInfoImpl implements PaymentClientInfo {
	private String tenantId;
	private String profileId;
	private String appId;
    private String language;

	public PaymentClientInfoImpl() {
	}

    public PaymentClientInfoImpl(String tenantId, String profileId, String appId, String language) {
        this.tenantId = tenantId;
        this.profileId = profileId;
        this.appId = appId;
        this.language = language;
    }

    public PaymentClientInfoImpl(String tenantId, String profileId, String appId) {
        this.tenantId = tenantId;
        this.profileId = profileId;
        this.appId = appId;
    }

	@Override
	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	@Override
	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

    @Override
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ClientInfoSession [tenantId=");
		builder.append(tenantId);
		builder.append(", profileId=");
		builder.append(profileId);
		builder.append(", appId=");
		builder.append(appId);
        builder.append(", language=");
        builder.append(language);
        builder.append("]");
		return builder.toString();
	}

}
