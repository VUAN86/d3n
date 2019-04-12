package de.ascendro.f4m.service.payment.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.ascendro.f4m.service.payment.model.external.IdentificationMethod;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
public class IdentificationInitializationRest {
	@JsonProperty("CallbackUrlError")
	protected String callbackUrlError;
	@JsonProperty("CallbackUrlSuccess")
	protected String callbackUrlSuccess;
	@JsonProperty("Method")
	protected IdentificationMethod method;
	@JsonProperty("UserId")
	protected String userId;

	public String getCallbackUrlError() {
		return callbackUrlError;
	}

	public void setCallbackUrlError(String callbackUrlError) {
		this.callbackUrlError = callbackUrlError;
	}

	public String getCallbackUrlSuccess() {
		return callbackUrlSuccess;
	}

	public void setCallbackUrlSuccess(String callbackUrlSuccess) {
		this.callbackUrlSuccess = callbackUrlSuccess;
	}

	public IdentificationMethod getMethod() {
		return method;
	}

	public void setMethod(IdentificationMethod method) {
		this.method = method;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("IdentificationInitializationRest [callbackUrlError=");
		builder.append(callbackUrlError);
		builder.append(", callbackUrlSuccess=");
		builder.append(callbackUrlSuccess);
		builder.append(", method=");
		builder.append(method);
		builder.append(", userId=");
		builder.append(userId);
		builder.append("]");
		return builder.toString();
	}
}
