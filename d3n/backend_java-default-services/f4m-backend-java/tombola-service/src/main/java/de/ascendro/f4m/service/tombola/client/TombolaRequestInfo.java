package de.ascendro.f4m.service.tombola.client;

import de.ascendro.f4m.service.request.RequestInfoImpl;

public class TombolaRequestInfo extends RequestInfoImpl {
	private String tombolaId;

	public TombolaRequestInfo(String tombolaId) {
		this.tombolaId = tombolaId;
	}
	
	public String getTombolaId() {
		return tombolaId;
	}

}
