package de.ascendro.f4m.server.multiplayer.dao;

import static de.ascendro.f4m.server.util.ElasticUtil.buildPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticResponse;
import de.ascendro.f4m.server.elastic.query.BoolQuery;
import de.ascendro.f4m.server.elastic.query.Query;
import de.ascendro.f4m.server.elastic.query.RangeQuery;
import de.ascendro.f4m.server.elastic.query.RootQuery;
import de.ascendro.f4m.server.elastic.query.Sort;
import de.ascendro.f4m.server.elastic.query.SortDirection;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.PublicGameFilter;
import de.ascendro.f4m.service.util.random.OutOfUniqueRandomNumbersException;
import de.ascendro.f4m.service.util.random.RandomSequenceGenerator;
import de.ascendro.f4m.service.util.random.RandomUtil;

public class PublicGameElasticDaoImpl implements PublicGameElasticDao {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PublicGameElasticDaoImpl.class);
	
	private static final String PROPERTY_APP_IDS = "appIds";
	private static final String PROPERTY_QUESTIONS = "numberOfQuestions";
	private static final String PROPERTY_FEE = "entryFeeAmount";
	private static final String PROPERTY_SEARCH_KEYWORDS = "searchKeywords";
	
	private static final String RAW_SUFFIX = "raw";
	
	private static final String PROPERTY_APP_ID_RAW = buildPath(PROPERTY_APP_IDS, RAW_SUFFIX);
	private static final String PROPERTY_MGI_ID_RAW = buildPath("multiplayerGameInstanceId", RAW_SUFFIX);
	private static final String PROPERTY_GAME_TYPE_RAW = buildPath("gameType", RAW_SUFFIX);
	private static final String PROPERTY_REGIONS_RAW = buildPath("playingRegions", RAW_SUFFIX);
	private static final String PROPERTY_POOLS_RAW = buildPath("poolIds", RAW_SUFFIX);
	private static final String PROPERTY_CURRENCY_RAW = buildPath("entryFeeCurrency", RAW_SUFFIX);
	private static final String PROPERTY_EXPIRY_DATE_RAW = buildPath("expiryDateTime", RAW_SUFFIX);
	private static final String PROPERTY_PLAY_DATE_RAW = buildPath("playDateTime", RAW_SUFFIX);
	private static final String PROPERTY_GAME_CREATOR_ID_RAW = buildPath("gameCreatorId", RAW_SUFFIX);
	private static final String PROPERTY_INVITER_ID_RAW = buildPath("inviterId", RAW_SUFFIX);

	private final ElasticClient elasticClient;
	private final GameConfigImpl config;
	private final JsonUtil jsonUtil;
	private final RandomUtil randomUtil;

	private boolean indexInitialized;

	@Inject
	public PublicGameElasticDaoImpl(ElasticClient elasticClient, GameConfigImpl config, JsonUtil jsonUtil,
			RandomUtil randomUtil) {
		this.elasticClient = elasticClient;
		this.config = config;
		this.jsonUtil = jsonUtil;
		this.randomUtil = randomUtil;
	}

	@Override
	public void createOrUpdate(List<String> appIds, Invitation publicGame) {
		initIndex();
		JsonObject json = jsonUtil.toJsonElement(publicGame).getAsJsonObject();
		json.add(PROPERTY_APP_IDS, jsonUtil.toJsonElement(appIds));
		json.addProperty(PROPERTY_SEARCH_KEYWORDS, getSearchKeywords(publicGame));
		elasticClient.createOrUpdate(getIndex(), getType(), publicGame.getMultiplayerGameInstanceId(), json);
	}

	private String getSearchKeywords(Invitation publicGame) {
		String[] keywords = { publicGame.getGame().getTitle(), publicGame.getGame().getDescription() };
		return StringUtils.trim(StringUtils.join(keywords, " "));
	}

	@Override
	public Invitation getPublicGame(String appId, String mgiId) {
		initIndex();
		BoolQuery query = Query.bool()
				.addMust(Query.term(PROPERTY_APP_ID_RAW, appId))
				.addMust(Query.term(PROPERTY_MGI_ID_RAW, mgiId));
		ElasticResponse elasticResponse = elasticClient.query(getIndex(), getType(), RootQuery.query(query.build()));
		
		Invitation publicGame;
		if (!elasticResponse.getResults().isEmpty()) {
			publicGame = jsonUtil.fromJson(elasticResponse.getResults().get(0), Invitation.class);
		} else {
			publicGame = null;
		}
		return publicGame;
	}
	
	@Override
	public long getNumberOfPublicGames(PublicGameFilter publicGameFilter) {
		initIndex();
		removeExpiredPublicGames();
		ElasticResponse response = getPublicGames(createPublicGamesQuery(publicGameFilter), SortDirection.asc, 0);
		return response.getTotal();
	}
	
    @Override
    public Invitation getNextPublicGame(PublicGameFilter publicGameFilter) {
    	initIndex();
    	removeExpiredPublicGames();
        ElasticResponse response = getPublicGames(createPublicGamesQuery(publicGameFilter), SortDirection.asc, 1);
        return response.getResults().isEmpty() ? new Invitation() : jsonUtil.fromJson(response.getResults().get(0), Invitation.class);
    }
	
	@Override
	public List<Invitation> searchPublicGames(PublicGameFilter filter, int limit) {
		initIndex();
		removeExpiredPublicGames();
		
		BoolQuery query = createPublicGamesQuery(filter);
		ElasticResponse responseDesc = getPublicGames(query, SortDirection.desc, limit);
		
		List<Invitation> randomizedPublicGameList;
		if (responseDesc.getResults().size() == limit) {
			ElasticResponse responseAsc = getPublicGames(query, SortDirection.asc, limit);
			randomizedPublicGameList = getRandomizedPublicGameList(responseDesc.getResults(), responseAsc.getResults());
		} else if (!responseDesc.getResults().isEmpty()) {
			randomizedPublicGameList = getRandomizedPublicGameList(responseDesc.getResults(), Collections.emptyList());
		} else {
			randomizedPublicGameList = Collections.emptyList();
		}
		return randomizedPublicGameList;
	}
	
	private ElasticResponse getPublicGames(BoolQuery query, SortDirection sortDirection, int limit) {
		RootQuery rootQuery = RootQuery.query(query.build())
				.sort(Sort.newSort(PROPERTY_EXPIRY_DATE_RAW, sortDirection))
				.size(limit);
		return elasticClient.query(getIndex(), getType(), rootQuery);
	}

	protected void removeExpiredPublicGames() {
		Query expiredGamesQuery = Query.range(PROPERTY_EXPIRY_DATE_RAW)
				.lte(RangeQuery.DATE_MATH_NOW)
				.build();
		removePublicGames(expiredGamesQuery);
	}
	
	@Override
	public void removeLiveTournamentsWithExpiredPlayDateTime() {
		initIndex();
		Query liveTournamentsWithExpiredPlayDateQuery = Query.bool()
				.addMust(Query.terms(PROPERTY_GAME_TYPE_RAW,
						new String[] { GameType.LIVE_TOURNAMENT.name(), GameType.USER_LIVE_TOURNAMENT.name() }))
				.addMust(Query.range(PROPERTY_PLAY_DATE_RAW).lte(RangeQuery.DATE_MATH_NOW).build())
				.build();
		removePublicGames(liveTournamentsWithExpiredPlayDateQuery);
	}
	
	private void removePublicGames(Query query) {
		int limit = config.getPropertyAsInteger(GameConfigImpl.EXPIRED_PUBLIC_GAME_LIST_SIZE);
		RootQuery rootQuery = RootQuery.query(query).size(limit);
		ElasticResponse response = elasticClient.query(getIndex(), getType(), rootQuery);

		List<JsonObject> publicGames = response.getResults();
		if (!publicGames.isEmpty()) {
			publicGames.forEach(json -> {
				Invitation publicGame = jsonUtil.fromJson(json, Invitation.class);
				delete(publicGame.getMultiplayerGameInstanceId(), true, true);
			});
			if (publicGames.size() >= limit) {
				removePublicGames(query);
			}
		}
	}

	private BoolQuery createPublicGamesQuery(PublicGameFilter filter) {
		BoolQuery query = Query.bool();
		if (filter.getAppId() != null) {
			query.addMust(Query.term(PROPERTY_APP_ID_RAW, filter.getAppId()));
		}
		if (filter.getGameType() != null) {
			query.addMust(Query.term(PROPERTY_GAME_TYPE_RAW, filter.getGameType().name()));
		}
		if (StringUtils.isNotBlank(filter.getSearchKeywords()) && filter.getSearchKeywords().matches("(\\W*\\w{3,}\\W*)+")) {
			// Search entered and has at least one word at least 3 chars long - add filter
			query.addMust(Query.match(PROPERTY_SEARCH_KEYWORDS).query(filter.getSearchKeywords()).minimumShouldMatch(2).build());
		}
		if (ArrayUtils.isNotEmpty(filter.getPlayingRegions())) {
			Arrays.stream(filter.getPlayingRegions()).forEach(region ->
				query.addMust(Query.term(PROPERTY_REGIONS_RAW, region))
			);
		}
		if (ArrayUtils.isNotEmpty(filter.getPoolIds())) {
			Arrays.stream(filter.getPoolIds()).forEach(pool ->
				query.addMust(Query.term(PROPERTY_POOLS_RAW, pool))
			);
		}
		if (filter.getNumberOfQuestions() != null) {
			query.addMust(Query.term(PROPERTY_QUESTIONS, filter.getNumberOfQuestions()));
		}
		if (filter.getEntryFeeCurrency() != null) {
			query.addMust(Query.term(PROPERTY_CURRENCY_RAW, filter.getEntryFeeCurrency().name()));
		}
		if (filter.getEntryFeeFrom() != null && filter.getEntryFeeTo() != null) {
			RangeQuery feeQuery = Query.range(PROPERTY_FEE);
			if (filter.isWithoutEntryFee()) {
				query.addMustNot(feeQuery.gt(0).build());
			} else {
				feeQuery.gte(filter.getEntryFeeFrom()).lte(filter.getEntryFeeTo());
				query.addMust(feeQuery.build());
			}
		}
		if (StringUtils.isNotEmpty(filter.getNotByUserId())) {
			query.addMust(Query.not(Query.term(PROPERTY_GAME_CREATOR_ID_RAW, filter.getNotByUserId())));
		}
		//delete after fix duplicate tournament
		query.addMustNot(Query.term(buildPath("gameId", RAW_SUFFIX), "237"));
		query.addMustNot(Query.term(buildPath("gameId", RAW_SUFFIX), "269"));
		return query;
	}
	
	private List<Invitation> getRandomizedPublicGameList(List<JsonObject> descList, List<JsonObject> ascList) {
		List<Invitation> publicGames = new ArrayList<>(descList.size());
		try {
			RandomSequenceGenerator random = new RandomSequenceGenerator(descList.size(), randomUtil);
			if (descList.size() == ascList.size()) {
				int middle = descList.size() / 2;
				for (int i = 0; i < descList.size(); i++) {
					int nextInt = random.nextInt();
					JsonObject publicGameJsonObject = nextInt < middle ? descList.get(nextInt) : ascList.get(nextInt - middle);
					//delete after fix
					Invitation invitation = jsonUtil.fromJson(publicGameJsonObject, Invitation.class);
					if (!invitation.getGame().getId().equals("269") && !invitation.getGame().getId().equals("237")) {
						publicGames.add(jsonUtil.fromJson(publicGameJsonObject, Invitation.class));
					}
				}
			} else {
				for (int i = 0; i < descList.size(); i++) {
					JsonObject publicGameJsonObject = descList.get(random.nextInt());
					//delete after fix
					Invitation invitation = jsonUtil.fromJson(publicGameJsonObject, Invitation.class);
					if (!invitation.getGame().getId().equals("269") && !invitation.getGame().getId().equals("237")) {
						publicGames.add(jsonUtil.fromJson(publicGameJsonObject, Invitation.class));
					}
				}
			}
		} catch (OutOfUniqueRandomNumbersException e) {
			LOGGER.error("RandomSequenceGenerator failed to choose unique random number", e);
			throw new F4MFatalErrorException("Failed to sort public games");
		}
		return publicGames;
	}

	@Override
	public void delete(String mgiId, boolean wait, boolean silent) {
		initIndex();
		elasticClient.delete(getIndex(), getType(), mgiId, wait, silent);
	}
	
	@Override
	public void changeUserOfPublicGame(String sourceUserId, String targetUserId) {
		initIndex();
		BoolQuery isInviter = Query.bool().addMust(Query.term(PROPERTY_INVITER_ID_RAW, sourceUserId));
		BoolQuery isCreator = Query.bool().addMust(Query.term(PROPERTY_GAME_CREATOR_ID_RAW, sourceUserId));
		BoolQuery query = Query.bool()
				.addShould(isInviter.build())
				.addShould(isCreator.build())
				.minimumShouldMatch(1);
		
		ElasticResponse response = elasticClient.query(getIndex(), getType(), RootQuery.query(query.build()));
		if (!response.getResults().isEmpty()) {
			response.getResults().forEach(json -> updateUserOfPublicGame(json, sourceUserId, targetUserId));
		}
	}

	private void updateUserOfPublicGame(JsonObject publicGameJsonString, String sourceUserId, String targetUserId) {
		Invitation publicGame = jsonUtil.fromJson(publicGameJsonString, Invitation.class);
		if (StringUtils.equals(publicGame.getInviter().getUserId(), sourceUserId)) {
			publicGame.getInviter().setUserId(targetUserId);
		}
		if (StringUtils.equals(publicGame.getCreator().getUserId(), sourceUserId)) {
			publicGame.getCreator().setUserId(targetUserId);
		}
		List<String> appIds = getPublicGameAppIds(publicGameJsonString);
		createOrUpdate(appIds, publicGame);
	}
	
	private List<String> getPublicGameAppIds(JsonObject publicGameJsonString) {
		List<String> appIds = new ArrayList<>();
		if (publicGameJsonString.get(PROPERTY_APP_IDS) != null) {
			JsonArray appIdsJsonArray = publicGameJsonString.get(PROPERTY_APP_IDS).getAsJsonArray();
			for (JsonElement appIdsJsonElement : appIdsJsonArray) {
				appIds.add(appIdsJsonElement.getAsString());
			}
		}
		return appIds;
	}

	private String getIndex() {
		return config.getProperty(GameConfigImpl.ELASTIC_INDEX_PUBLIC_GAME);
	}

	private String getType() {
		return config.getProperty(GameConfigImpl.ELASTIC_TYPE_PUBLIC_GAME);
	}

	private void initIndex() {
		if (!indexInitialized) {
			synchronized (this) {
				if (!indexInitialized) {
					String index = getIndex();
					if (!elasticClient.indexExists(index)) {
						elasticClient.createIndex(index, config.getProperty(GameConfigImpl.ELASTIC_MAPPING_INDEX_PUBLIC_GAME));
					}
					elasticClient.createMapping(index, getType(), config.getProperty(GameConfigImpl.ELASTIC_MAPPING_TYPE_PUBLIC_GAME));
					indexInitialized = true;
				}
			}
		}
	}

}
