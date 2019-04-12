package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.friend.model.api.ApiBuddy;
import de.ascendro.f4m.service.friend.model.api.ApiContact;
import de.ascendro.f4m.service.friend.model.api.ApiProfile;

public class ApiContactListResult {

	private ApiBuddy buddy;
	private ApiContact contact;
	private ApiProfile profile;
	
	public ApiContactListResult(ApiContact contact) {
		this(null, contact, null);
	}

	public ApiContactListResult(ApiBuddy buddy, ApiContact contact, ApiProfile profile) {
		this.buddy = buddy;
		this.contact = contact;
		this.profile = profile;
	}
	
	public ApiBuddy getBuddy() {
		return buddy;
	}

	public void setBuddy(ApiBuddy buddy) {
		this.buddy = buddy;
	}

	public ApiContact getContact() {
		return contact;
	}

	public ApiProfile getProfile() {
		return profile;
	}
	
	public void setProfile(ApiProfile profile) {
		this.profile = profile;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("buddy=").append(buddy);
		builder.append(", contact=").append(contact);
		builder.append(", profile=").append(profile);
		builder.append("]");
		return builder.toString();
	}

}
