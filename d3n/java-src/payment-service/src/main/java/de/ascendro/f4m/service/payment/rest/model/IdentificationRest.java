package de.ascendro.f4m.service.payment.rest.model;

import java.time.ZonedDateTime;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ascendro.f4m.service.payment.json.JacksonDateDeserializer;
import de.ascendro.f4m.service.payment.json.JacksonDateSerializer;
import de.ascendro.f4m.service.payment.model.external.IdentificationMethod;

@XmlRootElement
public class IdentificationRest {
	@JsonProperty("Id")
	private String id;
	@JsonProperty("ClientId")
	private String clientId;
	@JsonProperty("UserId")
	private String userId;
	@JsonProperty("IdentityId")
	private String identityId;
	@JsonProperty("IdentificationToken")
	private String identificationToken; //new undocumented property
	@JsonProperty("IdentificationUrl")
	private String identificationUrl; //new undocumented property
	@JsonProperty("Created")
	@JsonSerialize(using = JacksonDateSerializer.class)
	@JsonDeserialize(using = JacksonDateDeserializer.class)
	private ZonedDateTime created;
	@JsonProperty("Processed")
	@JsonSerialize(using = JacksonDateSerializer.class)
	@JsonDeserialize(using = JacksonDateDeserializer.class)
	private ZonedDateTime processed;
	@JsonProperty("CallbackUrlSuccess")
	private String callbackUrlSuccess; //new undocumented property, previously there was only one "callbackUrl"
	@JsonProperty("CallbackUrlError")
	private String callbackUrlError; //new undocumented property, previously there was only one "callbackUrl"
	@JsonProperty("State")
	private IdentificationState state;
	@JsonProperty("Method")
	private IdentificationMethod method;
	@JsonProperty("Client")
	private ObjectNode client; //FIXME: too much info currently is returned, data type will change probably
	@JsonProperty("Identity")
	private IdentityRest identity; //content type not clear, null in all test data, same as UserRest.Identity

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getIdentityId() {
		return identityId;
	}

	public void setIdentityId(String identityId) {
		this.identityId = identityId;
	}

	public String getIdentificationToken() {
		return identificationToken;
	}

	public void setIdentificationToken(String identificationToken) {
		this.identificationToken = identificationToken;
	}

	public String getIdentificationUrl() {
		return identificationUrl;
	}

	public void setIdentificationUrl(String identificationUrl) {
		this.identificationUrl = identificationUrl;
	}

	public ZonedDateTime getCreated() {
		return created;
	}

	public void setCreated(ZonedDateTime created) {
		this.created = created;
	}

	public ZonedDateTime getProcessed() {
		return processed;
	}

	public void setProcessed(ZonedDateTime processed) {
		this.processed = processed;
	}

	public String getCallbackUrlSuccess() {
		return callbackUrlSuccess;
	}

	public void setCallbackUrlSuccess(String callbackUrlSuccess) {
		this.callbackUrlSuccess = callbackUrlSuccess;
	}

	public String getCallbackUrlError() {
		return callbackUrlError;
	}

	public void setCallbackUrlError(String callbackUrlError) {
		this.callbackUrlError = callbackUrlError;
	}

	public IdentificationState getState() {
		return state;
	}

	public void setState(IdentificationState state) {
		this.state = state;
	}

	public IdentificationMethod getMethod() {
		return method;
	}

	public void setMethod(IdentificationMethod method) {
		this.method = method;
	}

	public ObjectNode getClient() {
		return client;
	}

	public void setClient(ObjectNode client) {
		this.client = client;
	}

	public IdentityRest getIdentity() {
		return identity;
	}

	public void setIdentity(IdentityRest identity) {
		this.identity = identity;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("IdentificationRest [id=");
		builder.append(id);
		builder.append(", clientId=");
		builder.append(clientId);
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", identityId=");
		builder.append(identityId);
		builder.append(", identificationToken=");
		builder.append(identificationToken);
		builder.append(", identificationUrl=");
		builder.append(identificationUrl);
		builder.append(", created=");
		builder.append(created);
		builder.append(", processed=");
		builder.append(processed);
		builder.append(", callbackUrlSuccess=");
		builder.append(callbackUrlSuccess);
		builder.append(", callbackUrlError=");
		builder.append(callbackUrlError);
		builder.append(", state=");
		builder.append(state);
		builder.append(", method=");
		builder.append(method);
		builder.append(", client=");
		builder.append(client);
		builder.append(", identity=");
		builder.append(identity);
		builder.append("]");
		return builder.toString();
	}
}
