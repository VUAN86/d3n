package de.ascendro.f4m.server.session;

import java.util.Comparator;

import de.ascendro.f4m.service.profile.model.get.app.GetAppConfigurationRequest;

public class GlobalClientSessionInfo {
	private static final int DESC_ORDER = -1;
	
	private String clientId;
	private String gatewayURL;
	private GetAppConfigurationRequest appConfig;
	private Long timestamp; //in seconds

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getGatewayURL() {
		return gatewayURL;
	}

	public void setGatewayURL(String gatewayURL) {
		this.gatewayURL = gatewayURL;
	}

	public GetAppConfigurationRequest getAppConfig() {
		return appConfig;
	}

	public void setAppConfig(GetAppConfigurationRequest appConfig) {
		this.appConfig = appConfig;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
	
	public static Comparator<? super GlobalClientSessionInfo> getNewestFirstComparator() {
		Comparator<? super GlobalClientSessionInfo> comparator = (o1, o2) -> {
			return o1.getTimestamp().compareTo(o2.getTimestamp()) * DESC_ORDER;
		};
		
		return comparator;
	}
}
