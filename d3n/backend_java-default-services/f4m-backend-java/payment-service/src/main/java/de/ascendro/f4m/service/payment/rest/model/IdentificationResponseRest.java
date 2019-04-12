package de.ascendro.f4m.service.payment.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
//TODO: this annotation is only to ignore other properties, which will certainly change because they currently contain sensitive data - ApiId&ApiKey
//remove annotation, when API is clear.
@JsonIgnoreProperties(ignoreUnknown=true)  
public class IdentificationResponseRest {
	@JsonProperty("Id")
	protected String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("IdentificationResponseRest [id=");
		builder.append(id);
		builder.append("]");
		return builder.toString();
	}
}
