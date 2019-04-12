package de.ascendro.f4m.service.payment.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes error in Paydent/Expolio API.
 */
@XmlRootElement
public class ErrorInfoRest {
	public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
	
	@JsonProperty("Code")
	private String code;
	@JsonProperty("Message")
	private String message;
	@JsonProperty("AdditionalMessage")
	private String additionalMessage;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getAdditionalMessage() {
		return additionalMessage;
	}

	public void setAdditionalMessage(String additionalMessage) {
		this.additionalMessage = additionalMessage;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ErrorInfoRest [code=");
		builder.append(code);
		builder.append(", message=");
		builder.append(message);
		builder.append(", additionalMessage=");
		builder.append(additionalMessage);
		builder.append("]");
		return builder.toString();
	}

}
