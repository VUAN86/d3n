package de.ascendro.f4m.service.payment.dao;

public class PendingIdentification {
    private String tenantId;
    private String appId;
    private String profileId;

    public PendingIdentification(String tenantId, String appId, String profileId) {
        this.tenantId = tenantId;
        this.appId = appId;
        this.profileId = profileId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PendingIdentification [tenantId=");
        builder.append(tenantId);
        builder.append(", appId=");
        builder.append(appId);
        builder.append(", profileId=");
        builder.append(profileId);
        builder.append("]");
        return builder.toString();
    }
}
