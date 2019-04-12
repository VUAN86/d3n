package de.ascendro.f4m.service.payment.model.external;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class InitIdentificationRequest implements JsonMessageContent {
	private IdentificationMethod method;

	public IdentificationMethod getMethod() {
		return method;
	}

	public void setMethod(IdentificationMethod method) {
		this.method = method;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InitIdentificationRequest [method=");
		builder.append(method);
		builder.append("]");
		return builder.toString();
	}

}
