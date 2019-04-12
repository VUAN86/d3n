package de.ascendro.f4m.service.game.engine.model.start.game;

import java.util.Arrays;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class StartGameResponse implements JsonMessageContent {
	private String gameInstanceId;

	/**
	 * Question decryption keys (offline game only)
	 */
	private String[] decryptionKeys;

	/**
	 * Question media keys
	 */
	private String[] questionBlobKeys;

	private String[] winningComponentIds;

	private String[] voucherIds;

	private String[] advertisementBlobKeys;

	private String loadingBlobKey;

	private int numberOfQuestions;

	private int advertisementDuration;

	private int loadingScreenDuration;

	private boolean advertisementSkipping;

	private String[] questionImageBlobKeys;

	public StartGameResponse() {
		//Empty constructor
	}

	public StartGameResponse(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public String[] getDecryptionKeys() {
		return decryptionKeys;
	}

	public void setDecryptionKeys(String[] decryptionKeys) {
		this.decryptionKeys = decryptionKeys;
	}

	public String[] getQuestionBlobKeys() {
		return questionBlobKeys;
	}

	public void setQuestionBlobKeys(String[] questionBlobKeys) {
		this.questionBlobKeys = questionBlobKeys;
	}

	public String[] getWinningComponentIds() {
		return winningComponentIds;
	}

	public void setWinningComponentIds(String[] winningComponentIds) {
		this.winningComponentIds = winningComponentIds;
	}

	public String[] getVoucherIds() {
		return voucherIds;
	}

	public void setVoucherIds(String[] voucherIds) {
		this.voucherIds = voucherIds;
	}

	public String[] getAdvertisementBlobKeys() {
		return advertisementBlobKeys;
	}

	public void setAdvertisementBlobKeys(String[] advertisementBlobKeys) {
		this.advertisementBlobKeys = advertisementBlobKeys;
	}

	public void setAdvertisementDuration(int advertisementDuration) {
		this.advertisementDuration = advertisementDuration;
	}

	public void setLoadingScreenDuration(int loadingScreenDuration) {
		this.loadingScreenDuration = loadingScreenDuration;
	}

	public void setAdvertisementSkipping(boolean advertisementSkipping) {
		this.advertisementSkipping = advertisementSkipping;
	}

	public int getAdvertisementDuration() {
		return advertisementDuration;
	}

	public int getLoadingScreenDuration() {
		return loadingScreenDuration;
	}

	public boolean isAdvertisementSkipping() {
		return advertisementSkipping;
	}

	public String getLoadingBlobKey() {
		return loadingBlobKey;
	}

	public void setNumberOfQuestions(int numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
	}

	public int getNumberOfQuestions() {

		return numberOfQuestions;
	}

	public void setLoadingBlobKey(String loadingBlobKey) {
		this.loadingBlobKey = loadingBlobKey;
	}
	
	public String[] getQuestionImageBlobKeys() {
		return questionImageBlobKeys;
	}

	public void setQuestionImageBlobKeys(String[] questionImageBlobKeys) {
		this.questionImageBlobKeys = questionImageBlobKeys;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StartGameResponse [gameInstanceId=");
		builder.append(gameInstanceId);
		builder.append(", decryptionKeys=");
		builder.append(Arrays.toString(decryptionKeys));
		builder.append(", questionBlobKeys=");
		builder.append(Arrays.toString(questionBlobKeys));
		builder.append(", winningComponentIds=");
		builder.append(Arrays.toString(winningComponentIds));
		builder.append(", voucherIds=");
		builder.append(Arrays.toString(voucherIds));
		builder.append(", advertisementBlobKeys=");
		builder.append(Arrays.toString(advertisementBlobKeys));
		builder.append(", loadingBlobKeys=");
		builder.append(loadingBlobKey);
		builder.append(", advertisementDuration=");
		builder.append(advertisementDuration);
		builder.append(", loadingScreenDuration=");
		builder.append(loadingScreenDuration);
		builder.append(", advertisementSkipping=");
		builder.append(advertisementSkipping);
		builder.append(", questionImageBlobKeys=");
		builder.append(Arrays.toString(questionImageBlobKeys));
		builder.append("]");
		return builder.toString();
	}
}
