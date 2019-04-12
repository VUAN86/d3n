package de.ascendro.f4m.service.usermessage.model;

import de.ascendro.f4m.service.json.model.JsonRequiredNullable;
import de.ascendro.f4m.service.usermessage.translation.TranslatableMessage;

/**
 * Request to send an e-mail to a user.
 */
public class SendEmailWrapperRequest extends TranslatableMessage implements MessageToUserIdRequest {
	public static final String NEW_LINE = "<br/>";
	
	private String address;
	@JsonRequiredNullable
	private String userId;
	private String subject;
	private String[] subjectParameters;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@Override
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String[] getSubjectParameters() {
		return subjectParameters;
	}

	public void setSubjectParameters(String[] subjectParameters) {
		this.subjectParameters = subjectParameters;
	}
	
	@Override
	protected void contentsToString(StringBuilder builder) {
		builder.append("address=");
		builder.append(address);
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", subject=");
		builder.append(subject);
		builder.append(", ");
		super.contentsToString(builder);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SendEmailRequest [");
		contentsToString(builder);
		builder.append("]");
		return builder.toString();
	}
}
