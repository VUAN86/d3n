package de.ascendro.f4m.service.payment.model.external;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class InitIdentificationResponse implements JsonMessageContent {
	private String forwardUrl;
	private String identificationId;

	public String getForwardUrl() {
		return forwardUrl;
	}

	public void setForwardUrl(String forwardUrl) {
		this.forwardUrl = forwardUrl;
	}

	public String getIdentificationId() {
		return identificationId;
	}

	public void setIdentificationId(String identificationId) {
		this.identificationId = identificationId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InitIdentificationResponse [forwardUrl=");
		builder.append(forwardUrl);
		builder.append(", identificationId=");
		builder.append(identificationId);
		builder.append("]");
		return builder.toString();
	}
}
