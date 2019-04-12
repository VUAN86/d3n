package de.ascendro.f4m.service.friend.model.api.player;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.friend.model.api.ApiBuddy;
import de.ascendro.f4m.service.friend.model.api.ApiContact;
import de.ascendro.f4m.service.friend.model.api.ApiProfile;
import de.ascendro.f4m.service.profile.model.Profile;

public class ApiPlayerListResult {

	private PlayerListResultType resultType;
	private String userId;
	private ApiBuddy buddy;
	private ApiContact contact;
	private ApiProfile profile;

	public ApiPlayerListResult(Buddy buddy) {
		resultType = PlayerListResultType.BUDDY;
		userId = buddy.getUserId();
		this.buddy = new ApiBuddy(buddy);
	}

	public ApiPlayerListResult(String appId, Contact contact) {
		resultType = PlayerListResultType.CONTACT;
		userId = contact.getUserId();
		this.contact = new ApiContact(appId, contact);
	}
	
	public ApiPlayerListResult(Profile profile) {
		resultType = PlayerListResultType.PLAYER;
		userId = profile.getUserId();
		this.profile = new ApiProfile(profile);
	}
	
	public PlayerListResultType getResultType() {
		return resultType;
	}

	public String getUserId() {
		return userId;
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
		builder.append("resultType=").append(resultType);
		builder.append(", userId=").append(userId);
		builder.append(", buddy=").append(buddy);
		builder.append(", contact=").append(contact);
		builder.append(", profile=").append(profile);
		builder.append("]");
		return builder.toString();
	}

	public Object getId() {
		switch (resultType) {
		case BUDDY:
			return buddy.getUserId();
		case CONTACT:
			return contact.getContactId();
		case PLAYER:
			return profile.getUserId();
		default:
			throw new F4MFatalErrorException("Unknown result type: " + resultType);
		}
	}

}
