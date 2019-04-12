package de.ascendro.f4m.service.analytics.client;

public class BonusPayoutRequestInfo extends PaymentTransferRequestInfo {

	private boolean fullRegistration;
	
	public boolean isFullRegistration() {
		return fullRegistration;
	}
	
	public void setFullRegistration(boolean fullRegistration) {
		this.fullRegistration = fullRegistration;
	}
	
}
