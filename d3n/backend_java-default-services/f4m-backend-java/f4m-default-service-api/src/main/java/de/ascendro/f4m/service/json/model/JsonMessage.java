package de.ascendro.f4m.service.json.model;

import java.util.Arrays;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.json.model.type.MessageType;
import de.ascendro.f4m.service.json.model.user.ClientInfo;

public class JsonMessage<T extends JsonMessageContent> {
	public static final String RESPONSE_NAME_SUFFIX = "Response";
	public static final String DEFAULT_ERROR_MESSAGE_NAME = "error";
	public static final String MESSAGE_NAME_PROPERTY = "message";
	public static final String MESSAGE_CONTENT_PROPERTY = "content";
	public static final String MESSAGE_SEQ_PROPERTY = "seq";
	public static final String MESSAGE_TIMESTAMP_PROPERTY = "timestamp";
    public static final String MESSAGE_CLIENT_INFO_PROPERTY = "clientInfo";
    public static final String MESSAGE_CLIENT_ID_PROPERTY = "clientId";

	@SerializedName(value = MESSAGE_NAME_PROPERTY)
	private String name;

	@SerializedName(value = MESSAGE_CONTENT_PROPERTY)
	@JsonRequiredNullable
	private T content;

	@SerializedName(value = DEFAULT_ERROR_MESSAGE_NAME)
	@JsonRequiredNullable
	private JsonMessageError error;

	@SerializedName(value = "ack")
	@JsonRequiredNullable
	private long[] ack;

	@SerializedName(value = MESSAGE_SEQ_PROPERTY)
	@JsonRequiredNullable
	private Long seq;

	@SerializedName(value = MESSAGE_TIMESTAMP_PROPERTY)
	private Long timestamp;
	
    @SerializedName(value = MESSAGE_CLIENT_INFO_PROPERTY)
    private ClientInfo clientInfo;

	public JsonMessage() {
		//empty constructor - used in tests
	}

	public JsonMessage(String name) {
		this.name = name;
	}

	public JsonMessage(MessageType messageType) {
		this.name = messageType.getMessageName();
	}

	public JsonMessage(MessageType messageType, T content) {
		this(messageType);
		this.content = content;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public T getContent() {
		return content;
	}

	public void setContent(T content) {
		this.content = content;
	}

	public long[] getAck() {
		return ack;
	}

	public Long getFirstAck() {
		final Long firstAck;

		if (ack != null && ack.length > 0) {
			firstAck = ack[0];
		} else {
			firstAck = null;
		}

		return firstAck;

	}

	public void setAck(long[] ack) {
		this.ack = ack;
	}

	public void setAck(Long ack) {
		if (ack != null) {
			this.ack = new long[] { ack };
		}
	}

	public Long getSeq() {
		return seq;
	}

	public void setSeq(Long seq) {
		this.seq = seq;
	}

	public JsonMessageError getError() {
		return error;
	}

	public void setError(JsonMessageError error) {
		this.error = error;
	}
	
	public ClientInfo getClientInfo() {
		return clientInfo;
	}
	
	public void setClientInfo(ClientInfo clientInfo) {
		this.clientInfo = clientInfo;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public Long getTimestampOrNow() {
		return timestamp != null ? timestamp : System.currentTimeMillis();
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isResponse() {
		return name != null && name.endsWith(RESPONSE_NAME_SUFFIX);
	}

	public String getTypeName() {
		final String messageTypeName;

		if (name != null) {
			messageTypeName = name.replaceAll(JsonMessage.RESPONSE_NAME_SUFFIX, "");
		} else {
			messageTypeName = null;
		}

		return messageTypeName;
	}

	public <E extends Enum<? extends MessageType> & MessageType> E getType(Class<E> messageTypeEnumeration) {
		E type = null;
		if (name != null) {
			type = getTypeByName(messageTypeEnumeration, getName());
			if (type == null) { // 2nd place with workaround for two approaches
								// - register request and response as single
				// class or separate into two different model
				// classes.
				type = getTypeByName(messageTypeEnumeration, getTypeName());
			}
		}
		return type;
	}

	private <E extends Enum<? extends MessageType> & MessageType> E getTypeByName(Class<E> messageTypeEnumeration,
			final String messageTypeName) {
		E type = null;
		for (E messageType : messageTypeEnumeration.getEnumConstants()) {
			if (messageType.getMessageName().equalsIgnoreCase(messageTypeName)) {
				type = messageType;
				break;
			}
		}
		return type;
	}

	public void setName(MessageType messageTypeEnum) {
		this.name = messageTypeEnum.getMessageName();
	}

	public static String getResponseMessageName(String receivedMessageName) {
		final String responseMessageName;

		if (!receivedMessageName.contains(RESPONSE_NAME_SUFFIX)) {
			responseMessageName = receivedMessageName + RESPONSE_NAME_SUFFIX;
		} else {
			responseMessageName = receivedMessageName;
		}

		return responseMessageName;
	}
	
	public String getClientId() {		
		return clientInfo != null ? clientInfo.getClientId() : null;
	}
	
	public void setClientId(String clientId) {
		if(clientInfo == null){
			clientInfo = new ClientInfo();
		}
		clientInfo.setClientId(clientId);		
	}

	public String getUserId() {
		return clientInfo != null ? clientInfo.getUserId() : null;
	}

	public String getTenantId() {
		return clientInfo != null ? clientInfo.getTenantId() : null;
	}
	
	public String getAppId() {
		return clientInfo != null ? clientInfo.getAppId() : null;
	}

	public boolean isAuthenticated() {
		return clientInfo != null && clientInfo.hasUserInfo();
	}

	@Override
	public String toString() {
		return "JsonMessage{" +
				"name='" + name + '\'' +
				", content=" + content +
				", error=" + error +
				", ack=" + Arrays.toString(ack) +
				", seq=" + seq +
				", timestamp=" + timestamp +
				", clientInfo=" + clientInfo +
				'}';
	}
}
