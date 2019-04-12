package de.ascendro.f4m.service.json.validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;

/**
 * Validate JSON Messages to JSON schema.
 * 
 * Validator uses schema map with request name and relates {@link Schema}.
 * 
 * JsonMessageValidator uses org.everit.json library 
 */
public class JsonMessageValidator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageValidator.class);

	private final JsonMessageSchemaMap jsonMessageSchemaMap;

	private final Config config;

	/**
	 * Public constructor.
	 * Contains only JSON Schema Map.
	 * @param jsonMessageSchemaMap
	 */
	@Inject
	public JsonMessageValidator(JsonMessageSchemaMap jsonMessageSchemaMap, Config config) {
		this.jsonMessageSchemaMap = jsonMessageSchemaMap;
		this.config = config;
	}

	/** 
	 * Validate JSON Message, 
	 * @param strMessage
	 * @return if message is valid according 
	 * @throws F4MValidationFailedException
	 */
	public void validate(String strMessage) throws F4MValidationFailedException{
		try {
			String messageName = getMessageName(strMessage);
			LOGGER.debug("validate   messageName {} ", messageName);
			Schema schema = getMessageSchema(messageName);
			LOGGER.debug("validate   messageName {} ", messageName);
			if (schema != null) {
				schema.validate(new JSONObject(new JSONTokener(strMessage)));
			} else if (messageName != null) {
				if (config.getPropertyAsBoolean(F4MConfigImpl.SERVICE_MESSAGE_MISSING_SCHEMA_THROWS_EXCEPTION)) {
					throw new F4MValidationFailedException("Json message schema was not found for message type: " + messageName);
				} else {
					LOGGER.warn("Json message schema was not found for message type: " + messageName);
				}
			}
		} catch (JSONException e) {
			throw new F4MValidationFailedException("Invalid JSON content", e);
		} catch (ValidationException e) {
			StringBuilder violations = new StringBuilder();
			getViolations(violations, e);
			LOGGER.error("Input:\n{}\nJSON Schema violations:\n{}", strMessage, violations.toString());
			RuntimeException cause = new RuntimeException(violations.toString(), e); //add violation to stacktrace for easier debugging
			if (config.getPropertyAsBoolean(F4MConfigImpl.SERVICE_MESSAGE_VALIDATION_ERROR_THROWS_EXCEPTION)) {
				throw new F4MValidationFailedException("Json message is not valid", cause);
			} else {
				LOGGER.error("Json message is not valid", cause);
			}
		}
	}

	private void getViolations(StringBuilder violations, ValidationException e) {
		violations.append("<").append(e.getPointerToViolation()).append("> ").append(e.getErrorMessage()).append("\n");
		for (ValidationException ve : e.getCausingExceptions()) {
			getViolations(violations, ve);
		}
	}

	protected Schema getMessageSchema(String messageName) {
		return jsonMessageSchemaMap.get(messageName);
	}

	protected String getMessageName(final String message) {
		String namespace = null;
		if (message.length() > 0) {
			String trimmedMessage = message.replaceAll("\\s+", "");
			int start = trimmedMessage.indexOf(JsonMessage.MESSAGE_NAME_PROPERTY)
					+ JsonMessage.MESSAGE_NAME_PROPERTY.length() + 3;
			int end = trimmedMessage.indexOf(',', start) - 1;
			if (start >= 0 && end > 0) {
				namespace = trimmedMessage.substring(start, end);
			}
		}
		return namespace;
	}

	public void validatePermissions(String messageName, String... userRoles)
			throws F4MInsufficientRightsException {
		final Set<String> messageRoles = jsonMessageSchemaMap.getMessageRoles(messageName);
		if (messageRoles != null) {
			final Set<String> userRoleSet = new HashSet<>(Arrays.asList(userRoles));
			if (Collections.disjoint(messageRoles, userRoleSet)) {
				throw new F4MInsufficientRightsException(String.format("Insufficient roles %s for message [%s] with required one of message permissions %s / roles %s", 
						Arrays.toString(userRoles), messageName, jsonMessageSchemaMap.getMessagePermissions(messageName),
						messageRoles));
			}
		}
	}

}
