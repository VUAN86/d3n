package de.ascendro.f4m.service.json;

import static de.ascendro.f4m.service.json.model.user.ClientInfo.MESSAGE_CLIENT_INFO_APP_CONFIG_PROPERTY;
import static de.ascendro.f4m.service.json.model.user.ClientInfo.MESSAGE_CLIENT_INFO_PROFILE_PROPERTY;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.util.F4MEnumUtils;

public class JsonMessageDeserializer implements JsonDeserializer<JsonMessage<? extends JsonMessageContent>>, 
	JsonSerializer<JsonMessage<? extends JsonMessageContent>> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageDeserializer.class);
	public static final Type CLIENT_INFO_TYPE = new TypeToken<ClientInfo>() {}.getType();

	private final JsonMessageTypeMap jsonMessageTypeMap;

	@Inject
	public JsonMessageDeserializer(JsonMessageTypeMap jsonMessageTypeMap) {
		this.jsonMessageTypeMap = jsonMessageTypeMap;
	}

	@Override
	public JsonMessage<? extends JsonMessageContent> deserialize(JsonElement jsonElement, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		final JsonObject messageJsonObject = jsonElement.getAsJsonObject();

		String name = readMandatoryAttribute(messageJsonObject, JsonMessage.MESSAGE_NAME_PROPERTY);
		final JsonMessage<JsonMessageContent> message = new JsonMessage<>(name);

		final JsonMessageContent content = parseJsonMessageContent(message,
				messageJsonObject.get(JsonMessage.MESSAGE_CONTENT_PROPERTY), context);
		ClientInfo clientInfo = parseClientInfo(context, messageJsonObject.get(JsonMessage.MESSAGE_CLIENT_INFO_PROPERTY));
		clientInfo = parseClientId(clientInfo, messageJsonObject.get(JsonMessage.MESSAGE_CLIENT_ID_PROPERTY));
		final Long timestamp = parseJsonMessageTimestamp(messageJsonObject);

		message.setClientInfo(clientInfo);
		message.setName(name);
		message.setContent(content);
		message.setTimestamp(timestamp);
		deserializeOtherFields(message, messageJsonObject, context);

		return message;
	}

	private ClientInfo parseClientId(ClientInfo clientInfo, JsonElement jsonElement) {
		final String clientId;
		if (jsonElement != null && jsonElement.isJsonPrimitive() && !jsonElement.isJsonNull()) {
			clientId = jsonElement.getAsString();
			
			final ClientInfo resultClientInfo;
			if (clientInfo != null) {
				resultClientInfo = clientInfo;
			} else {
				resultClientInfo = new ClientInfo();
			}			
			
			resultClientInfo.setClientId(clientId);

			return resultClientInfo;
		} else {
			return clientInfo;
		}
	}

	protected ClientInfo parseClientInfo(JsonDeserializationContext context, JsonElement jsonElement) {
		final ClientInfo clientInfo;
		if (jsonElement != null && jsonElement.isJsonObject()) {
			clientInfo = new ClientInfo();
			final JsonObject clientInfoJsonObject = jsonElement.getAsJsonObject();

			// profile
			final JsonElement profileJsonElement = clientInfoJsonObject.get(MESSAGE_CLIENT_INFO_PROFILE_PROPERTY);
			final ClientInfo clientInfoProfile = context.deserialize(profileJsonElement, CLIENT_INFO_TYPE);
			if (clientInfoProfile != null) {
				clientInfo.setUserId(clientInfoProfile.getUserId());
				clientInfo.setLanguage(clientInfoProfile.getLanguage());
				clientInfo.setHandicap(clientInfoProfile.getHandicap());
				clientInfo.setRoles(clientInfoProfile.getRoles());
				clientInfo.setEmails(clientInfoProfile.getEmails());
				clientInfo.setPhones(clientInfoProfile.getPhones());
			}

			// app config
			final JsonElement appConfigJsonElement = clientInfoJsonObject.get(MESSAGE_CLIENT_INFO_APP_CONFIG_PROPERTY);
			final ClientInfo clientInfoAppConfig = context.deserialize(appConfigJsonElement, CLIENT_INFO_TYPE);
			if (clientInfoAppConfig != null) {
				clientInfo.setAppId(clientInfoAppConfig.getAppId());
				clientInfo.setTenantId(clientInfoAppConfig.getTenantId());
				clientInfo.setDeviceUUID(clientInfoAppConfig.getDeviceUUID());
			}
			
			if (clientInfoJsonObject.has(ClientInfo.MESSAGE_CLIENT_INFO_IP_PROPERTY)
					&& clientInfoJsonObject.get(ClientInfo.MESSAGE_CLIENT_INFO_IP_PROPERTY).isJsonPrimitive()) {
				clientInfo.setIp(clientInfoJsonObject.get(ClientInfo.MESSAGE_CLIENT_INFO_IP_PROPERTY).getAsString());
			}
			if (clientInfoJsonObject.has(ClientInfo.MESSAGE_CLIENT_INFO_COUNTRY_CODE)
					&& clientInfoJsonObject.get(ClientInfo.MESSAGE_CLIENT_INFO_COUNTRY_CODE).isJsonPrimitive()) {
				ISOCountry countryCode = F4MEnumUtils.getEnum(ISOCountry.class,
						clientInfoJsonObject.get(ClientInfo.MESSAGE_CLIENT_INFO_COUNTRY_CODE).getAsString());
				clientInfo.setCountryCode(countryCode);
			}
			if (clientInfoJsonObject.has(ClientInfo.MESSAGE_CLIENT_INFO_ORIGIN_COUNTRY)
					&& clientInfoJsonObject.get(ClientInfo.MESSAGE_CLIENT_INFO_ORIGIN_COUNTRY).isJsonPrimitive()) {
				ISOCountry originCountry = F4MEnumUtils.getEnum(ISOCountry.class,
						clientInfoJsonObject.get(ClientInfo.MESSAGE_CLIENT_INFO_ORIGIN_COUNTRY).getAsString());
				clientInfo.setOriginCountry(originCountry);
			}
			
		} else {
			clientInfo = null;
		}
		return clientInfo;
	}

	private String readMandatoryAttribute(final JsonObject messageJsonObject, String propertyName) {
		JsonElement messageNameElement = messageJsonObject.get(propertyName);
		if (messageNameElement == null) {
			throw new F4MValidationFailedException("Mandatory attribute '" + propertyName + "' is missing");
		}
		return messageNameElement.getAsString();
	}

	protected void deserializeOtherFields(JsonMessage<JsonMessageContent> message, JsonObject messageJsonObject,
			JsonDeserializationContext context) {
		for (Field field : JsonMessage.class.getDeclaredFields()) {
			final SerializedName serializedName = field.getDeclaredAnnotation(SerializedName.class);
			final String propertyName = getPropertyName(serializedName, field);

			if (isOtherField(propertyName)) {
				final JsonElement propertyElement = messageJsonObject.get(propertyName);
				setFieldValue(message, field, propertyElement, context);
			}
		}
	}

	protected boolean isOtherField(String propertyName) {
		return !(propertyName.equalsIgnoreCase(JsonMessage.MESSAGE_NAME_PROPERTY)
				|| propertyName.equalsIgnoreCase(JsonMessage.MESSAGE_CONTENT_PROPERTY)
				|| propertyName.equalsIgnoreCase(JsonMessage.MESSAGE_CLIENT_INFO_PROPERTY));
	}

	protected String getPropertyName(SerializedName serializedName, Field field) {
		final String propertyName;

		if (serializedName != null) {
			propertyName = serializedName.value();
		} else {
			propertyName = field.getName();
		}

		return propertyName;
	}

	protected void setFieldValue(JsonMessage<?> message, Field field, JsonElement propertyElement,
			JsonDeserializationContext context) {
		if (propertyElement != null) {
			final Object fieldValue = context.deserialize(propertyElement, field.getGenericType());
			try {
				field.setAccessible(true);
				field.set(message, fieldValue);
				field.setAccessible(false);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.error("Cannot set field[" + field.getName() + "]: " + e);
			}
		}
	}

	protected JsonMessageContent parseJsonMessageContent(JsonMessage<JsonMessageContent> message,
			JsonElement contentElement, JsonDeserializationContext context) {
		JsonMessageContent jsonMessageContent = null;
		Type type = getMessageType(message);
		if (type != null) {
			jsonMessageContent = context.deserialize(contentElement, type);
		} else if (contentElement.isJsonObject() && contentElement.getAsJsonObject() != null 
				&& contentElement.getAsJsonObject().entrySet().isEmpty()) {
			jsonMessageContent = new EmptyJsonMessageContent();
		} else if (! contentElement.isJsonNull()) {
			LOGGER.warn("JSON content of type {} cannot be recognized {}", message.getTypeName(), contentElement);
			// XXX: Consider, if assert should be used here - if unrecognized
			// JSON attribute is standard case in tests,
			// exception should be used. If it is a normal business case, assert
			// should be removed.
			// assert false : "JSON content of type " + messageName + " cannot
			// be recognized";
			// throw new RuntimeException("JSON content of type '" + messageName
			// + "' cannot be recognized");
		}
		return jsonMessageContent;
	}

	protected Type getMessageType(JsonMessage<JsonMessageContent> message) {
		Type type = jsonMessageTypeMap.get(message.getName());
		// workaround for two approaches - register request and response as
		// single class or separate into two different model classes.
		if (type == null) {
			type = jsonMessageTypeMap.get(message.getTypeName());
		}
		return type;
	}

	protected Long parseJsonMessageTimestamp(JsonObject messageJsonObject) {
		Long timestamp = null;
		final JsonElement timestampElement = messageJsonObject.get(JsonMessage.MESSAGE_TIMESTAMP_PROPERTY);
		if (timestampElement != null && !(timestampElement instanceof JsonNull)) {
			timestamp = timestampElement.getAsLong();
		}
		return timestamp;
	}

	@Override
	public JsonElement serialize(JsonMessage<? extends JsonMessageContent> message, Type type,
			JsonSerializationContext context) {
		final JsonElement jsonMessageElement = new GsonProvider(null).get().toJsonTree(message);

		if(jsonMessageElement.isJsonObject()){
			final JsonObject jsonMessageObject = jsonMessageElement.getAsJsonObject();
			if(jsonMessageObject.has(JsonMessage.MESSAGE_CLIENT_INFO_PROPERTY) &&
					jsonMessageObject.get(JsonMessage.MESSAGE_CLIENT_INFO_PROPERTY).isJsonObject()){
				final JsonObject clientInfoJsonObject = jsonMessageObject.get(JsonMessage.MESSAGE_CLIENT_INFO_PROPERTY)
						.getAsJsonObject();
				
				//profile
				final JsonObject profile = new JsonObject();
				moveProperty(ClientInfo.MESSAGE_CLIENT_INFO_PROFILE_USER_ID_PROPERTY, clientInfoJsonObject, profile);
				moveProperty(ClientInfo.MESSAGE_CLIENT_INFO_PROFILE_LANGUAGE_PROPERTY, clientInfoJsonObject, profile);
				moveProperty(ClientInfo.MESSAGE_CLIENT_INFO_PROFILE_HANDICAP_PROPERTY, clientInfoJsonObject, profile);
				moveProperty(ClientInfo.MESSAGE_CLIENT_INFO_PROFILE_ROLES_PROPERTY, clientInfoJsonObject, profile);
				moveProperty(ClientInfo.MESSAGE_CLIENT_INFO_PROFILE_EMAILS_PROPERTY, clientInfoJsonObject, profile);
				moveProperty(ClientInfo.MESSAGE_CLIENT_INFO_PROFILE_PHONES_PROPERTY, clientInfoJsonObject, profile);
				clientInfoJsonObject.add(ClientInfo.MESSAGE_CLIENT_INFO_PROFILE_PROPERTY, profile);

				//app config
				final JsonObject appConfig = new JsonObject();
				moveProperty(ClientInfo.MESSAGE_CLIENT_INFO_APP_CONFIG_APP_ID_PROPERTY, clientInfoJsonObject, appConfig);
				moveProperty(ClientInfo.MESSAGE_CLIENT_INFO_APP_CONFIG_TENANT_ID_PROPERTY, clientInfoJsonObject, appConfig);
				moveProperty(ClientInfo.MESSAGE_CLIENT_INFO_APP_CONFIG_DEVICE_UUID_PROPERTY, clientInfoJsonObject, appConfig);
				clientInfoJsonObject.add(ClientInfo.MESSAGE_CLIENT_INFO_APP_CONFIG_PROPERTY, appConfig);
				
				//client id
				moveProperty(JsonMessage.MESSAGE_CLIENT_ID_PROPERTY, clientInfoJsonObject, jsonMessageElement.getAsJsonObject());
			}
		}
		
		return jsonMessageElement;
	}
	
	private void moveProperty(String propertyName, JsonObject sourceObject, JsonObject targetObject){
		targetObject.add(propertyName, sourceObject.get(propertyName));
		sourceObject.remove(propertyName);
	}

}
