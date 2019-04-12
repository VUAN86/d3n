package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ContactListRequest extends FilterCriteria implements JsonMessageContent {

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;

	private String searchTerm;
	private Boolean hasApp;
	private Boolean hasProfile;
	private Boolean hasInvitation;
	private Boolean includeProfileInfo;
	private Boolean includeBuddyInfo;

	public String getSearchTerm() {
		return searchTerm;
	}
	
	public boolean isHasApp() {
		return hasApp == null ? true : hasApp;
	}

	public void setHasApp(boolean hasApp) {
		this.hasApp = hasApp;
	}

	public Boolean isHasProfile() {
		return hasProfile;
	}

	public void setHasProfile(Boolean hasProfile) {
		this.hasProfile = hasProfile;
	}

	public Boolean isHasInvitation() {
		return hasInvitation;
	}

	public void setHasInvitation(Boolean hasInvitation) {
		this.hasInvitation = hasInvitation;
	}

	public boolean isIncludeProfileInfo() {
		return includeProfileInfo == null ? false : includeProfileInfo;
	}

	public void setIncludeProfileInfo(boolean includeProfileInfo) {
		this.includeProfileInfo = includeProfileInfo;
	}

	public boolean isIncludeBuddyInfo() {
		return includeBuddyInfo == null ? false : includeBuddyInfo;
	}

	public void setIncludeBuddyInfo(boolean includeBuddyInfo) {
		this.includeBuddyInfo = includeBuddyInfo;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("searchTerm=").append(searchTerm);
		builder.append(", hasApp=").append(hasApp);
		builder.append(", hasProfile=").append(hasProfile);
		builder.append(", hasInvitation=").append(hasInvitation);
		builder.append(", includeProfileInfo=").append(includeProfileInfo);
		builder.append(", includeBuddyInfo=").append(includeBuddyInfo);
		builder.append(", limit=").append(getLimit());
		builder.append(", offset=").append(getOffset());
		builder.append("]");
		return builder.toString();
	}

}
