package de.ascendro.f4m.server.analytics.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class Question extends JsonObjectWrapper {

    public static final String QUESTION_ID_PROPERTY = "questionId";
    public static final String ANSWER_CORRECT_PROPERTY = "answerCorrect";
    public static final String ANSWER_SPEED_PROPERTY = "answerSpeed";
    public static final String OWNER_ID_PROPERTY = "ownerId";


    public Question() {
    }

    public Question(JsonObject questionJsonObject) {
        this.jsonObject = questionJsonObject;
    }

    public void setQuestionId(Long questionId) {
        setProperty(QUESTION_ID_PROPERTY, questionId);
    }

    public Long getQuestionId() {
        return getPropertyAsLong(QUESTION_ID_PROPERTY);
    }

    public void setAnswerIsCorrect(Boolean answerIsCorrect) {
        setProperty(ANSWER_CORRECT_PROPERTY, answerIsCorrect);
    }

    public Boolean isAnswerCorrect() {
        return getPropertyAsBoolean(ANSWER_CORRECT_PROPERTY);
    }

    public void setAnswerSpeed(Long answerSpeed) {
        setProperty(ANSWER_SPEED_PROPERTY, answerSpeed);
    }

    public Long getAnswerSpeed() {
        return getPropertyAsLong(ANSWER_SPEED_PROPERTY);
    }

    public void setOwnerId(String ownerId) {
        setProperty(OWNER_ID_PROPERTY, ownerId);
    }

    public String getOwnerId() {
        return getPropertyAsString(OWNER_ID_PROPERTY);
    }
}
