package de.ascendro.f4m.service.game.engine.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.annotations.SerializedName;

public class Question extends QuestionKey {
    public static final String ID_PROPERTY = "id";

    @SerializedName(value = ID_PROPERTY)
	private String id;

	@SerializedName(value = "questionCreatorResourceId")
    private String creatorResourceId;

	private String[] answers;
	private String[] correctAnswers;
	private long[] answerMaxTimes;

	private int stepCount = 1;
	private String[] questionBlobKeys;
	private String[] decryptionKeys;

	private QuestionIndex multiIndex;

	@SerializedName(value = "questionResolutionImage")
	private String resolutionImage;

	private String source;

	@SerializedName(value = "questionResolutionText")
	private String resolutionText;

	@SerializedName(value = "questionExplanation")
	private String explanation;

	private String hint;
	
	public String[] medias;

	public Set<String> getQuestionBlobKeys() {
		return toSet(questionBlobKeys);
	}

	public Set<String> getQuestionImageBlobKeys() {
		return toSet(medias);
	}

	public Set<String> getDecryptionKeys() {
		return toSet(decryptionKeys);
	}

	public String getQuestionBlobKey(int step) {
		final String question;
		if (questionBlobKeys != null && step < questionBlobKeys.length) {
			question = questionBlobKeys[step];
		} else {
			question = null;
		}
		return question;
	}

	public String getDecryptionKey(int step) {
		final String decryptionKey;
		if (decryptionKeys != null && step < decryptionKeys.length) {
			decryptionKey = decryptionKeys[step];
		} else {
			decryptionKey = null;
		}
		return decryptionKey;
	}

	public long getTotalAnswerMaxTimes() {
		long[] answerMaxMsT = getAnswerMaxTimes();
		long result = 0;
		if (answerMaxMsT != null) {
			for (long stepAnswerMaxMsT : answerMaxMsT) {
				result += stepAnswerMaxMsT;
			}
		}
		return result;
	}

