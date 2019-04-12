package de.ascendro.f4m.service.result.engine.server;

import com.google.gson.JsonObject;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.request.jackpot.JackpotDataGetter;
import de.ascendro.f4m.server.request.jackpot.SimpleGameInfo;
import de.ascendro.f4m.server.result.*;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryAlreadyExistsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.JokerType;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.result.engine.exception.F4MResultsNotReleasedException;
import de.ascendro.f4m.service.result.engine.exception.F4MWinningComponentPurchasePending;
import de.ascendro.f4m.service.result.engine.model.ApiProfileWithResults;
import de.ascendro.f4m.service.result.engine.model.CompletedGameHistoryInfo;
import de.ascendro.f4m.service.result.engine.model.GameHistoryUpdateKind;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateMultiplayerResultsRequest;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsRequest;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsResponse;
import de.ascendro.f4m.service.result.engine.model.get.*;
import de.ascendro.f4m.service.result.engine.model.move.MoveResultsRequest;
import de.ascendro.f4m.service.result.engine.model.multiplayerGameResult.*;
import de.ascendro.f4m.service.result.engine.model.respond.RespondToUserInteractionRequest;
import de.ascendro.f4m.service.result.engine.model.store.StoreUserWinningComponentRequest;
import de.ascendro.f4m.service.result.engine.util.ResultEngineUtil;
import de.ascendro.f4m.service.result.engine.util.UserInteractionHandler;
import de.ascendro.f4m.service.result.engine.util.UserResultsByHandicapRange;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.WinningOption;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;

public class ResultEngineServiceServerMessageHandler extends DefaultJsonMessageHandler {

	private final ResultEngineUtil resultEngineUtil;
	private final UserInteractionHandler userInteractionHandler;
	private final CommonProfileAerospikeDao commonProfileAerospikeDao;
	private final CommonBuddyElasticDao commonBuddyElasticDao;
	private final JackpotDataGetter jackpotDataGetter;
	private final CommonGameInstanceAerospikeDaoImpl gameInstanceAerospikeDao;
	private final CommonUserWinningAerospikeDao userWinningComponentAerospikeDao;
	private final TransactionLogAerospikeDao transactionLogAerospikeDao;

