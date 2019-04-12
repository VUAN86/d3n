package de.ascendro.f4m.service.payment.rest.wrapper;

import javax.ws.rs.client.Client;

import de.ascendro.f4m.service.payment.dao.TenantInfo;

public class RestClient {
	private Client client;
	private TenantInfo tenantInfo;
	
	public RestClient(Client client, TenantInfo tenantInfo) {
		this.client = client;
		this.tenantInfo = tenantInfo;
	}
	
	public Client getClient() {
		return client;
	}
	public TenantInfo getTenantInfo() {
		return tenantInfo;
	}
	
}