	private Set<String> toSet(String[] array) {
		final Set<String> set = new LinkedHashSet<>();
		Collections.addAll(set, Optional.ofNullable(array).orElse(new String[0]));
		return set;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String[] getAnswers() {
		return answers;
	}

	public void setAnswers(String[] answers) {
		this.answers = answers;
	}

	public String[] getCorrectAnswers() {
		return correctAnswers;
	}

	public void setCorrectAnswers(String...correctAnswers) {
		this.correctAnswers = correctAnswers;
	}

	public long[] getAnswerMaxTimes() {
		return answerMaxTimes;
	}
	
    public long getAnswerMaxTime(int step) {
        return answerMaxTimes[step];
    }

	public void setAnswerMaxTimes(long[] answerMaxTimes) {
		this.answerMaxTimes = answerMaxTimes;
	}

	public int getStepCount() {
		return stepCount;
	}

	public void setStepCount(int stepCount) {
		this.stepCount = stepCount;
	}

	public QuestionIndex getMultiIndex() {
		return multiIndex;
	}

	public void setMultiIndex(QuestionIndex multiIndex) {
		this.multiIndex = multiIndex;
	}

	public String getResolutionImage() {
		return resolutionImage;
	}

	public void setResolutionImage(String resolutionImage) {
		this.resolutionImage = resolutionImage;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getResolutionText() {
		return resolutionText;
	}

	public void setResolutionText(String resolutionText) {
		this.resolutionText = resolutionText;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}

	public String[] getMedias() {
		return medias;
	}

	public void setMedias(String[] medias) {
		this.medias = medias;
	}

	public void setQuestionBlobKeys(String[] questionBlobKeys) {
		this.questionBlobKeys = questionBlobKeys;
	}

	public void setDecryptionKeys(String[] decryptionKeys) {
		this.decryptionKeys = decryptionKeys;
	}

	public String getCreatorResourceId() {
		return creatorResourceId;
	}

	public void setCreatorResourceId(String creatorResourceId) {
		this.creatorResourceId = creatorResourceId;
	}	

	public String getHint() {
		return hint;
	}
	
	public void setHint(String hint) {
		this.hint = hint;
	}

	/**
	 * Get the answers to be removed on use of 50/50 chance joker.
	 * Will return the same answers for the same question.
	 * @return <code>null</code> if no answers may be removed, otherwise answers to be removed
	 */
	public String[] calculateRemovedAnswers() {
		int answersToRemove = answers.length / 2;
		if (answersToRemove > 0) {
			Set<String> removableAnswers = new TreeSet<>(Arrays.asList(answers));
			removableAnswers.removeAll(Arrays.asList(correctAnswers));
			if (removableAnswers.size() >= answersToRemove) {
				List<String> removedAnswers = new ArrayList<>(removableAnswers);
				Random random = new Random(id.hashCode());
				Collections.shuffle(removedAnswers, random);
				return removedAnswers.subList(0, answersToRemove).toArray(new String[answersToRemove]);
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Question [id=");
		builder.append(id);
		builder.append(", creatorResourceId=");
		builder.append(creatorResourceId);
		builder.append(", answers=");
		builder.append(Arrays.toString(answers));
		builder.append(", correctAnswers=");
		builder.append(Arrays.toString(correctAnswers));
		builder.append(", answerMaxTimes=");
		builder.append(Arrays.toString(answerMaxTimes));
		builder.append(", stepCount=");
		builder.append(stepCount);
		builder.append(", questionBlobKeys=");
		builder.append(Arrays.toString(questionBlobKeys));
		builder.append(", decryptionKeys=");
		builder.append(Arrays.toString(decryptionKeys));
		builder.append(", multiIndex=");
		builder.append(multiIndex);
		builder.append(", resolutionImage=");
		builder.append(resolutionImage);
		builder.append(", source=");
		builder.append(source);
		builder.append(", resolutionText=");
		builder.append(resolutionText);
		builder.append(", explanation=");
		builder.append(explanation);
		builder.append(", hint=");
		builder.append(hint);
		builder.append(", medias=");
		builder.append(Arrays.toString(medias));
		builder.append(", toString()=");
		builder.append(super.toString());
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(answerMaxTimes);
		result = prime * result + Arrays.hashCode(answers);
		result = prime * result + Arrays.hashCode(correctAnswers);
		result = prime * result + ((creatorResourceId == null) ? 0 : creatorResourceId.hashCode());
		result = prime * result + Arrays.hashCode(decryptionKeys);
		result = prime * result + ((explanation == null) ? 0 : explanation.hashCode());
		result = prime * result + ((hint == null) ? 0 : hint.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + Arrays.hashCode(medias);
		result = prime * result + ((multiIndex == null) ? 0 : multiIndex.hashCode());
		result = prime * result + Arrays.hashCode(questionBlobKeys);
		result = prime * result + ((resolutionImage == null) ? 0 : resolutionImage.hashCode());
		result = prime * result + ((resolutionText == null) ? 0 : resolutionText.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + stepCount;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Question other = (Question) obj;
		if (!Arrays.equals(answerMaxTimes, other.answerMaxTimes))
			return false;
		if (!Arrays.equals(answers, other.answers))
			return false;
		if (!Arrays.equals(correctAnswers, other.correctAnswers))
			return false;
		if (creatorResourceId == null) {
			if (other.creatorResourceId != null)
				return false;
		} else if (!creatorResourceId.equals(other.creatorResourceId))
			return false;
		if (!Arrays.equals(decryptionKeys, other.decryptionKeys))
			return false;
		if (explanation == null) {
			if (other.explanation != null)
				return false;
		} else if (!explanation.equals(other.explanation))
			return false;
		if (hint == null) {
			if (other.hint != null)
				return false;
		} else if (!hint.equals(other.hint))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (!Arrays.equals(medias, other.medias))
			return false;
		if (multiIndex == null) {
			if (other.multiIndex != null)
				return false;
		} else if (!multiIndex.equals(other.multiIndex))
			return false;
		if (!Arrays.equals(questionBlobKeys, other.questionBlobKeys))
			return false;
		if (resolutionImage == null) {
			if (other.resolutionImage != null)
				return false;
		} else if (!resolutionImage.equals(other.resolutionImage))
			return false;
		if (resolutionText == null) {
			if (other.resolutionText != null)
				return false;
		} else if (!resolutionText.equals(other.resolutionText))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (stepCount != other.stepCount)
			return false;
		return true;
	}
	
}
