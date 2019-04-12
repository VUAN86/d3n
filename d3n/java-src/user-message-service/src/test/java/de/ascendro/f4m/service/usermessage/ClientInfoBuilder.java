package de.ascendro.f4m.service.usermessage;

import de.ascendro.f4m.service.json.model.user.ClientInfo;

public class ClientInfoBuilder {

	private ClientInfo clientInfo;
	
	public ClientInfoBuilder() {
		clientInfo = new ClientInfo();
	}
	
	public ClientInfoBuilder withTenantIdAppId(String tenantId, String appId) {
		clientInfo.setTenantId(tenantId);
		clientInfo.setAppId(appId);
		return this;
	}
	
	public ClientInfo build() {
		return clientInfo;
	}
	
	public static ClientInfo fromTenantIdAppId(String tenantId, String appId) {
		return new ClientInfoBuilder().withTenantIdAppId(tenantId, appId).build();
	}
}
