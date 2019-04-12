package de.ascendro.f4m.service.profile.model.merge;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class MergeProfileRequest implements JsonMessageContent {
	private String source;
	private String target;

	public MergeProfileRequest() {
	}

	public MergeProfileRequest(String source, String target) {
		this.source = source;
		this.target = target;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

}
