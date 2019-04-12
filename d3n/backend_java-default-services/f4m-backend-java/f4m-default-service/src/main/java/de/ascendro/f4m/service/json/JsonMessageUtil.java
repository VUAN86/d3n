package de.ascendro.f4m.service.json;

import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.validation.F4MValidationException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;
import de.ascendro.f4m.service.json.model.type.MessageType;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.util.ServiceUtil;

public class JsonMessageUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageUtil.class);

	/** First three parts of the project package */
	private static final String OWN_PACKAGE_PREFIX;
	static {
		String[] packageParts = JsonMessageUtil.class.getPackage().getName().split("\\.");
		OWN_PACKAGE_PREFIX = packageParts[0] + "." + packageParts[1] + "." + packageParts[2];
	}

	/** Regexp matching messages containing non-null messages */
	private static final Pattern ERROR_PATTERN = Pattern.compile("\"error\"\\s*:(?!\\s*null)");

	private final Gson gson;
	private final ServiceUtil serviceUtil;
	private final JsonMessageValidator jsonMessageValidator;

	@Inject
	public JsonMessageUtil(GsonProvider gsonProvider, ServiceUtil serviceUtil, JsonMessageValidator jsonMessageValidator) {
		gson = gsonProvider.get();
		this.serviceUtil = serviceUtil;
		this.jsonMessageValidator = jsonMessageValidator;
	}

	public <T extends JsonMessageContent> JsonMessage<T> fromJson(String jsonMessage) {
		return gson.<JsonMessage<T>> fromJson(jsonMessage, JsonMessage.class);
	}

	public void validate(String json) throws F4MValidationException {
		if (StringUtils.isBlank(json)) {
			throw new F4MValidationFailedException("Message is mandatory");
		} else if (! ERROR_PATTERN.matcher(json).find() && jsonMessageValidator != null) {
			// Validation should be invoked only if there are no errors
			jsonMessageValidator.validate(json);
		}
	}

	public void validatePermissions(JsonMessage<? extends JsonMessageContent> jsonMessage)
			throws F4MInsufficientRightsException, F4MValidationFailedException {
		if (jsonMessage == null) {
			throw new F4MValidationFailedException("Message is mandatory");
		} else if (jsonMessage.getError() == null && jsonMessage.getClientInfo() != null
				&& jsonMessage.getClientInfo().hasUserInfo()) {
			// Validation should be invoked only if there are no errors
			final List<String> userRoles = new ArrayList<>();
			if (jsonMessage.getClientInfo().hasRoles()) {
				Collections.addAll(userRoles, jsonMessage.getClientInfo().getRoles());
				Collections.addAll(userRoles, getTenantIdRoles(jsonMessage.getClientInfo()));
			}
			jsonMessageValidator.validatePermissions(jsonMessage.getName(),
					userRoles.toArray(new String[userRoles.size()]));
		}
	}

	private String[] getTenantIdRoles(ClientInfo clientInfo) {
		final String[] tenantIdRoles;
		if (clientInfo.getTenantId() != null) {
			tenantIdRoles = Arrays.stream(clientInfo.getRoles())
					.filter(role -> !clientInfo.hasRole(clientInfo.getTenantId(), role))
					.map(role -> StringUtils.removeStart(role, ClientInfo.getTenantPrefix(clientInfo.getTenantId())))
					.toArray(String[]::new);
		} else {
			tenantIdRoles = new String[0];
		}
		return tenantIdRoles;
	}

	public <T> T fromJson(String jsonString, Class<T> jsonStringClass) {
		return gson.<T> fromJson(jsonString, jsonStringClass);
	}

	public <T> T fromJson(String jsonString, Type type) {
		return gson.<T> fromJson(jsonString, type);
	}

	public <T extends JsonMessageContent> T fromJson(JsonElement jsonElement, Class<T> contentClass)
			throws F4MValidationException {
		JsonElement error = jsonElement.getAsJsonObject().get("error");
		if (error == null || error.isJsonNull()) {
			jsonMessageValidator.validate(jsonElement.toString());
		}
		return gson.<T> fromJson(jsonElement, contentClass);
	}

	public <T extends JsonMessageContent> T fromJson(Reader reader) throws JsonSyntaxException {
		return gson.<T> fromJson(reader, JsonMessage.class);
	}

	public String toJson(JsonMessage<? extends JsonMessageContent> jsonMessage) throws F4MValidationException {
		JsonElement jsonTree = gson.toJsonTree(jsonMessage);
		setRequiredFieldsNotNull(jsonTree, jsonMessage);
		if (jsonMessage.getError() == null && jsonMessageValidator != null) {
			jsonMessageValidator.validate(jsonTree.toString());
		}
		return jsonTree.toString();
	}

	private void setRequiredFieldsNotNull(JsonElement jsonTree, Object object) {
	//	LOGGER.debug("setRequiredFieldsNotNull {} ", jsonTree, object);
		if (/*jsonTree != null && */ jsonTree.isJsonObject()) {
			Class<?> clazz = object.getClass();
			do {
				for (Field field : clazz.getDeclaredFields()) {
					boolean resetAccess = false;
					if (!field.isAccessible()) {
						field.setAccessible(true);
						resetAccess = true;
					}
					setNull(field, object, (JsonObject) jsonTree);
					if (resetAccess) {
						field.setAccessible(false);
					}
				}
				clazz = clazz.getSuperclass();
			} while (clazz != Object.class);
		}
	}

	private void setNull(Field field, Object object, JsonObject jsonTree) {
		try {
			Object value = field.get(object);
			if (value == null) {
				if (field.isAnnotationPresent(JsonRequiredNullable.class)) {
					jsonTree.add(getPropertyName(field), null);
				}
			} else {
				Class<?> clazz = value.getClass();
				// FIXME: When needed, should implement these too:
				// if (Collection.class.isAssignableFrom(clazz)) {
				//	... and elements return true to isOwnClass() ...
				if (Map.class.isAssignableFrom(clazz) && field.isAnnotationPresent(JsonRequiredNullable.class)) {
				//	LOGGER.debug("setNull {}+_+_+_+_+_{}+_+_++_+_+{}_+_+_+_+_+_{}_+_+_+_+{}", jsonTree, jsonTree.get(getPropertyName(field)), value, getPropertyName(field), field);

					setMapNull((Map<?, ?>) value, jsonTree, field);
				} if (clazz.isArray() && isOwnClass(clazz.getComponentType())) {
				//	LOGGER.debug("setNull {}+_+_+_+_+_{}+_+_++_+_+{}_+_+_+_+_+_{}_+_+_+_+{}", jsonTree, jsonTree.get(getPropertyName(field)), value, getPropertyName(field), field);

					setArrayNull(value, jsonTree, field);
				} else if (isOwnClass(clazz)) {
					// For our own classes, see if there are more required fields
				//	LOGGER.debug("setNull {}+_+_+_+_+_{}+_+_++_+_+{}_+_+_+_+_+_{}_+_+_+_+{}", jsonTree, jsonTree.get(getPropertyName(field)), value, getPropertyName(field), field);
					setRequiredFieldsNotNull(jsonTree.get(getPropertyName(field)), value);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Could not set a required field", e);
		}
	}

	private void setMapNull(Map<?, ?> value, JsonObject jsonTree, Field field) {
		JsonObject jsonObject = jsonTree.getAsJsonObject(getPropertyName(field));
		if (jsonObject != null) {
			value.forEach((key, val) -> {
				if (val == null) {
					jsonObject.add(key.toString(), JsonNull.INSTANCE);
				}
			});
		}
	}

	private void setArrayNull(Object value, JsonObject jsonTree, Field field) {
		JsonArray jsonArray = jsonTree.getAsJsonArray(getPropertyName(field));
		if (jsonArray != null) {
			final int length = Array.getLength(value);
			for (int i = 0 ; i < length ; i++) {
				JsonElement element = jsonArray.get(i);
				if (element != null) {
					setRequiredFieldsNotNull(element, Array.get(value, i));
				}
			}
		}
	}

	private boolean isOwnClass(Class<?> clazz) {
		return clazz.getPackage() != null && clazz.getName().startsWith(OWN_PACKAGE_PREFIX);
	}

	private String getPropertyName(Field field) {
		final SerializedName serializedName = field.getDeclaredAnnotation(SerializedName.class);
		return serializedName == null ? field.getName() : serializedName.value();
	}

	public JsonElement toJsonElement(JsonMessageContent messageContent) {
		return gson.toJsonTree(messageContent);
	}

	private String getErrorResponseMessageName(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		final String messageResponseName;
		if (originalMessageDecoded != null && originalMessageDecoded.getName() != null) {
			messageResponseName = JsonMessage.getResponseMessageName(originalMessageDecoded.getName());
		} else {
			messageResponseName = JsonMessage.DEFAULT_ERROR_MESSAGE_NAME;
		}
		return messageResponseName;
	}

	public JsonMessage<JsonMessageContent> createResponseErrorMessage(JsonMessageError error,
			JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		final String messageName = getErrorResponseMessageName(originalMessageDecoded);
		final JsonMessage<JsonMessageContent> jsonErrorMessage = createNewMessage(messageName);
		if (originalMessageDecoded != null) {
			setMessageDetailsByOriginalMessage(jsonErrorMessage, originalMessageDecoded);
		}
		jsonErrorMessage.setError(error);
		return jsonErrorMessage;
	}

	public JsonMessage<JsonMessageContent> createResponseErrorMessage(F4MException f4mException,
			JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		return createResponseErrorMessage(new JsonMessageError(f4mException), originalMessageDecoded);
	}

	public JsonMessage<JsonMessageContent> createNewErrorMessage(MessageType type, F4MException f4mException) {
		final JsonMessage<JsonMessageContent> jsonErrorMessage = createNewMessage(type);

		jsonErrorMessage.setError(new JsonMessageError(f4mException));

		return jsonErrorMessage;
	}

	public <T extends JsonMessageContent> JsonMessage<T> createNewMessage(MessageType type) {
		return createNewMessage(type, null);
	}

	public <T extends JsonMessageContent> JsonMessage<T> createNewGatewayMessage(MessageType type, T content,
			String clientId) {
		final JsonMessage<T> message = createNewMessage(type.getMessageName(), content);

		message.setTimestamp(serviceUtil.getMessageTimestamp());
		message.setClientId(clientId);

		return message;
	}

	public <T extends JsonMessageContent> JsonMessage<T> createNewMessage(MessageType type, T content) {
		return createNewMessage(type.getMessageName(), content);
	}

	public <T extends JsonMessageContent> JsonMessage<T> createNewMessage(String name, T content) {
		final JsonMessage<T> message = createNewMessage(name);
		message.setContent(content);
		return message;
	}

	public <T extends JsonMessageContent> JsonMessage<T> createNewMessage(String name) {
		final JsonMessage<T> message = new JsonMessage<>(name);
		message.setSeq(serviceUtil.generateId());
		message.setTimestamp(serviceUtil.getMessageTimestamp());
		return message;
	}

	public <T extends JsonMessageContent> JsonMessage<T> createNewResponseMessage(String messageResponseName,
			T responseMessageContent, JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		JsonMessage<T> responseMessage = createNewMessage(messageResponseName, responseMessageContent);
		if (originalMessageDecoded != null) {
			setMessageDetailsByOriginalMessage(responseMessage, originalMessageDecoded);
		}
		return responseMessage;
	}

	protected void setMessageDetailsByOriginalMessage(JsonMessage<? extends JsonMessageContent> responseMessage,
			JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		responseMessage.setAck(originalMessageDecoded.getSeq());

		String clientId = originalMessageDecoded.getClientId();
		if (clientId != null) {
			responseMessage.setClientId(clientId);
			responseMessage.setTimestamp(serviceUtil.getMessageTimestamp());
		}
	}

	/**
	 * Parses JsonMessage without content/error attributes.
	 * @param originalMessageEncoded
	 * @return
	 */
	public JsonMessage<? extends JsonMessageContent> parseBaseAttributesFromJson(String originalMessageEncoded) {
		JsonObject jsonObject = fromJson(originalMessageEncoded, JsonObject.class);
		JsonObjectWrapper wrapper = new JsonObjectWrapper(jsonObject);
		JsonMessage<JsonMessageContent> message = new JsonMessage<>();
		message.setName(wrapper.getPropertyAsString(JsonMessage.MESSAGE_NAME_PROPERTY));
		message.setSeq(wrapper.getPropertyAsLongObject(JsonMessage.MESSAGE_SEQ_PROPERTY));
		return message;
	}

	public Integer indexOfJsonArrayElement(JsonArray searchArray, JsonElement value) {
		Integer index = null;

		if (searchArray != null) {
			for (int i = 0; i < searchArray.size(); i++) {
				if (value.equals(searchArray.get(i))) {
					index = i;
					break;
				}
			}
		}

		return index;
	}

	public void addToParent(JsonElement parent, String propertyName, JsonElement value, boolean overwriteExisting) {
		if (parent.isJsonObject() && propertyName != null && (overwriteExisting || parent.getAsJsonObject().get(propertyName) == null)) {
			parent.getAsJsonObject().add(propertyName, value);
		} else if (parent.isJsonArray() && (overwriteExisting || parent.getAsJsonArray().size() == 0)) {
			parent.getAsJsonArray().add(value);
		}
	}
}
