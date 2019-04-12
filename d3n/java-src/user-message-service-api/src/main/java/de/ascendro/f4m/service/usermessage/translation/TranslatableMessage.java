package de.ascendro.f4m.service.usermessage.translation;

import java.util.Arrays;

import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public class TranslatableMessage implements MessageWithParams {

	private static final String LANGUAGE_AUTO = "auto";
	
	@JsonRequiredNullable
	private String language;
	
	private String message;
	private String[] parameters;

	public boolean isLanguageAuto() {
		return LANGUAGE_AUTO.equals(language);
	}
	
	public void setLanguageAuto() {
		language = LANGUAGE_AUTO;
	}
	
	public ISOLanguage getISOLanguage() {
		return ISOLanguage.fromString(language);
	}
	
	public void setISOLanguage(ISOLanguage language) {
		this.language = language == null ? null : language.getValue();
	}
	
	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String[] getParameters() {
		return parameters;
	}

	public void setParameters(String[] parameters) {
		this.parameters = parameters;
	}
	
	protected void contentsToString(StringBuilder builder) {
		builder.append("language=");
		builder.append(language);
		builder.append(", message=");
		builder.append(message);
		builder.append(", parameters=");
		builder.append(Arrays.toString(parameters));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TranslatableMessage [");
		contentsToString(builder);
		builder.append("]");
		return builder.toString();
	}
}
