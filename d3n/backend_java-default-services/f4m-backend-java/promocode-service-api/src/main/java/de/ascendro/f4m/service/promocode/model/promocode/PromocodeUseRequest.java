package de.ascendro.f4m.service.promocode.model.promocode;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class PromocodeUseRequest implements JsonMessageContent {

	private String code;

	public PromocodeUseRequest(String code) {
		this.code = code;
	}


	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PromocodeUseRequest [");
		builder.append("code=").append(code);
		builder.append("]");
		return builder.toString();
	}
}
