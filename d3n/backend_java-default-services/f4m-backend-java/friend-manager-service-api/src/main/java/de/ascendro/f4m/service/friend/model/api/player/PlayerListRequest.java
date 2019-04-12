package de.ascendro.f4m.service.friend.model.api.player;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class PlayerListRequest extends FilterCriteria implements JsonMessageContent {

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;

	private String searchTerm;
	private Boolean includeUnconnectedPlayers;
	private Boolean includeBuddies;
	private Boolean favorite;
	private Boolean includeContacts;
	private Boolean excludeBuddiesFromContacts;
	private Boolean excludeBuddiesFromUnconnected;
	private Boolean excludeContactsFromBuddies;
	private Boolean excludeContactsFromUnconnected;
	private Boolean includeProfileInfo;
	private Boolean includeBuddyInfo;

	public String getSearchTerm() {
		return searchTerm;
	}

	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}

	public boolean isIncludeUnconnectedPlayers() {
		return includeUnconnectedPlayers == null ? true : includeUnconnectedPlayers;
	}

	public void setIncludeUnconnectedPlayers(boolean includeUnconnectedPlayers) {
		this.includeUnconnectedPlayers = includeUnconnectedPlayers;
	}

	public boolean isIncludeBuddies() {
		return includeBuddies == null ? true : includeBuddies;
	}

	public void setIncludeBuddies(boolean includeBuddies) {
		this.includeBuddies = includeBuddies;
	}

	public Boolean getFavorite() {
		return favorite;
	}
	
	public void setFavorite(Boolean favorite) {
		this.favorite = favorite;
	}
	
	public boolean isIncludeContacts() {
		return includeContacts == null ? true : includeContacts;
	}

	public void setIncludeContacts(boolean includeContacts) {
		this.includeContacts = includeContacts;
	}

	public boolean isExcludeBuddiesFromContacts() {
		return excludeBuddiesFromContacts == null ? true : excludeBuddiesFromContacts;
	}

	public void setExcludeBuddiesFromContacts(boolean excludeBuddiesFromContacts) {
		this.excludeBuddiesFromContacts = excludeBuddiesFromContacts;
	}

	public boolean isExcludeBuddiesFromUnconnected() {
		return excludeBuddiesFromUnconnected == null ? true : excludeBuddiesFromUnconnected;
	}

	public void setExcludeBuddiesFromUnconnected(boolean excludeBuddiesFromUnconnected) {
		this.excludeBuddiesFromUnconnected = excludeBuddiesFromUnconnected;
	}

	public boolean isExcludeContactsFromBuddies() {
		return excludeContactsFromBuddies == null ? false : excludeContactsFromBuddies;
	}

	public void setExcludeContactsFromBuddies(boolean excludeContactsFromBuddies) {
		this.excludeContactsFromBuddies = excludeContactsFromBuddies;
	}

	public boolean isExcludeContactsFromUnconnected() {
		return excludeContactsFromUnconnected == null ? true : excludeContactsFromUnconnected;
	}

	public void setExcludeContactsFromUnconnected(boolean excludeContactsFromUnconnected) {
		this.excludeContactsFromUnconnected = excludeContactsFromUnconnected;
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
		builder.append(", includeUnconnectedPlayers=").append(includeUnconnectedPlayers);
		builder.append(", includeBuddies=").append(includeBuddies);
		builder.append(", favorite=").append(favorite);
		builder.append(", includeContacts=").append(includeContacts);
		builder.append(", excludeBuddiesFromContacts=").append(excludeBuddiesFromContacts);
		builder.append(", excludeBuddiesFromUnconnected=").append(excludeBuddiesFromUnconnected);
		builder.append(", excludeContactsFromBuddies=").append(excludeContactsFromBuddies);
		builder.append(", excludeContactsFromUnconnected=").append(excludeContactsFromUnconnected);
		builder.append(", includeProfileInfo=").append(includeProfileInfo);
		builder.append(", includeBuddyInfo=").append(includeBuddyInfo);
		builder.append(", limit=").append(getLimit());
		builder.append(", offset=").append(getOffset());
		builder.append("]");
		return builder.toString();
	}

}
