package de.ascendro.f4m.service.friend.model.api.buddy;

import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class BuddyListRequest extends FilterCriteria implements JsonMessageContent {

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;

	private String userId;
	private String searchTerm;
	private BuddyListOrderType orderType;
	private BuddyRelationType[] includedRelationTypes;
	private BuddyRelationType[] excludedRelationTypes;
	private Boolean includeProfileInfo;
	private Boolean favorite;

	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getSearchTerm() {
		return searchTerm;
	}

	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}

	public BuddyListOrderType getOrderType() {
		return orderType == null ? BuddyListOrderType.NONE : orderType;
	}

	public void setOrderType(BuddyListOrderType orderType) {
		this.orderType = orderType;
	}

	public BuddyRelationType[] getIncludedRelationTypes() {
		return includedRelationTypes == null ? new BuddyRelationType[0] : includedRelationTypes;
	}

	public void setIncludedRelationTypes(BuddyRelationType[] includedRelationTypes) {
		this.includedRelationTypes = includedRelationTypes;
	}

	public BuddyRelationType[] getExcludedRelationTypes() {
		return excludedRelationTypes == null ? new BuddyRelationType[0] : excludedRelationTypes;
	}
	
	public void setExcludedRelationTypes(BuddyRelationType[] excludedRelationTypes) {
		this.excludedRelationTypes = excludedRelationTypes;
	}

	public boolean isIncludeProfileInfo() {
		return includeProfileInfo == null ? false : includeProfileInfo;
	}

	public void setIncludeProfileInfo(boolean includeProfileInfo) {
		this.includeProfileInfo = includeProfileInfo;
	}

	public Boolean getFavorite() {
		return favorite;
	}
	
	public void setFavorite(Boolean favorite) {
		this.favorite = favorite;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("searchTerm=").append(searchTerm);
		builder.append(", orderType=").append(orderType);
		builder.append(", includedRelationTypes=").append(includedRelationTypes);
		builder.append(", excludedRelationTypes=").append(excludedRelationTypes);
		builder.append(", includeProfileInfo=").append(includeProfileInfo);
		builder.append(", favorite=").append(favorite);
		builder.append(", limit=").append(getLimit());
		builder.append(", offset=").append(getOffset());
		builder.append("]");
		return builder.toString();
	}

}
