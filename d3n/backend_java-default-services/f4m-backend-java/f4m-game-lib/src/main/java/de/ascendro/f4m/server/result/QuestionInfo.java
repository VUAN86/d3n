package de.ascendro.f4m.server.result;

import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import de.ascendro.f4m.service.game.engine.model.Answer;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class QuestionInfo extends JsonObjectWrapper {

	public static final String QUESTION_INDEX_PROPERTY = "questionIndex";
	public static final String PROVIDED_ANSWERS_PROPERTY = "providedAnswers";
	public static final String PROPERTY_ORIGINAL_ORDER_ANSWERS = "originalOrderAnswers";
	public static final String ANSWERED_CORRECTLY_PROPERTY = "answeredCorrectly";
	public static final String SKIPPED_PROPERTY = "skipped";
	
    public static final String ID_PROPERTY = "id";
    public static final String CORRECT_ANSWERS_PROPERTY = "correctAnswers";
    public static final String ANSWER_MAX_TIMES_PROPERTY = "answerMaxTimes";
    public static final String QUESTION_BLOB_KEYS_PROPERTY = "questionBlobKeys";
    public static final String DECRYPTION_KEYS_PROPERTY = "decryptionKeys";
    
    public static final String COMPLEXITY_PROPERTY = "complexity";
    
    public static final String RESOLUTION_IMAGE_PROPERTY = "questionResolutionImage";
    public static final String SOURCE_PROPERTY = "source";
    public static final String RESOLUTION_TEXT_PROPERTY = "questionResolutionText";
    public static final String EXPLANATION_PROPERTY = "questionExplanation";
    
	public static final String QUESTION_IMAGE_BLOB_KEYS_PROPERTY = "questionImageBlobKeys";

	public QuestionInfo() {
		// initialize empty object
	}

	public QuestionInfo(JsonObject resultQuestion) {
		super(resultQuestion);
	}

	public QuestionInfo(int questionIndex, Question question, Answer answer, boolean isCorrectAnswer, boolean isSkipped) {
		setQuestionIndex(questionIndex);
		setQuestionBlobKeys(question.getQuestionBlobKeys());
		setQuestionImageBlobKeys(question.getQuestionImageBlobKeys());
		setCorrectAnswers(question.getCorrectAnswers());
		setProvidedAnswers(answer == null ? new String[0] : answer.getAnswers());
		setAnsweredCorrectly(isCorrectAnswer);
		setSkipped(isSkipped);
		setResolutionImage(question.getResolutionImage());
		setSource(question.getSource());
		setResolutionText(question.getResolutionText());
		setExplanation(question.getExplanation());
		setDecriptionKeys(question.getDecryptionKeys());
		setComplexity(question.getComplexity());
		setAnswerMaxMsT(question.getAnswerMaxTimes());
	}

	public void setQuestionIndex(int index) {
		setProperty(QUESTION_INDEX_PROPERTY, index);
	}

	public int getQuestionIndex() {
		return getPropertyAsInt(QUESTION_INDEX_PROPERTY);
	}

	public void setQuestionBlobKeys(Set<String> questionBlobKeys) {
		setArray(QUESTION_BLOB_KEYS_PROPERTY, questionBlobKeys);
	}

	public Set<String> getQuestionBlobKeys() {
		return getPropertyAsStringSet(QUESTION_BLOB_KEYS_PROPERTY);
	}

	public void setQuestionImageBlobKeys(Set<String> questionImageBlobKeys) {
		setArray(QUESTION_IMAGE_BLOB_KEYS_PROPERTY, questionImageBlobKeys);
	}

	public Set<String> getQuestionImageBlobKeys() {
		return getPropertyAsStringSet(QUESTION_IMAGE_BLOB_KEYS_PROPERTY);
	}

	public void setCorrectAnswers(String[] correctAnswers) {
		setArray(CORRECT_ANSWERS_PROPERTY, correctAnswers);
	}

	public String[] getCorrectAnswers() {
		return getPropertyAsStringArray(CORRECT_ANSWERS_PROPERTY);
	}

	public void setProvidedAnswers(String[] providedAnswers) {
		setArray(PROVIDED_ANSWERS_PROPERTY, providedAnswers);
	}

	public String[] getProvidedAnswers() {
		return getPropertyAsStringArray(PROVIDED_ANSWERS_PROPERTY);
	}

	public void setPropertyOriginalOrderAnswers(String[] originalOrderAnswers) {
		setArray(PROPERTY_ORIGINAL_ORDER_ANSWERS, originalOrderAnswers);
	}

	public String[] getPropertyOriginalOrderAnswers() {
		return getPropertyAsStringArray(PROPERTY_ORIGINAL_ORDER_ANSWERS);
	}

	public void setResolutionImage(String resolutionImage) {
		setProperty(RESOLUTION_IMAGE_PROPERTY, resolutionImage);
	}

	public boolean isAnsweredCorrectly() {
		Boolean result = getPropertyAsBoolean(ANSWERED_CORRECTLY_PROPERTY);
		return result == null ? false : result;
	}

	public void setAnsweredCorrectly(boolean answeredCorrectly) {
		setProperty(ANSWERED_CORRECTLY_PROPERTY, answeredCorrectly);
	}

	public boolean isSkipped() {
		Boolean result = getPropertyAsBoolean(SKIPPED_PROPERTY);
		return result == null ? false : result;
	}
	
	public void setSkipped(boolean skipped) {
		setProperty(SKIPPED_PROPERTY, skipped);
	}
	
	public String getResolutionImage() {
		return getPropertyAsString(RESOLUTION_IMAGE_PROPERTY);
	}

	public void setSource(String source) {
		setProperty(SOURCE_PROPERTY, source);
	}

	public String getSource() {
		return getPropertyAsString(SOURCE_PROPERTY);
	}

	public void setResolutionText(String resolutionText) {
		setProperty(RESOLUTION_TEXT_PROPERTY, resolutionText);
	}

	public String getResolutionText() {
		return getPropertyAsString(RESOLUTION_TEXT_PROPERTY);
	}

	public void setExplanation(String explanation) {
		setProperty(EXPLANATION_PROPERTY, explanation);
	}

	public String getExplanation() {
		return getPropertyAsString(EXPLANATION_PROPERTY);
	}

	public void setComplexity(int complexity) {
		setProperty(COMPLEXITY_PROPERTY, complexity);
	}

	public int getComplexity() {
		return getPropertyAsInt(COMPLEXITY_PROPERTY);
	}
	
	public String[] getDecryptionKeys() {
		return getPropertyAsStringArray(DECRYPTION_KEYS_PROPERTY);
	}
	
	public void setDecriptionKeys(Set<String> decriptionKeys) {
		setArray(DECRYPTION_KEYS_PROPERTY, decriptionKeys);
	}

	public long[] getAnswerMaxMsT() {
		return getPropertyAsLongArray(ANSWER_MAX_TIMES_PROPERTY);
	}

	public long getAnswerMaxTime(int step) {
		return getAnswerMaxMsT()[step];
	}
	
	public void setAnswerMaxMsT(long[] answerMaxTimes) {
		if (answerMaxTimes != null) {
			for (long answerMaxTime : answerMaxTimes) {
				addElementToArray(ANSWER_MAX_TIMES_PROPERTY, new JsonPrimitive(answerMaxTime));
			}
		}
	}

}
