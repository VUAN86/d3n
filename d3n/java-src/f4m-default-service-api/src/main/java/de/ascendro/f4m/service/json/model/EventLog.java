package de.ascendro.f4m.service.json.model;

import com.google.gson.JsonObject;

public class EventLog extends JsonObjectWrapper {

	public static final String CATEGORY_POTENTIAL_FRAUD_WARNING = "PotentialFraudWarning";
	
	public static final String PROPERTY_ID = "id";
	public static final String PROPERTY_EVENT_LOG_TYPE = "eventLogType";
	public static final String PROPERTY_CATEGORY = "category";
	public static final String PROPERTY_USER_ID = "userId";
	public static final String PROPERTY_GAME_INSTANCE_ID = "gameInstanceId";
	public static final String PROPERTY_QUESTION_INDEX = "questionIndex";
	public static final String PROPERTY_MESSAGE = "message";

	public EventLog() {
		// Initialize empty object
	}
	
	public EventLog(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	public EventLog(EventLogType type, String category, String message) {
		this(type, category, message, null, null, null);
	}
	
	public EventLog(EventLogType type, String category, String message, String userId) {
		this(type, category, message, userId, null, null);
	}
	
	public EventLog(EventLogType type, String category, String message, String userId, String gameInstanceId) {
		this(type, category, message, userId, gameInstanceId, null);
	}
	
	public EventLog(EventLogType type, String category, String message, String userId, String gameInstanceId, Integer questionIndex) {
		setEventLogType(type);
		setCategory(category);
		setMessage(message);
		setUserId(userId);
		setGameInstanceId(gameInstanceId);
		setQuestionIndex(questionIndex);
	}
	
	public String getId() {
		return getPropertyAsString(PROPERTY_ID);
	}
	
	public void setId(String id) {
		setProperty(PROPERTY_ID, id);
	}
	
	public EventLogType getEventLogType() {
		return EventLogType.fromString(getPropertyAsString(PROPERTY_EVENT_LOG_TYPE));
	}

	public void setEventLogType(EventLogType eventLogType) {
		setProperty(PROPERTY_EVENT_LOG_TYPE, eventLogType == null ? null : eventLogType.getValue());
	}

	public String getCategory() {
		return getPropertyAsString(PROPERTY_CATEGORY);
	}

	public void setCategory(String category) {
		setProperty(PROPERTY_CATEGORY, category);
	}

	public String getUserId() {
		return getPropertyAsString(PROPERTY_USER_ID);
	}

	public void setUserId(String userId) {
		setProperty(PROPERTY_USER_ID, userId);
	}

	public String getGameInstanceId() {
		return getPropertyAsString(PROPERTY_GAME_INSTANCE_ID);
	}

	public void setGameInstanceId(String gameInstanceId) {
		setProperty(PROPERTY_GAME_INSTANCE_ID, gameInstanceId);
	}

	public Integer getQuestionIndex() {
		return getPropertyAsInteger(PROPERTY_QUESTION_INDEX);
	}

	public void setQuestionIndex(Integer questionIndex) {
		setProperty(PROPERTY_QUESTION_INDEX, questionIndex);
	}

	public String getMessage() {
		return getPropertyAsString(PROPERTY_MESSAGE);
	}

	public void setMessage(String message) {
		setProperty(PROPERTY_MESSAGE, message);
	}
	
}
