package de.ascendro.f4m.service.game.selection.builder;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.util.JsonLoader;

public class GetGameListRequestBuilder {

	private String type;
	private String pool;
	private double handicapFrom = 0.0d;
	private double handicapTo = 0.0d;
	private Boolean isFree;
	private Boolean isOfflineGame;
	private boolean byInvitation = false;
	private boolean byFriendCreated = false;
	private boolean byFriendPlayed = false;
	private int numberOfQuestionsFrom = 0;
	private int numberOfQuestionsTo = 0;
	private Boolean hasSpecialPrize;
	private Boolean isSpecialGame;
	private String fullText = StringUtils.EMPTY;
	
	private final JsonLoader jsonLoader = new JsonLoader(this);
	private final JsonUtil jsonUtil = new JsonUtil();

	public GetGameListRequestBuilder() {
	}

	public static GetGameListRequestBuilder createGetGameListRequest() {
		return new GetGameListRequestBuilder();
	}

	public String buildRequestJson(ClientInfo clientInfo) throws Exception {
		String result = jsonLoader.getPlainTextJsonFromResources("getGameListRequest.json", clientInfo);
		result = replace(result, "type", type);
		result = replace(result, "pool", pool);
		result = replace(result, "handicapFrom", handicapFrom);
		result = replace(result, "handicapTo", handicapTo);
		result = replace(result, "isFree", isFree);
		result = replace(result, "isOfflineGame", isOfflineGame);
		result = replace(result, "byInvitation", byInvitation);
		result = replace(result, "byFriendCreated", byFriendCreated);
		result = replace(result, "byFriendPlayed", byFriendPlayed);
		result = replace(result, "numberOfQuestionsFrom", numberOfQuestionsFrom);
		result = replace(result, "numberOfQuestionsTo", numberOfQuestionsTo);
		result = replace(result, "hasSpecialPrize", hasSpecialPrize);
		result = replace(result, "isSpecialGame", isSpecialGame);		
		result = replace(result, "fullText", fullText);
		
		return result;
	}

	private String replace(String result, String property, Object value) {
		if (value != null) {
			return result.replaceFirst("\"<<" + property + ">>\"", jsonUtil.toJson(value));
		} else {
			return result.replaceFirst("\"" + property + "\": \"<<" + property + ">>\",", "");
		}
	}

	public GetGameListRequestBuilder withType(String type) {
		this.type = type;
		return this;
	}

	public GetGameListRequestBuilder withPool(String pool) {
		this.pool = pool;
		return this;
	}

	public GetGameListRequestBuilder withHandicap(double from, double to) {
		this.handicapFrom = from;
		this.handicapTo = to;
		return this;
	}

	public GetGameListRequestBuilder withIsFree(Boolean isFree) {
		this.isFree = isFree;
		return this;
	}

	public GetGameListRequestBuilder withIsOfflineGame(Boolean isOfflineGame) {
		this.isOfflineGame = isOfflineGame;
		return this;
	}

	public GetGameListRequestBuilder withByInvitation() {
		this.byInvitation = true;
		return this;
	}

	public GetGameListRequestBuilder withByFriendCreated() {
		this.byFriendCreated = true;
		return this;
	}

	public GetGameListRequestBuilder withByFriendPlayed() {
		this.byFriendPlayed = true;
		return this;
	}

	public GetGameListRequestBuilder withNumberOfQuestions(int from, int to) {
		this.numberOfQuestionsFrom = from;
		this.numberOfQuestionsTo = to;
		return this;
	}

	public GetGameListRequestBuilder withHasSpecialPrize(Boolean hasSpecialPrize) {
		this.hasSpecialPrize = hasSpecialPrize;
		return this;
	}

	public GetGameListRequestBuilder withIsSpecialGame(Boolean isSpecialGame) {
		this.isSpecialGame = isSpecialGame;
		return this;
	}

	public GetGameListRequestBuilder withFullText(String fullText) {
		this.fullText = fullText;
		return this;
	}
	
}
