package de.ascendro.f4m.service.usermessage.model;

import de.ascendro.f4m.service.usermessage.translation.TranslatableMessage;

/**
 * Request to send an SMS to a user.
 */
public class SendSmsRequest extends TranslatableMessage implements MessageToUserIdRequest {
	private String phone;
	private String userId;

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	@Override
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	@Override
	protected void contentsToString(StringBuilder builder) {
		builder.append("phone=");
		builder.append(phone);
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", ");
		super.contentsToString(builder);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SendSmsRequest [");
		contentsToString(builder);
		builder.append("]");
		return builder.toString();
	}

}
