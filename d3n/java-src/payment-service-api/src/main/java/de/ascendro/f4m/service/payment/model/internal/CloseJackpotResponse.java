package de.ascendro.f4m.service.payment.model.internal;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;

public class CloseJackpotResponse extends EmptyJsonMessageContent {

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CloseJackpotResponse []");
		return builder.toString();
	}
}