	private static final Logger LOGGER = LoggerFactory.getLogger(ResultEngineServiceServerMessageHandler.class);
    private static DecimalFormat df=new DecimalFormat("0.0");
	private static DateTimeFormatter beginOfDayFormat = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR).appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral("-")
			.appendValue(ChronoField.DAY_OF_MONTH)
			.parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
			.toFormatter()
			.withZone(ZoneOffset.UTC);
    private static DateTimeFormatter endOfDayFormat = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR).appendLiteral("-")
            .appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral("-")
            .appendValue(ChronoField.DAY_OF_MONTH)
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 23)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 59)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 59)
            .toFormatter()
            .withZone(ZoneOffset.UTC);

	public ResultEngineServiceServerMessageHandler(ResultEngineUtil resultEngineUtil,
			UserInteractionHandler userInteractionHandler, CommonProfileAerospikeDao commonProfileAerospikeDao,
			CommonBuddyElasticDao commonBuddyElasticDao, JackpotDataGetter jackpotDataGetter,
			CommonGameInstanceAerospikeDaoImpl gameInstanceAerospikeDao, CommonUserWinningAerospikeDao userWinningComponentAerospikeDao,
			TransactionLogAerospikeDao transactionLogAerospikeDao) {
		this.resultEngineUtil = resultEngineUtil;
		this.userInteractionHandler = userInteractionHandler;
		this.commonProfileAerospikeDao = commonProfileAerospikeDao;
		this.commonBuddyElasticDao = commonBuddyElasticDao;
		this.jackpotDataGetter = jackpotDataGetter;
		this.gameInstanceAerospikeDao = gameInstanceAerospikeDao;
		this.userWinningComponentAerospikeDao = userWinningComponentAerospikeDao;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
	}

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) throws F4MException {
		Validate.notNull(message, "Message is mandatory");
		final ResultEngineMessageTypes resultEngineMessageType;
		if ((resultEngineMessageType = message.getType(ResultEngineMessageTypes.class)) != null) {
			return onResultEngineMessage(message, resultEngineMessageType);
		} else {
			throw new F4MValidationFailedException("Unrecognized message type");
		}		
	}

	@SuppressWarnings("unchecked")
	private JsonMessageContent onResultEngineMessage(JsonMessage<? extends JsonMessageContent> message, ResultEngineMessageTypes resultEngineMessageType) {
		switch (resultEngineMessageType) {
		case CALCULATE_RESULTS:
			return onCalculateResults((JsonMessage<CalculateResultsRequest>) message);
		case GET_STATISTIC:
			return onGetStatistic((GetStatisticRequest) message.getContent());
		case GET_RESULTS:
			return onGetResults((GetResultsRequest) message.getContent(), true);
		case GET_RESULTS_INTERNAL:
			return onGetResults((GetResultsRequest) message.getContent(), false);
		case MOVE_RESULTS:
			return onMoveResults((MoveResultsRequest) message.getContent());
		case CALCULATE_MULTIPLAYER_RESULTS:
			return onCalculateMultiplayerResults((JsonMessage<CalculateMultiplayerResultsRequest>) message);
		case GET_MULTIPLAYER_RESULTS:
			return onGetMultiplayerResults((JsonMessage<GetMultiplayerResultsRequest>) message);
		case RESPOND_TO_USER_INTERACTION:
			return onRespondToUserInteraction((JsonMessage<RespondToUserInteractionRequest>) message);
		case STORE_USER_WINNING_COMPONENT:
			return onStoreUserWinningComponent((JsonMessage<StoreUserWinningComponentRequest>) message);
        case COMPLETED_GAME_LIST:
            return onCompletedGameList((JsonMessage<CompletedGameListRequest>) message);
		case COMPLETED_GAME_LIST_FOR_USER:
			return onCompletedGameListForUser((JsonMessage<CompletedGameListForUserRequest>) message);
		case MULTIPLAYER_RESULTS_GLOBAL_LIST:
			return onMultiplayerResultsGlobalList((JsonMessage<MultiplayerResultsGlobalListRequest>) message);
		case MULTIPLAYER_RESULTS_BUDDIES_LIST:
			return onMultiplayerResultsBuddiesList((JsonMessage<MultiplayerResultsBuddiesListRequest>) message);
		case RESYNC_MULTIPLAYER_RESULTS:
			return onResyncMultiplayerResults((JsonMessage<ResyncMultiplayerResultsRequest>) message);
        default:
			throw new F4MValidationFailedException("Unsupported message type[" + resultEngineMessageType + "]");
		}
	}

	private CalculateResultsResponse onCalculateResults(JsonMessage<CalculateResultsRequest> request)
			throws F4MEntryAlreadyExistsException {
		initClientInfoViaGameInstanceIfMissing(request);
		final CalculateResultsRequest message = request.getContent();
		final GameInstance gameInstance = new GameInstance(message.getGameInstance());

		validateGameForCalculation(gameInstance.getGameState(), gameInstance.getNumberOfQuestionsIncludingSkipped());
		// Verify if there are no previous results / recalculation requested
		final String gameInstanceId = gameInstance.getId();
		boolean resultsExist = resultEngineUtil.getResults(gameInstanceId) != null;
		if (resultsExist && (message.isForceRecalculate() == null || !message.isForceRecalculate())) {
			throw new F4MEntryAlreadyExistsException(
					"Results already calculated for the game instance [" + gameInstanceId + "]");
		}

		// Calculate and store the new results
		Results results = resultEngineUtil.calculateResults(gameInstance, request.getClientInfo());

		resultEngineUtil.storeResults(gameInstanceId, results,
				message.isForceRecalculate() != null && message.isForceRecalculate());

		// Update statistics
		resultEngineUtil.updateAverageAnswerTimeAndWriteWarningsOnTooBigDeviations(results, gameInstance);

		// Save completed game history item for user
		resultEngineUtil.saveGameHistoryForUser(gameInstance, results, GameHistoryUpdateKind.INITIAL);
		// Save end game event and for single-player games also game end event
		resultEngineUtil.savePlayerGameEndEvent(gameInstance, results, request.getClientInfo());
		if (! gameInstance.getGame().isMultiUserGame()) {
			resultEngineUtil.saveGameEndEvent(gameInstance.getGame(), request.getClientInfo());
		}
		resultEngineUtil.resyncSinglePlayerResults(results);

		// mark skipped questions as skipped
		Set<Integer> skippedQuestions = gameInstance.getJokersUsed(JokerType.SKIP);
		for (int ind : skippedQuestions) {
			results.getAnswerResults().get(ind).getQuestionInfo().setSkipped(true);
		}

		// Perform user / service interactions
		boolean waitForOtherServiceResponse = userInteractionHandler.releaseUserVoucher(gameInstance, results, request,
				getSessionWrapper());
		if (!waitForOtherServiceResponse) {
			waitForOtherServiceResponse = userInteractionHandler.invokeUserInteractions(results, request, getSessionWrapper());
		}
		return waitForOtherServiceResponse ? null : new CalculateResultsResponse(results);
	}
	
	private void initClientInfoViaGameInstanceIfMissing(JsonMessage<CalculateResultsRequest> request) {
		if(request.getClientInfo() == null){
			final GameInstance gameInstance = new GameInstance(request.getContent().getGameInstance());
	        final Profile profile = commonProfileAerospikeDao.getProfile(gameInstance.getUserId());
	        final ClientInfo clientInfo = new ClientInfo(gameInstance.getTenantId(), gameInstance.getAppId(),
	                gameInstance.getUserId(), gameInstance.getUserIp(), gameInstance.getUserHandicap());
	        clientInfo.setRoles(profile.getRoles(gameInstance.getUserId()));
	        request.setClientInfo(clientInfo);
		}	
	}

	private void validateGameForCalculation(GameState gameState, int numberOfQuestions) {
		if (gameState == null) {
			throw new F4MValidationFailedException("Missing game state");
		} else if (!gameState.isCompleted() && !gameState.isCancelled()) {
			final GameStatus status = gameState.getGameStatus();
			throw new F4MValidationFailedException(String.format("Game at particular state [%s] cannot be calculated",
					status != null ? status.name() : ""));
		} else if (!gameState.hasAllAnswers(numberOfQuestions)) {
			throw new F4MValidationFailedException(
					String.format("Game state is missing answers. Expected %d items.", numberOfQuestions));
		}
	}

	private GetStatisticResponse onGetStatistic(GetStatisticRequest request) throws F4MEntryNotFoundException {
		final String gameId = Integer.toString(request.getParentId());
		List<String> gameInstances = gameInstanceAerospikeDao.getGameInstancesList(gameId);
		Map<String,String> searchBy = request.getSearchBy();
		int requestedLimit = request.getLimit() == 0 ? 32767 : request.getLimit();
		if (searchBy.containsKey("gameInstanceId")){
			LinkedList<String> filteredInstances = new LinkedList();
			for (String curInst : gameInstances) {
				if (curInst.matches(searchBy.get("gameInstanceId").replace("%",".+"))) {
					filteredInstances.add(curInst);
				}
			}
			gameInstances= filteredInstances;
		}
		List <GameStatisticItem> gamePreStatistic = new ArrayList();
		for (String gameInstanceId : gameInstances){
			String userNickname = "", userId="", mgi = "", winningComponentId = "", winningComponentTitle = "", transactionReason = "";
			double userHandicap=0;
			int wrongAnswers=0,correctAnswers=0,skippedCount=0, adsViewed=0;
            BalanceObj balance = new BalanceObj();
			Results results = resultEngineUtil.getResults(gameInstanceId);
			List<String> advertisementShown = null;
			UserWinningComponent userWinComp = null;
			ZonedDateTime endDateTime = null;
            List<TransactionLog> transactions = transactionLogAerospikeDao.getTransactionLogsByGameInstance(gameInstanceId);
			if (results != null ) {
				advertisementShown = gameInstanceAerospikeDao.getAdvertisementShown(gameInstanceId);
				if (results.getUserWinningComponentId() != null)
				    userWinComp = userWinningComponentAerospikeDao.getWinningComponentResult(results.getUserWinningComponentId());
				if (results.getMultiplayerGameInstanceId() != null) mgi=results.getMultiplayerGameInstanceId();
				endDateTime = results.getEndDateTime();
				userId=results.getUserId();
				JsonObject userInfo =results.getUserInfo();
				if (userInfo!=null) {
					try {
						userNickname = userInfo.get("nickname").toString().replace("\"","");
					} catch (Exception e){
						userNickname = "unknown";
					}
				}
				double oldHandicap = pullDoubleItemAmount(results,ResultType.OLD_HANDICAP);
				double newHandicap = pullDoubleItemAmount(results,ResultType.NEW_HANDICAP);
                String handicapStr = df.format(newHandicap-oldHandicap);
                try {
                    userHandicap = df.parse(handicapStr).doubleValue() ;
                } catch (ParseException e) {
                    LOGGER.error("Parse Error {}",handicapStr);
                }
                correctAnswers= (int) pullDoubleItemAmount(results,ResultType.CORRECT_ANSWERS);
				wrongAnswers = (int) pullDoubleItemAmount(results,ResultType.TOTAL_QUESTIONS) - (int) pullDoubleItemAmount(results,ResultType.CORRECT_ANSWERS);
				Currency jackpotWinningCurrency = results.getJackpotWinningCurrency();
				BigDecimal jackpotWinning = results.getJackpotWinning();
				if (jackpotWinningCurrency != null && jackpotWinning != null) {
                    routeWon(balance, jackpotWinningCurrency.getFullName(), jackpotWinning.doubleValue());
                    transactionReason = "User jackpot won";
                }
                if (results.getAnswerResults()!=null) {
                    for (int ind : results.getAnswerResults().keySet()) {
                        if (results.getAnswerResults().get(ind).getQuestionInfo().isSkipped())
                            skippedCount++;
                    }
                }
			}
			if (userWinComp != null){
				winningComponentTitle = userWinComp.getTitle();
				winningComponentId = userWinComp.getWinningComponentId();
                WinningOption winning = userWinComp.getWinning();
                if (winning!=null) {
                    StatisticCurrency winningCurrency = StatisticCurrency.valueOf(winning.getType().name());
                    //TODO: add SuperPrize
                    if (winningCurrency != null &&  winning.getPrizeId() != null && StatisticCurrency.VOUCHER.getFullName().equals(winningCurrency.getFullName())) {
                        BigDecimal voucherId = new BigDecimal(userWinComp.getWinning().getPrizeId());
                        routeWon(balance, winningCurrency.getFullName(), voucherId.doubleValue());
                        transactionReason = transactionReason + (!"".equals(transactionReason) ? ", " : "") + "User winning component won";
                    }
                }
			}
			if (advertisementShown !=null){
				adsViewed = advertisementShown.size();
			}

            if (!transactions.isEmpty()){
                for (TransactionLog transaction : transactions){
					BalanceObj transactionBalance = new BalanceObj();
					StatisticCurrency transactionCurrency = StatisticCurrency.valueOf(transaction.getCurrency().name());
					BigDecimal amount = transaction.getAmount();
					if (transactionCurrency != null &&  amount != null) {
						routeWon(transactionBalance,transactionCurrency.getFullName(), amount.doubleValue());
					}
					GameStatisticItem statisticRow =new GameStatisticItem(gameInstanceId, mgi, transaction.getTransactionId(),transaction.getReason(),
                            endDateTime,userId,userNickname,userHandicap, correctAnswers,wrongAnswers,skippedCount,
                            adsViewed,winningComponentId,winningComponentTitle,transactionBalance.getVouchers(), transactionBalance.getBonus(),
							transactionBalance.getCredits(), transactionBalance.getMoney(),transaction.getStatus().toString(),request.getOrderBy().get(0));
                    if (filtering(statisticRow, searchBy))  continue;
                    gamePreStatistic.add(statisticRow);
                }
            }

            GameStatisticItem statisticRow =new GameStatisticItem(gameInstanceId, mgi, transactionReason,
                    endDateTime,userId,userNickname,userHandicap, correctAnswers,wrongAnswers,skippedCount,
                    adsViewed,winningComponentId,winningComponentTitle,balance.getVouchers(), balance.getBonus(),
                    balance.getCredits(), balance.getMoney(),"","",request.getOrderBy().get(0));
            if (filtering(statisticRow, searchBy))  continue;
			gamePreStatistic.add(statisticRow);
		}
        Collections.sort(gamePreStatistic);
        List <GameStatisticItem> gameStatistic = new ArrayList<GameStatisticItem>();
        int lastElement = (int) request.getOffset()+ requestedLimit;
        for (int recordInd = 0 + (int) request.getOffset(); ( recordInd < lastElement ) && recordInd < gamePreStatistic.size(); recordInd++) {
            gameStatistic.add(gamePreStatistic.get(recordInd));
        }
        return new GetStatisticResponse(gameStatistic,request.getLimit(),request.getOffset(),gamePreStatistic.size());
	}

	private boolean filtering(GameStatisticItem statisticRow, Map<String,String> searchBy){
	    return (searchBy.containsKey("mgi") && !statisticRow.getMultiplayerGameInstanceId().matches(searchBy.get("mgi").replace("%",".+")))||
                (searchBy.containsKey("transactionId") && !statisticRow.getTransactionId().matches(".+" + searchBy.get("transactionId").replace("%",".+"))) ||
                (searchBy.containsKey("userName") && !statisticRow.getUserName().matches(searchBy.get("userName").replace("%",".+"))) ||
                (searchBy.containsKey("status") && !statisticRow.getStatus().matches(".+" + searchBy.get("status").replace("%",".+"))) ||
                (searchBy.containsKey("startDate") && (!statisticRow.hasDate() || statisticRow.getDate().isBefore(ZonedDateTime.parse(searchBy.get("startDate"), beginOfDayFormat)))) ||
                (searchBy.containsKey("endDate") && (!statisticRow.hasDate() || statisticRow.getDate().isAfter(ZonedDateTime.parse(searchBy.get("endDate"), endOfDayFormat))));
    }

	public enum StatisticCurrency {
		VOUCHER("Vouchers"),
		MONEY("Money"),
		BONUS("Bonus points"),
		CREDIT("Credits"),
		CREDITS("Credits");

		private final String fullName;

		private StatisticCurrency(String fullName) {
			this.fullName = fullName;
		}

		public String getFullName() {
			return fullName;
		}

	}

	private void routeWon (BalanceObj balance, String currency, double amount){
		switch (currency){
			case "Bonus points":
                balance.appendBonus(amount);
				break;
			case "Credits":
                balance.appendCredits(amount);
				break;
			case "Money":
                balance.appendMoney(amount);
				break;
			case "Vouchers":
                balance.appendVouchers(amount);
				break;
		}

	}

	private double pullDoubleItemAmount(Results results, ResultType resultType){
		ResultItem item = results.getResultItems().get(resultType);
		double itemDouble = 0;
		if (item !=null) {
			itemDouble = item.getAmount();
		}
		return itemDouble;
	}

	private GetResultsResponse onGetResults(GetResultsRequest message, boolean checkReleased) throws F4MEntryNotFoundException {
		final String gameInstanceId = message.getGameInstanceId();
		GameInstance gameInstance = gameInstanceAerospikeDao.getGameInstance(gameInstanceId);
		if (gameInstance.getGame().getType() == GameType.TIPP_TOURNAMENT && !checkReleased) {
			return new GetResultsResponse(new Results());
		}
		Results results = getResults(gameInstanceId);
		assert results != null;
		Set<UserInteractionType> userInteractions = results.getUserInteractions();
		if (checkReleased && !userInteractions.isEmpty()) {
			String msg = "User interactions in progress: " + results.getUserInteractions();
			if (userInteractions.contains(UserInteractionType.BUY_WINNING_COMPONENT)) {
				throw new F4MWinningComponentPurchasePending(msg);
			} else {
				throw new F4MResultsNotReleasedException(msg);
			}
		}
		return new GetResultsResponse(results);
	}

	private Results getResults(String gameInstanceId) {
		Results results = resultEngineUtil.getResults(gameInstanceId);
		if (results == null) {
			throw new F4MEntryNotFoundException("No results found for game instance [" + gameInstanceId + "]");
		}
		return results;
	}
	
	private JsonMessageContent onMoveResults(MoveResultsRequest message) {
		resultEngineUtil.moveResults(message.getSourceUserId(), message.getTargetUserId());
		return new EmptyJsonMessageContent();
	}

	private EmptyJsonMessageContent onCalculateMultiplayerResults(JsonMessage<CalculateMultiplayerResultsRequest> message) {
		CalculateMultiplayerResultsRequest content = message.getContent();
		// Calculate the new results
		UserResultsByHandicapRange results = resultEngineUtil.calculateMultiplayerGameOutcome(content.getMultiplayerGameInstanceId());
		// Verify if there are no previous results / recalculation requested
		boolean resultsExist = resultEngineUtil.getMultiplayerResults(content.getMultiplayerGameInstanceId()) != null;
		if (!resultsExist || (content.isForceRecalculate() != null && content.isForceRecalculate())) {
			// Calculate the new results
			MultiplayerResults multiplayerResults = new MultiplayerResults(content.getMultiplayerGameInstanceId(), results.getGame().getType());
			// Store the new results
			resultEngineUtil.storeMultiplayerResults(multiplayerResults, content.isForceRecalculate() != null && content.isForceRecalculate());
			// Update individual game results to include multiplayer game results
			resultEngineUtil.updateIndividualResults(results);
			// Index tournament results
			resultEngineUtil.resyncMultiplayerResults(results);

			// Notify involved users about game end
			resultEngineUtil.sendGameEndNotificationsToInvolvedUsers(results);
			// Store users results
			resultEngineUtil.storeUserPayout(results);
		}
		// Initiate winning payout
		resultEngineUtil.initiatePayout(results, results.getGame().getType().isTournament());
		return new EmptyJsonMessageContent();
	}

	private GetMultiplayerResultsResponse onGetMultiplayerResults(JsonMessage<GetMultiplayerResultsRequest> message) throws F4MEntryNotFoundException {
		Results results = getResults(message, message.getContent().getGameInstanceId());
		MultiplayerResults multiplayerResults = getMultiplayerResults(results);

		GameType gameType = multiplayerResults.getGameType();
		if (gameType.isDuel()) {
			Results opponentResults = resultEngineUtil.getResults(results.getDuelOpponentGameInstanceId());
			return new GetMultiplayerResultsResponse(results, opponentResults);
		} else if (gameType.isTournament()) {
			int globalPlace = resultEngineUtil.getMultiplayerGameRankByResults(multiplayerResults.getMultiplayerGameInstanceId(), 
					results.getUserId(), results.getCorrectAnswerCount(), results.getGamePointsWithBonus(), null, null);
			List<String> buddyIds = getBuddyIds(message);
			int placeAmongBuddies = CollectionUtils.isEmpty(buddyIds) ? 0
					: resultEngineUtil.getMultiplayerGameRankByResults(multiplayerResults.getMultiplayerGameInstanceId(), 
							results.getUserId(), results.getCorrectAnswerCount(), results.getGamePointsWithBonus(), results.getHandicapRangeId(), buddyIds);
			int globalPlaceAmongBuddies = CollectionUtils.isEmpty(buddyIds) ? 0 
					: resultEngineUtil.getMultiplayerGameRankByResults(multiplayerResults.getMultiplayerGameInstanceId(), 
							results.getUserId(), results.getCorrectAnswerCount(), results.getGamePointsWithBonus(), null, buddyIds);
			return new GetMultiplayerResultsResponse(results, globalPlace, placeAmongBuddies, globalPlaceAmongBuddies);
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
	}
	
	private JsonMessageContent onRespondToUserInteraction(JsonMessage<RespondToUserInteractionRequest> request) {
		LOGGER.debug("onRespondToUserInteraction"); // 0
		RespondToUserInteractionRequest message = request.getContent();
		UserInteractionType type = message.getUserInteractionType();
		Validate.notNull(type, "User interaction type needs to be specified");
		String gameInstanceId = message.getGameInstanceId();
		Results results = getResults(gameInstanceId);
		Set<UserInteractionType> userInteractions = results.getUserInteractions();
		if (! userInteractions.contains(type)) {
			throw new F4MEntryNotFoundException("No user interaction of type [" + type + "] found for game instance [" + gameInstanceId + "]");
		}
		boolean waitForOtherServiceResponse = userInteractionHandler.respondToUserInteraction(type, results, message.getResponse(), request, getSessionWrapper());
		if (! waitForOtherServiceResponse) {
			resultEngineUtil.removeUserInteraction(gameInstanceId, type);
			return new EmptyJsonMessageContent();
		}
		return null;
	}

	private JsonMessageContent onStoreUserWinningComponent(JsonMessage<StoreUserWinningComponentRequest> request) {
		LOGGER.debug("onStoreUserWinningComponent");
		StoreUserWinningComponentRequest message = request.getContent();
		Results results = resultEngineUtil.getResults(message.getGameInstanceId());
		if (results == null) {
			throw new F4MEntryNotFoundException("No results found for game instance [" + message.getGameInstanceId() + "]");
		}
		resultEngineUtil.storeUserWinningComponent(request.getClientInfo(), results, message.getUserWinningComponentId(), message.isPaid());
		boolean waitForOtherServiceResponse = userInteractionHandler.invokeUserInteractions(results, request, getSessionWrapper());
		return waitForOtherServiceResponse ? null : new EmptyJsonMessageContent();
	}

	private CompletedGameListResponse onCompletedGameList(JsonMessage<CompletedGameListRequest> message) {
		return onCompletedGameList(message, message.getUserId());
	}
	
	private CompletedGameListResponse onCompletedGameListForUser(JsonMessage<CompletedGameListForUserRequest> message) {
		return onCompletedGameList(message, message.getContent().getUserId());
	}
	
	private CompletedGameListResponse onCompletedGameList(JsonMessage<? extends CompletedGameListRequest> message, String userId) {
		if (! message.isAuthenticated()) {
			throw new F4MInsufficientRightsException("No user id specified");
		}
		final String tenantId = message.getTenantId();
		if (StringUtils.isBlank(tenantId)) {
			throw new F4MInsufficientRightsException("No tenant id specified");
		}
		
		Validate.notNull(message.getContent(), "Message content of gameListByStatus is mandatory");
		Validate.isInstanceOf(CompletedGameListRequest.class, message.getContent());

		CompletedGameListResponse result;
		CompletedGameListRequest completedGameListRequest = message.getContent();

		completedGameListRequest.validateFilterCriteria(CompletedGameListRequest.MAX_LIST_LIMIT);

        ZonedDateTime dateFrom = DateTimeUtil.parseISODateTimeString(completedGameListRequest.getDateFrom());
        ZonedDateTime dateTo = DateTimeUtil.parseISODateTimeString(completedGameListRequest.getDateTo());

        if (dateTo.isBefore(dateFrom)) {
            throw new F4MValidationFailedException("dateFrom must be before dateTo");
        }

		if (completedGameListRequest.getLimit() == 0) {
			result = new CompletedGameListResponse(completedGameListRequest.getLimit(), completedGameListRequest.getOffset());
		} else {
            result = resultEngineUtil.getCompletedGameListResponse(userId, tenantId,
					completedGameListRequest.getGameTypes(),
					completedGameListRequest.getOffset(),
                    completedGameListRequest.getLimit(),
					dateFrom,
					dateTo,
					completedGameListRequest.getSearchBy("isMultiplayerGameFinished"),
					completedGameListRequest.getSearchBy("gameOutcome"));
        }

		boolean syncResponse = jackpotDataGetter.synchronizeUpdatedDataHistory(getGamesHistoryList(result.getItems()),
				message, this.getSessionWrapper(), result);
		if (!syncResponse){
			result = null; 
		}
		return result;
	}
	
	private List<SimpleGameInfo> getGamesHistoryList(List<CompletedGameHistoryInfo> publicGames){
		List<SimpleGameInfo>  resultArray = new ArrayList<>();
		for (CompletedGameHistoryInfo publicGame: publicGames){
			resultArray.add(new SimpleGameInfo(publicGame.getGame().getId(), publicGame.getMultiplayerGameInstanceId(),
					publicGame.isGameIsFree()));
		}
		return resultArray;
	}
	

	private JsonMessageContent onMultiplayerResultsGlobalList(JsonMessage<MultiplayerResultsGlobalListRequest> message) {
		MultiplayerResultsGlobalListRequest request = message.getContent();
		Results results = getResults(message, request.getGameInstanceId());
		String mgiId = results.getMultiplayerGameInstanceId(); 
		MultiplayerResultsStatus status = resultEngineUtil.hasMultiplayerResults(mgiId)
				? MultiplayerResultsStatus.FINAL
				: MultiplayerResultsStatus.PRELIMINARY;
		Integer handicapRangeId = null;
		if (! request.isAmongAllHandicapRanges()) {
			handicapRangeId = results.getHandicapRangeId();
		}
		
		ListResult<ApiProfileWithResults> result = resultEngineUtil.listMultiplayerResults(mgiId, message.getUserId(),
				handicapRangeId, null, request.getLimit(), request.getOffset(), status);
		return new MultiplayerResultsGlobalListResponse(result.getLimit(), result.getOffset(), result.getTotal(),
				result.getItems(), status);
	}

	private JsonMessageContent onMultiplayerResultsBuddiesList(JsonMessage<MultiplayerResultsBuddiesListRequest> message) {
		MultiplayerResultsBuddiesListRequest request = message.getContent();
		Results results = getResults(message, request.getGameInstanceId());
		String mgiId = results.getMultiplayerGameInstanceId();
		MultiplayerResultsStatus status = resultEngineUtil.hasMultiplayerResults(mgiId)
				? MultiplayerResultsStatus.FINAL
				: MultiplayerResultsStatus.PRELIMINARY;
		Integer handicapRangeId = null;
		if (! request.isAmongAllHandicapRanges()) {
			handicapRangeId = results.getHandicapRangeId();
		}
		
		List<String> buddyIds = getBuddyIds(message);
		
		ListResult<ApiProfileWithResults> result = resultEngineUtil.listMultiplayerResults(mgiId, message.getUserId(),
				handicapRangeId, buddyIds, request.getLimit(), request.getOffset(), status);
		return new MultiplayerResultsBuddiesListResponse(result.getLimit(), result.getOffset(), result.getTotal(), result.getItems(), status);
	}

	private JsonMessageContent onResyncMultiplayerResults(JsonMessage<ResyncMultiplayerResultsRequest> message) {
		String mgiId = message.getContent().getMultiplayerGameInstanceId();
		resultEngineUtil.resyncMultiplayerResults(mgiId);
		return new EmptyJsonMessageContent();
	}

	private MultiplayerResults getMultiplayerResults(Results results) {
		MultiplayerResults multiplayerResults = resultEngineUtil.getMultiplayerResults(results.getMultiplayerGameInstanceId());
		if (multiplayerResults == null) {
			if (resultEngineUtil.isMultiplayerInstanceAvailable(results.getMultiplayerGameInstanceId())) {
				throw new F4MResultsNotReleasedException("Results not yet calculated for the multiplayerGameInstanceId [" +
						results.getMultiplayerGameInstanceId() + "]");
			} else {
				throw new F4MEntryNotFoundException("No game results found for the multiplayerGameInstanceId [" +
						results.getMultiplayerGameInstanceId() + "]");
			}
		}
		return multiplayerResults;
	}

	private Results getResults(JsonMessage<?> message, String gameInstanceId) {
		if (! message.isAuthenticated()) {
			throw new F4MInsufficientRightsException("User has to be authenticated to get multiplayer game results");
		}
		
		Results results = resultEngineUtil.getResults(gameInstanceId);
		if (results == null) {
			throw new F4MEntryNotFoundException("No results found for game instance [" + gameInstanceId + "]");
		}
		return results;
	}

	private List<String> getBuddyIds(JsonMessage<?> message) {
		return commonBuddyElasticDao.getAllBuddyIds(message.getUserId(), message.getClientInfo().getAppId(), message.getTenantId(),
				new BuddyRelationType[] { BuddyRelationType.BUDDY }, new BuddyRelationType[] { BuddyRelationType.BLOCKED, BuddyRelationType.BLOCKED_BY }, null);
	}

    private class BalanceObj {
        private double bonus;
        private double credits;
        private double money;
        private double vouchers;

        public BalanceObj() {
            this.bonus = 0;
            this.credits = 0;
            this.money = 0;
            this.vouchers = 0;
        }

        public double getBonus() {
            return bonus;
        }

        public double getCredits() {
            return credits;
        }

        public double getMoney() {
            return money;
        }

        public double getVouchers() {
            return vouchers;
        }

        public void appendBonus(double inc){
            this.bonus+=inc;
        }

        public void appendCredits(double inc){
            this.credits+=inc;
        }

        public void appendMoney(double inc){
            this.money+=inc;
        }

        public void appendVouchers(double inc){
            this.vouchers+=inc;
        }

        @Override
        public String toString() {
            return "BalanceObj{" + "bonus=" + bonus + ", credits=" + credits + ", money=" + money + ", vouchers=" + vouchers + '}';
        }
    }
}
