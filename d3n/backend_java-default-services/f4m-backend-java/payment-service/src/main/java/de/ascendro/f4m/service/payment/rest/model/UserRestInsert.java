package de.ascendro.f4m.service.payment.rest.model;

import java.time.ZonedDateTime;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.ascendro.f4m.service.payment.json.JacksonDateDeserializer;
import de.ascendro.f4m.service.payment.json.JacksonDateSerializer;
import de.ascendro.f4m.service.payment.json.JsonSerializerParameter;
import de.ascendro.f4m.service.util.DateTimeUtil;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
public class UserRestInsert extends UserInfoRestBase {
	
	@JsonProperty("UserId")
	protected String userId;
	@JsonProperty(value = "DateOfBirth")
	@JsonSerialize(using = JacksonDateSerializer.class)
	@JsonSerializerParameter(format = DateTimeUtil.JSON_DATE_FORMAT)
	@JsonDeserialize(using = JacksonDateDeserializer.class)
	protected ZonedDateTime dateOfBirth;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public ZonedDateTime getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(ZonedDateTime dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserRestInsert [UserId=");
		builder.append(userId);
		builder.append(", name=");
		builder.append(name);
		builder.append(", firstName=");
		builder.append(firstName);
		builder.append(", zip=");
		builder.append(zip);
		builder.append(", street=");
		builder.append(street);
		builder.append(", dateOfBirth=");
		builder.append(dateOfBirth);
		builder.append("]");
		return builder.toString();
	}

}
