package de.ascendro.f4m.service.game.selection.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.GameResponseSanitizer;
import de.ascendro.f4m.server.game.GameUtil;
import de.ascendro.f4m.server.request.jackpot.PaymentGetJackpotRequestInfo;
import de.ascendro.f4m.server.request.jackpot.PaymentServiceCommunicator;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorAerospikeDao;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.winning.model.MaxJackpot;
import de.ascendro.f4m.service.game.selection.model.game.*;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessService;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.winning.model.WinningComponent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.IntStream;

import static de.ascendro.f4m.service.game.selection.model.game.GameType.LIVE_TOURNAMENT;
import static de.ascendro.f4m.service.game.selection.model.game.GameType.TOURNAMENT;

public class GameSelector {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameSelector.class);

	private final GameSelectorAerospikeDao gameSelectorAerospikeDao;
	private final GameAerospikeDao gameAerospikeDao;
	private final CommonUserWinningAerospikeDao winningComponentAerospikeDao;
	private final JsonUtil jsonUtil;
	private final MultiplayerGameInstanceManager mgiManager;
	private final PaymentServiceCommunicator paymentServiceCommunicator;
	private GameResponseSanitizer gameResponseSanitizer;
	private UserGameAccessService userGameAccessService;

	@Inject
	public GameSelector(GameSelectorAerospikeDao gameSelectorAerospikeDao, JsonUtil jsonUtil,
						MultiplayerGameInstanceManager mgiManager, PaymentServiceCommunicator paymentServiceCommunicator,
						GameResponseSanitizer gameResponseSanitizer, UserGameAccessService userGameAccessService, CommonUserWinningAerospikeDao winningComponentAerospikeDao,
						GameAerospikeDao gameAerospikeDao) {

		this.gameSelectorAerospikeDao = gameSelectorAerospikeDao;
		this.winningComponentAerospikeDao = winningComponentAerospikeDao;
		this.jsonUtil = jsonUtil;
		this.mgiManager = mgiManager;
		this.paymentServiceCommunicator = paymentServiceCommunicator;
		this.gameResponseSanitizer = gameResponseSanitizer;
		this.userGameAccessService = userGameAccessService;
		this.gameAerospikeDao = gameAerospikeDao;
	}

	/**
	 * 	 Filters out special games that have exceeded play count and weed out expired games.
	 * @param userId
	 * @param gameFilter
	 * @return
	 */
	public GetGameListResponse getGameList(String userId, GameFilter gameFilter) {
		GetGameListResponse response = new GetGameListResponse();

		String tenantId = gameFilter.getTenantId();
		String appId = gameFilter.getAppId();
		
		JsonArray gameList = getGameListBySearchFields(tenantId, appId, gameFilter.getSearchFields());

		for (JsonElement gameElem : gameList){
			JsonObject game = gameElem.getAsJsonObject();
			if (game.get("isPromotion") == null) continue;
			boolean isPromotion = game.get("isPromotion").getAsBoolean();
			ZonedDateTime now = ZonedDateTime.now();
			if (game.get("promotionStartDate") == null) continue;
			ZonedDateTime promotionStartDate = ZonedDateTime.parse(game.get("promotionStartDate").getAsString());
			if (game.get("promotionEndDate") == null) continue;
			ZonedDateTime promotionEndDate = ZonedDateTime.parse(game.get("promotionEndDate").getAsString());
			if (isPromotion && now.isAfter(promotionStartDate) && now.isBefore(promotionEndDate)) {
				game.getAsJsonObject("typeConfiguration").getAsJsonObject("gameQuickQuiz").remove("isSpecialGame");
				game.getAsJsonObject("typeConfiguration").getAsJsonObject("gameQuickQuiz").addProperty("isSpecialGame",true);
			}
		}
		gameList = filterGameListBySpentSpecialGame(gameList, userId);

		if (gameFilter.byInvitation()) {
			Set<String> gameIdSet = mgiManager.getGamesUserHasBeenInvitedTo(tenantId, appId, userId);
			gameList = filterGameListByIds(gameList, gameIdSet);
		}

		if (gameFilter.byFriendCreated()) {
			Set<String> gameIdSet = getGameIdListCreatedByFriends(tenantId, appId, gameFilter.getFriends());
			gameList = filterGameListByIds(gameList, gameIdSet);
		}

		if (gameFilter.byFriendPlayed()) {
			Set<String> gameIdSet = new HashSet<>(Arrays.asList(gameFilter.getGames()));
			gameList = filterGameListByIds(gameList, gameIdSet);
		}

		if (gameFilter.hasAdditionalFilteringFields()) {
			gameList = filterGameListByAdditionalFields(gameFilter, gameList);
		}

		if (gameFilter.getFullText() != null) {
			gameList = filterGameListByFullText(gameFilter.getFullText(), gameList);
		}
		
		if (gameFilter.getCountryCode() != null) {
			gameList = filterGameListByPlayingRegions(gameList, gameFilter.getCountryCode());
		}
		
		if (gameFilter.getIsSpecialGame() != null) {
			gameList = filterGameListBySpecialGame(gameList, gameFilter.getIsSpecialGame());
			if (gameFilter.getIsSpecialGame()) {
				gameList = sortBySpecialGameWeight(gameList);
			}
		}
		response.setGames(removeExcessInfo(gameList));
		return response;
	}

	private JsonArray getGameListBySearchFields(String tenantId, String appId, Map<String, String> searchFields) {
		JsonArray gameList;
		if (searchFields.isEmpty()) {
			gameList = gameSelectorAerospikeDao.getGameList(tenantId, appId);
		} else {
			gameList = gameSelectorAerospikeDao.getGameList(tenantId, appId, searchFields);
		}
		return gameList;
	}

	private JsonArray filterGameListByIds(JsonArray gameList, Set<String> gameIdSet) {
		JsonArray resultArray = new JsonArray();
		for (JsonElement gameElement : gameList) {
			Game game = jsonUtil.fromJson(gameElement, Game.class);
			if (gameIdSet.contains(game.getGameId())) {
				resultArray.add(gameElement);
			}
		}
		return resultArray;
	}

	private JsonArray filterGameListByPlayingRegions(JsonArray gameList, String countryCode) {
		JsonArray resultArray = new JsonArray();
		for (JsonElement gameElement : gameList) {
			Game game = jsonUtil.fromJson(gameElement, Game.class);
			if (!game.getRegionalLimitationEnabled() || canPlayGameInThisRegion(game,countryCode)){
				resultArray.add(gameElement);
			}

		}
		return resultArray;
	}

	private JsonArray filterGameListBySpecialGame(JsonArray gameList, Boolean isSpecialGame) {
		JsonArray resultArray = new JsonArray();
		for (JsonElement gameElement : gameList) {
			Game game = jsonUtil.fromJson(gameElement, Game.class);
			if (isSpecialGame == game.isSpecial()){
				resultArray.add(gameElement);
			}
		}
		return resultArray;
	}


	/**
	 * Filters out special games that have exceeded play count and weed out expired games.
	 * @return JsonArray list games.
	 */
	private JsonArray filterGameListBySpentSpecialGame(JsonArray gameList, String userId) {
		JsonArray resultArray = new JsonArray();
		IntStream.range(0, gameList.size()).forEach(i -> {
			Game game = null;
			try {
				game = jsonUtil.fromJson(gameList.get(i), Game.class);
			} catch (Exception e) {
				LOGGER.error("filterGameListBySpentSpecialGame ERROR: {} for game: {}", e.getMessage(), gameList.get(i));
			}
			if (game != null ){
				String gameId = game.getGameId();
				if (!GameUtil.haveMultipleGamesPurchase(game) || userGameAccessService.isVisible(game, userId)) {
					if (gameId != null && DateTimeUtil.getCurrentDateTime().compareTo(gameAerospikeDao.getGame(gameId).getEndDateTime()) > 0) {
						// need to do in the Configurator timer cleaning game expired.
						LOGGER.error("*** A bug with the game expired ***");
					} else {
						resultArray.add(gameList.get(i));
					}
				}
			}
		});
		
		return resultArray;
	}
	
	protected JsonArray sortBySpecialGameWeight(JsonArray gameList) {
		//TODO: refactor all "filter..." methods to use one List<Game> as a parameter 
		//instead of parsing Game each time and creating new JsonArray every time. Or maybe even a Stream!
		List<Pair<Game, JsonElement>> games = new ArrayList<>(gameList.size());
		for (JsonElement gameElement : gameList) {
			games.add(new ImmutablePair<>(jsonUtil.fromJson(gameElement, Game.class), gameElement));
		}
		return games.stream()
				.sorted((g1, g2) -> Double.compare(getSortWeight(g2), getSortWeight(g1)))
				.map(Pair::getValue)
				.collect(Collector.of(JsonArray::new, JsonArray::add,
						(left, right) -> { left.addAll(right); return left; },
						Characteristics.IDENTITY_FINISH));
	}

	private Double getSortWeight(Pair<Game, JsonElement> g1) {
		return Optional.ofNullable(g1.getKey()).map(Game::getTypeConfiguration)
				.map(GameTypeConfiguration::getGameQuickQuiz).map(SingleplayerGameTypeConfigurationData::getWeight)
				.orElse(Double.MIN_VALUE);
	}
	
	private boolean canPlayGameInThisRegion(Game game, String countryCode) {
		return game.getPlayingRegions() == null || Arrays.asList(game.getPlayingRegions()).contains(countryCode);
	}	
	
	private JsonArray filterGameListByAdditionalFields(GameFilter gameFilter, JsonArray gameList) {
		JsonArray filteredGameList = new JsonArray();
		for (JsonElement gameJsonElement : gameList) {
			Game game = jsonUtil.fromJson(gameJsonElement, Game.class);
			if (isGameApplicable(gameFilter, game)) {
				filteredGameList.add(gameJsonElement);
			}
		}
		return filteredGameList;
	}

	private boolean isGameApplicable(GameFilter gameFilter, Game game) {
		return game.withinHandicap(gameFilter.getHandicapFrom(), gameFilter.getHandicapTo()) && game
				.withinNumberOfQuestions(gameFilter.getNumberOfQuestionsFrom(), gameFilter.getNumberOfQuestionsTo());
	}

	private JsonArray filterGameListByFullText(String fullText, JsonArray gameList) {
		JsonArray filteredGameList = new JsonArray();
		for (JsonElement gameJsonElement : gameList) {
			Game game = jsonUtil.fromJson(gameJsonElement, Game.class);
			String title = game.getTitle();
			String description = game.getGameDescription();
			if (StringUtils.containsIgnoreCase(title, fullText)
					|| StringUtils.containsIgnoreCase(description, fullText)) {
				filteredGameList.add(gameJsonElement);
			}
		}
		return filteredGameList;
	}

	private Set<String> getGameIdListCreatedByFriends(String tenantId, String appId, String[] friends) {
		Set<String> gameIdSet = new HashSet<>();

		for (String friendId : friends) {
			JsonArray gameList = gameSelectorAerospikeDao.getGameList(tenantId, appId, Game.SEARCH_FIELD_CREATOR,
					friendId);
			for (JsonElement gameElement : gameList) {
				Game game = jsonUtil.fromJson(gameElement, Game.class);
				gameIdSet.add(game.getGameId());
			}
		}

		return gameIdSet;
	}
	
	public List<Game> getGameListByType(String tenantId, String appId, GameType type) {
		Map<String, String> searchFields = new HashMap<>();
		searchFields.put(Game.SEARCH_FIELD_TYPE, type.name());
		JsonArray gameJsonArray = getGameListBySearchFields(tenantId, appId, searchFields);
		
		Type listType = new TypeToken<List<Game>>() {}.getType();
		return jsonUtil.getEntityListFromJsonString(gameJsonArray.toString(), listType);
	}

	public long getNumberOfGamesByType(String tenantId, String appId, GameType type) {
		Map<String, String> searchFields = new HashMap<>();
		searchFields.put(Game.SEARCH_FIELD_TYPE, type.name());
		JsonArray gameJsonArray = getGameListBySearchFields(tenantId, appId, searchFields);

		return gameJsonArray.size();
	}

	private JsonArray removeExcessInfo(JsonArray gameList) {
		for (int i = 0; i < gameList.size(); i++) {
			JsonElement gameJsonElement = gameList.get(i);
			Game game = jsonUtil.fromJson(gameJsonElement, Game.class);
			if (gameResponseSanitizer.needsRemovingExcessInfo(game)) {
				game = gameResponseSanitizer.removeExcessInfo(game);
				gameList.set(i, jsonUtil.toJsonElement(game));
			}
		}
		return gameList;
	}

	public Game getGame(String gameId) {
		Game game;
		try {
			game = gameSelectorAerospikeDao.getGame(gameId);
			
		} catch (F4MEntryNotFoundException notFoundEx) {
			LOGGER.warn("Tried to select game, wich does not exist by id {}", gameId, notFoundEx);
			game = new Game();
		}
		return game;
	}
	
	public GetGameResponse getGameWithJackpotInfo(GetGameRequest request, 
			JsonMessage<GetGameRequest> message, SessionWrapper sessionWrapper) {
		String gameId = request.getGameId();
		Game game = gameResponseSanitizer.removeExcessInfo(getGame(gameId));
		ISOCountry countryCode = null;
		String tenantId = null;
		ClientInfo clientInfo = message.getClientInfo();
		if (clientInfo != null) {
			countryCode = clientInfo.getCountryCode();
			tenantId = clientInfo.getTenantId();
		}

		if ((game.getRegionalLimitationEnabled()) && countryCode != null
				&& !canPlayGameInThisRegion(game, countryCode.toString())) {
			throw new F4MEntryNotFoundException("Game is not allowed for this region.");
		}
		
		if (game.isTypeOf(LIVE_TOURNAMENT, TOURNAMENT)) {
			addTournamentData(game);
		}
		MaxJackpot gameMaxJackpot = calculateGameMaxJackpot(game);
        game.setGameMaxJackpotType(gameMaxJackpot.getCurrency());
        game.setGameMaxJackpotAmount(gameMaxJackpot.getAmount());
		GetGameResponse currentResponse = new GetGameResponse(game);
		if (!StringUtils.isEmpty(game.getMultiplayerGameInstanceId()) && !GameUtil.isFreeGame(game, null)) {
			addJackpotData(game, tenantId, message, sessionWrapper, currentResponse);
			currentResponse = null; //do not return response while waiting for jackpot data
		} 
		return currentResponse;
	}

	private MaxJackpot calculateGameMaxJackpot(Game game){
		MaxJackpot gameMaxJackpot= new MaxJackpot();
		if (game.getResultConfiguration() != null && game.getResultConfiguration().getBonusPointsForAllCorrectAnswers()!=null)
			gameMaxJackpot.addJackpot("BONUS",BigDecimal.valueOf(game.getResultConfiguration().getBonusPointsForAllCorrectAnswers()),false);
		if (game.getResultConfiguration() != null && game.getResultConfiguration().getBonusPointsPerCorrectAnswerForUnpaid()!=null
				&& game.getNumberOfQuestions()!=null) {
			//TODO: maybe other NumberOfQuestions
			int numberOfQuestions = game.getNumberOfQuestions() == 0 ? 7 : game.getNumberOfQuestions();
			gameMaxJackpot.addJackpot("BONUS", BigDecimal.valueOf(game.getResultConfiguration().getBonusPointsPerCorrectAnswerForUnpaid() * numberOfQuestions), false);
		}
		if (game.getResultConfiguration()!=null && game.getResultConfiguration().getSpecialPrizeVoucherId()!=null)
			gameMaxJackpot.addJackpot("VOUCHER", new BigDecimal(game.getResultConfiguration().getSpecialPrizeVoucherId()),false);
		MaxJackpot winningComponentsMaxJackpot= new MaxJackpot();
		List<GameWinningComponentListItem> winningComponents = Arrays.asList(game.getWinningComponents());
		Collections.sort(winningComponents);
		for (GameWinningComponentListItem winningComponentListItem : game.getWinningComponents()){
			WinningComponent winningComponent = winningComponentAerospikeDao.getWinningComponent(winningComponentListItem.getWinningComponentId());
			winningComponentsMaxJackpot.addWinningComponentJackpot(winningComponent);
		}
		gameMaxJackpot.addJackpot(winningComponentsMaxJackpot.getCurrency(),winningComponentsMaxJackpot.getAmount(),false);
		return gameMaxJackpot;
	}

	private void addTournamentData(Game game) {
		String mgiId = mgiManager.getTournamentInstanceId(game.getGameId());
		if (mgiId != null) {
			CustomGameConfig multiplayerGameConfig = mgiManager.getMultiplayerGameConfig(mgiId);
			game.setMultiplayerGameInstanceId(mgiId);
			game.setLiveTournamentDateTime(multiplayerGameConfig.getPlayDateTime());
		}
	}
	
	private void addJackpotData(Game game, String tenantId,JsonMessage<GetGameRequest> message,
			SessionWrapper sessionWrapper, GetGameResponse response){
		PaymentGetJackpotRequestInfo requestInfo = new PaymentGetJackpotRequestInfo(game, message,
				sessionWrapper);
		requestInfo.setResponseToForward(response);
		paymentServiceCommunicator.sendGetJackpotRequest(game.getMultiplayerGameInstanceId(), tenantId, requestInfo);
	}

}
