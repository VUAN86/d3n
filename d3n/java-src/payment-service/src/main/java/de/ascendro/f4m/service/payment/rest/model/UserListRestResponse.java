package de.ascendro.f4m.service.payment.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Describes response to user list call in Paydent/Expolio API.
 */
@XmlRootElement
public class UserListRestResponse extends ListResponseRest<UserRest> {
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserListRestResponse [limit=");
		builder.append(limit);
		builder.append(", offset=");
		builder.append(offset);
		builder.append(", totalCount=");
		builder.append(total);
		builder.append(", data=");
		builder.append(data);
		builder.append("]");
		return builder.toString();
	}

}