package de.ascendro.f4m.service.payment.model;

public interface PaymentClientInfo {
	public String getTenantId();
	public String getProfileId();
    public String getAppId();
    public String getLanguage();
}
