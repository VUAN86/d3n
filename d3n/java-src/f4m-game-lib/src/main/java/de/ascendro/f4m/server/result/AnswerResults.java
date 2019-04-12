package de.ascendro.f4m.server.result;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class AnswerResults extends JsonObjectWrapper {

	public static final String QUESTION_INDEX_PROPERTY = "questionIndex";
	public static final String RESULT_ITEMS_PROPERTY = "resultItems";
	public static final String QUESTION_INFO_PROPERTY = "questionInfo";

	public AnswerResults() {
		// Initialize empty object
	}
	
	public AnswerResults(int questionIndex) {
		setQuestionIndex(questionIndex);
	}
	
	public AnswerResults(JsonObject results) {
		super(results);
	}
	
	public int getQuestionIndex() {
		return getPropertyAsInt(QUESTION_INDEX_PROPERTY);
	}
	
	public void setQuestionIndex(int index) {
		setProperty(QUESTION_INDEX_PROPERTY, index);
	}
	
	public Map<ResultType, ResultItem> getResultItems() {
		JsonArray resultItems = getArray(RESULT_ITEMS_PROPERTY);
		if (resultItems == null || resultItems.size() == 0) {
			return Collections.emptyMap();
		}
		Map<ResultType, ResultItem> results = new EnumMap<>(ResultType.class);
		resultItems.forEach(item -> {
			if (item.isJsonObject()) {
				ResultItem resultItem = new ResultItem(item.getAsJsonObject());
				if (resultItem.getResultType() != null) {
					results.put(resultItem.getResultType(), resultItem);
				}
			}
		});
		return results;
	}

	public void addResultItem(ResultItem resultItem) {
		JsonArray resultItems = getArray(RESULT_ITEMS_PROPERTY);
		if (resultItems == null) {
			resultItems = new JsonArray();
			jsonObject.add(RESULT_ITEMS_PROPERTY, resultItems);
		}
		resultItems.add(resultItem.getJsonObject());
	}
	
	public void addQuestionInfo(QuestionInfo questionInfo) {
		jsonObject.add(QUESTION_INFO_PROPERTY, questionInfo.getJsonObject());
	}
	
	public QuestionInfo getQuestionInfo() {
		JsonObject infoJsonObject = getPropertyAsJsonObject(QUESTION_INFO_PROPERTY);
		if (infoJsonObject == null) {
			infoJsonObject = new JsonObject();
		}
		return new QuestionInfo(infoJsonObject);
	}

}
