package de.ascendro.f4m.service.friend.builder;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListOrderType;
import de.ascendro.f4m.service.friend.model.api.contact.ContactListRequest;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class BuddyListRequestBuilder {
	
	private String searchTerm = StringUtils.EMPTY;
	private BuddyListOrderType orderType = BuddyListOrderType.NONE;
	private BuddyRelationType[] includedRelationTypes = {};
	private BuddyRelationType[] excludedRelationTypes = {};
	private Boolean includeProfileInfo = false;
	private String userId = "";
	private int limit = ContactListRequest.MAX_LIST_LIMIT;
	private long offset = 0L;
	private Boolean favorite;
	
	private final JsonLoader jsonLoader = new JsonLoader(this);
	private final JsonUtil jsonUtil = new JsonUtil();
	
	public static BuddyListRequestBuilder createBuddyListRequest() {
		return new BuddyListRequestBuilder();
	}
	
	public String buildRequestJson() throws Exception {
		String buddyListRequestJson = jsonLoader.getPlainTextJsonFromResources("buddyListRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("\"<<searchTerm>>\"", jsonUtil.toJson(searchTerm))
				.replace("\"<<orderType>>\"", jsonUtil.toJson(orderType))
				.replace("\"<<includedRelationTypes>>\"", jsonUtil.toJson(includedRelationTypes))
				.replace("\"<<excludedRelationTypes>>\"", jsonUtil.toJson(excludedRelationTypes))
				.replace("\"<<includeProfileInfo>>\"", jsonUtil.toJson(includeProfileInfo))
				.replace("\"<<limit>>\"", Integer.toString(limit))
				.replace("<<userId>>", userId)
				.replace("\"<<offset>>\"", Long.toString(offset));
    	if (favorite != null) {
    		buddyListRequestJson = buddyListRequestJson.replace("\"<<favorite>>\"", favorite.toString());
    	} else {
    		buddyListRequestJson = buddyListRequestJson.replace(",\"favorite\": \"<<favorite>>\"", "");
    	}
    	return buddyListRequestJson;
	}

	public BuddyListRequestBuilder withUserId(String userId) {
		this.userId = userId;
		return this;
	}
	
	public BuddyListRequestBuilder withSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
		return this;
	}

	public BuddyListRequestBuilder withFavorite(Boolean favorite) {
		this.favorite = favorite;
		return this;
	}
	
	public BuddyListRequestBuilder withOrderType(BuddyListOrderType orderType) {
		this.orderType = orderType;
		return this;
	}

	public BuddyListRequestBuilder withIncludedRelationTypes(BuddyRelationType... includedRelationTypes) {
		this.includedRelationTypes = includedRelationTypes;
		return this;
	}

	public BuddyListRequestBuilder withExcludedRelationTypes(BuddyRelationType... excludedRelationTypes) {
		this.excludedRelationTypes = excludedRelationTypes;
		return this;
	}

	public BuddyListRequestBuilder withIncludeProfileInfo() {
		this.includeProfileInfo = true;
		return this;
	}

	public BuddyListRequestBuilder withLimit(int limit) {
		this.limit = limit;
		return this;
	}

	public BuddyListRequestBuilder withOffset(long offset) {
		this.offset = offset;
		return this;
	}

}
