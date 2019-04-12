package de.ascendro.f4m.service.profile.model.merge;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class ProfileMergeEvent extends JsonObjectWrapper {

	public static final String PROFILE_MERGE_EVENT_TOPIC = "profile/merge-user";

	public static final String PROPERTY_SOURCE_USER_ID = "sourceUserId";
	public static final String PROPERTY_TARGET_USER_ID = "targetUserId";
	
	public ProfileMergeEvent(JsonObject object) {
		super(object);
	}
	
	public ProfileMergeEvent(String sourceUserId, String targetUserId) {
		setSourceUserId(sourceUserId);
		setTargetUserId(targetUserId);
	}
	
	public ProfileMergeEvent(MergeProfileRequest mergeProfileRequest) {
		this(mergeProfileRequest.getSource(), mergeProfileRequest.getTarget());
	}

	public String getSourceUserId() {
		return getPropertyAsString(PROPERTY_SOURCE_USER_ID);
	}

	public void setSourceUserId(String sourceUserId) {
		setProperty(PROPERTY_SOURCE_USER_ID, sourceUserId);
	}
	
	public String getTargetUserId() {
		return getPropertyAsString(PROPERTY_TARGET_USER_ID);
	}

	public void setTargetUserId(String targetUserId) {
		setProperty(PROPERTY_TARGET_USER_ID, targetUserId);
	}
	
}
