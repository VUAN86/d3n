package de.ascendro.f4m.service.result.engine.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class QuestionStatistics extends JsonObjectWrapper {

	public static final String PROPERTY_ID = "id";
	public static final String PROPERTY_AVERAGE_ANSWER_TIME = "averageAnswerTime";
	public static final String PROPERTY_USERS_ANSWERED = "usersAnswered";

	public QuestionStatistics() {
		// Initialize empty object
	}

	public QuestionStatistics(JsonObject jsonObject) {
		super(jsonObject);
	}

	public String getId() {
		return getPropertyAsString(PROPERTY_ID);
	}
	
	public void setId(String id) {
		setProperty(PROPERTY_ID, id);
	}

	public Double getAverageAnswerTime() {
		return getPropertyAsDoubleObject(PROPERTY_AVERAGE_ANSWER_TIME);
	}

	public void setAverageAnswerTime(Double averageAnswerTime) {
		setProperty(PROPERTY_AVERAGE_ANSWER_TIME, averageAnswerTime);
	}
	
	public Long getUsersAnswered() {
		return getPropertyAsLongObject(PROPERTY_USERS_ANSWERED);
	}
	
	public void setUsersAnswered(Long usersAnswered) {
		setProperty(PROPERTY_USERS_ANSWERED, usersAnswered);
	}
	
}
