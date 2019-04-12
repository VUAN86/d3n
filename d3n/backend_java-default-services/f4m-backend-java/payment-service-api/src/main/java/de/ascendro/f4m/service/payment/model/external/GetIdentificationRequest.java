package de.ascendro.f4m.service.payment.model.external;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;

public class GetIdentificationRequest extends EmptyJsonMessageContent {

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetIdentificationRequest []");
		return builder.toString();
	}
}
