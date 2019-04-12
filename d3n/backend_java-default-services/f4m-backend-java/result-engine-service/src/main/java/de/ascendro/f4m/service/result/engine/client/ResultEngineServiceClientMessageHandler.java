package de.ascendro.f4m.service.result.engine.client;

import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.request.jackpot.PaymentGetJackpotRequestInfo;
import de.ascendro.f4m.server.request.jackpot.SimpleGameInfo;
import de.ascendro.f4m.server.result.ResultItem;
import de.ascendro.f4m.server.result.ResultType;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.server.result.UserInteractionType;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.model.game.Jackpot;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotResponse;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.result.engine.model.CompletedGameHistoryInfo;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsRequest;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsResponse;
import de.ascendro.f4m.service.result.engine.model.get.CompletedGameListResponse;
import de.ascendro.f4m.service.result.engine.model.respond.RespondToUserInteractionRequest;
import de.ascendro.f4m.service.result.engine.model.store.StoreUserWinningComponentRequest;
import de.ascendro.f4m.service.result.engine.util.ResultEngineUtil;
import de.ascendro.f4m.service.result.engine.util.UserInteractionHandler;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultEngineServiceClientMessageHandler extends DefaultJsonMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResultEngineServiceClientMessageHandler.class);

	private final ResultEngineUtil resultEngineUtil;
	private final UserInteractionHandler userInteractionHandler;
	private final TransactionLogAerospikeDao transactionLogDao;
	private final EventServiceClient eventServiceClient;
	private final Tracker tracker;

	public ResultEngineServiceClientMessageHandler(ResultEngineUtil resultEngineUtil, UserInteractionHandler userInteractionHandler,
			TransactionLogAerospikeDao transactionLogDao, EventServiceClient eventServiceClient, Tracker tracker) {
		this.resultEngineUtil = resultEngineUtil;
		this.userInteractionHandler = userInteractionHandler;
		this.transactionLogDao = transactionLogDao;
		this.eventServiceClient = eventServiceClient;
		this.tracker = tracker;
	}

	@Override
	protected void onEventServiceRegistered() {
		eventServiceClient.subscribe(true, ResultEngineMessageTypes.SERVICE_NAME, ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC);
	}

	@Override
    public JsonMessageContent onUserMessage(RequestContext context) throws F4MException {
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
    	ProfileMessageTypes profileMessageType;
    	VoucherMessageTypes voucherMessageType;
    	PaymentMessageTypes paymentMessageType;
		EventMessageTypes eventMessageType;
    	if ((profileMessageType = message.getType(ProfileMessageTypes.class)) != null) {
    		return onProfileMessage(profileMessageType, message, context.getOriginalRequestInfo());
    	} else if ((voucherMessageType = message.getType(VoucherMessageTypes.class)) != null) {
    		return onVoucherMessage(voucherMessageType, context);
    	} else if ((paymentMessageType = message.getType(PaymentMessageTypes.class)) != null) {
    		if (context.getOriginalRequestInfo() instanceof PaymentGetJackpotRequestInfo){
        		return onPaymentGetJackpotMessage(paymentMessageType, context);
    		} else {
        		return onPaymentMessage(paymentMessageType, message, context.getOriginalRequestInfo());
    		}
		} else if ((eventMessageType = message.getType(EventMessageTypes.class)) != null) {
			return onEventMessage(message.getClientInfo(), message, eventMessageType);
		} else if (message.getType(UserMessageMessageTypes.class) == UserMessageMessageTypes.SEND_USER_PUSH_RESPONSE) {
			return null; // ignore userMessage/sendUserPushResponse
		} else {
			throw new F4MValidationFailedException("Unrecognized message " + message.getName());
    	}
    }
	
	@Override
	public void onUserErrorMessage(RequestContext ctx) {
		JsonMessage<? extends JsonMessageContent> message = ctx.getMessage();
    	PaymentMessageTypes paymentMessageType;
		if (PaymentMessageTypes.isTransferFundsResponseWithTransactionId(ctx.getMessage().getType(PaymentMessageTypes.class))) {
			updateTransactionLog(null, ctx.getOriginalRequestInfo(), TransactionStatus.COMPLETED);
		} else if ((paymentMessageType = message.getType(PaymentMessageTypes.class)) != null
				&& ctx.getOriginalRequestInfo() instanceof PaymentGetJackpotRequestInfo) {
			onPaymentGetJackpotMessage(paymentMessageType, ctx);
		}
	}

	private JsonMessageContent onProfileMessage(ProfileMessageTypes messageType, JsonMessage<? extends JsonMessageContent> message,
			ServiceRequestInfo requestInfo) {
		switch (messageType) {
		case UPDATE_PROFILE_RESPONSE:
			onUpdateProfileResponse(message, requestInfo);
			return null;
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + messageType + "]");
		}
	}

	private void onUpdateProfileResponse(JsonMessage<? extends JsonMessageContent> message, ServiceRequestInfo requestInfo) {
		if (message.getError() == null) {
			LOGGER.debug("onUpdateProfileResponse");
			respondToRequestBeforeUserInteraction(requestInfo, UserInteractionType.HANDICAP_ADJUSTMENT, null);
		}
	}

	private JsonMessageContent onVoucherMessage(VoucherMessageTypes messageType, RequestContext context) {
		switch (messageType) {
		case USER_VOUCHER_ASSIGN_RESPONSE:
			onAddVoucherToProfileResponse(context.getOriginalRequestInfo(), context.getClientInfo());
			return null;
		case USER_VOUCHER_RELEASE_RESPONSE:
			onReleaseVoucher(context.getOriginalRequestInfo());
			return null;
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + messageType + "]");
		}
	}

	private void onReleaseVoucher(ServiceRequestInfo requestInfo) {
		final SessionWrapper session = requestInfo.getSourceSession();
		final JsonMessage<? extends JsonMessageContent> originalMessage = requestInfo.getSourceMessage();
		final Results results = resultEngineUtil.getResults(requestInfo.getGameInstanceId());
		boolean waitForOtherServiceResponse = userInteractionHandler.invokeUserInteractions(results, originalMessage,
				session);
		if (! waitForOtherServiceResponse && originalMessage != null) {
			if (originalMessage.getContent() instanceof CalculateResultsRequest) {
				sendResponse(originalMessage, new CalculateResultsResponse(results), requestInfo.getSourceSession());
			} else {
				throw new F4MValidationFailedException("Unknown request: " + originalMessage.getContent());
			}
		}
	}

	private void onAddVoucherToProfileResponse(ServiceRequestInfo requestInfo, ClientInfo clientInfo) {
		final JsonMessage<? extends JsonMessageContent> originalMessage = requestInfo.getSourceMessage();
		LOGGER.debug("onAddVoucherToProfileResponse");
		if (originalMessage.getContent() instanceof UserVoucherAssignRequest) {
			UserVoucherAssignRequest request = (UserVoucherAssignRequest) originalMessage.getContent();
			//check if voucher won
			if (StringUtils.isNotBlank(request.getVoucherId())) {
				RewardEvent rewardEvent = new RewardEvent();
				rewardEvent.setVoucherWon(true);
				rewardEvent.setVoucherId(request.getVoucherId());
				rewardEvent.setGameInstanceId(requestInfo.getGameInstanceId());
				tracker.addEvent(originalMessage.getClientInfo(), rewardEvent);
			}
		}

		respondToRequestBeforeUserInteraction(requestInfo, UserInteractionType.SPECIAL_PRIZE, null);
	}

	private void respondToRequestBeforeUserInteraction(ServiceRequestInfo requestInfo, UserInteractionType userInteractionType, BigDecimal totalBonusPoints) {
		final SessionWrapper session = requestInfo.getSourceSession();
		final JsonMessage<? extends JsonMessageContent> originalMessage = requestInfo.getSourceMessage();
		LOGGER.debug("respondToRequestBeforeUserInteraction userInteractionType {} ", userInteractionType);// 3
		resultEngineUtil.removeUserInteraction(requestInfo.getGameInstanceId(), userInteractionType);
		final Results results = resultEngineUtil.getResults(requestInfo.getGameInstanceId());
		if (totalBonusPoints != null) {
			// Update total bonus points
			// as per task #10659, should store BONUS_POINTS + TOTAL_BONUS_POINTS
			if (results.getResultItems().get(ResultType.BONUS_POINTS) != null) {
				totalBonusPoints = totalBonusPoints.add(BigDecimal.valueOf(results.getResultItems().get(ResultType.BONUS_POINTS).getAmount()));
				// also add to results object :
				results.addResultItem(new ResultItem(ResultType.TOTAL_BONUS_POINTS, totalBonusPoints.doubleValue(), Currency.BONUS));
			}
			resultEngineUtil.storeTotalBonusPoints(results.getGameInstanceId(), totalBonusPoints);
		}

		boolean waitForOtherServiceResponse = userInteractionHandler.invokeUserInteractions(results, originalMessage, session);
		if (! waitForOtherServiceResponse && originalMessage != null) {
			if (originalMessage.getContent() instanceof CalculateResultsRequest) {
				sendResponse(originalMessage, new CalculateResultsResponse(results), requestInfo.getSourceSession());
			} else if (originalMessage.getContent() instanceof RespondToUserInteractionRequest
					|| originalMessage.getContent() instanceof StoreUserWinningComponentRequest) {
				sendResponse(originalMessage, new EmptyJsonMessageContent(), requestInfo.getSourceSession());
			} else {
				throw new F4MValidationFailedException("Unknown request: " + originalMessage.getContent());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private JsonMessageContent onPaymentMessage(PaymentMessageTypes messageType, 
			JsonMessage<? extends JsonMessageContent> message, ServiceRequestInfo requestInfo) {
		if (message.getError() == null) {
			if (PaymentMessageTypes.isTransferFundsResponseWithTransactionId(messageType)) {
				String transactionId = ((JsonMessage<TransactionId>) message).getContent().getTransactionId();
				onTransferBetweenAccountsResponse(transactionId, requestInfo);
			} else if (PaymentMessageTypes.CLOSE_JACKPOT_RESPONSE == messageType) {
				String transactionId = null; //no specific transactionId in case of close jackpot - many transactions are created in Paydent internally, the ids are not returned.
				onTransferBetweenAccountsResponse(transactionId, requestInfo);
			} else if (PaymentMessageTypes.GET_ACCOUNT_BALANCE_RESPONSE == messageType) {
				onGetAccountBalanceResponse((JsonMessage<GetAccountBalanceResponse>) message, requestInfo);
			} else {
				throw new F4MValidationFailedException("Unsupported message type[" + messageType + "]");
			}
		}
		return null;
	}

	private JsonMessageContent onPaymentGetJackpotMessage(PaymentMessageTypes messageType, 
			RequestContext context) {

		if (PaymentMessageTypes.GET_JACKPOT_RESPONSE == messageType) {
			onGetJackpotResponse(context);
		} else {
			throw new F4MValidationFailedException("Unsupported message type[" + messageType + "]");
		}
		
		
		return null;
	}
	
	
	private void onTransferBetweenAccountsResponse(String transactionId, ServiceRequestInfo requestInfo) {
		// Log transaction completed
		LOGGER.debug("onTransferBetweenAccountsResponse");
		updateTransactionLog(transactionId, requestInfo, TransactionStatus.COMPLETED);
		respondToRequestBeforeUserInteraction(requestInfo, UserInteractionType.BONUS_POINT_TRANSFER, null);
	}

	private void updateTransactionLog(String transactionId, ServiceRequestInfo requestInfo, TransactionStatus status) {
		final JsonMessage<? extends JsonMessageContent> originalMessage = requestInfo.getSourceMessage();

		for (String transactionLogId : requestInfo.getTransactionLogIds()) {
			TransactionLog transactionLog = transactionLogDao.getTransactionLog(transactionLogId);
			if (transactionLog!=null && transactionLog.getAmount().signum() == 1) {
				//Add reward event
				RewardEvent rewardEvent = new RewardEvent();
				rewardEvent.setPrizeWon(transactionLog.getAmount(), transactionLog.getCurrencyTo());
				rewardEvent.setGameInstanceId(requestInfo.getGameInstanceId());

				ClientInfo clientInfo = new ClientInfo();
				if (originalMessage.getClientInfo() != null) {
					clientInfo.setAppId(originalMessage.getClientInfo().getAppId());
					clientInfo.setIp(originalMessage.getClientInfo().getIp());
				}
				clientInfo.setTenantId(transactionLog.getTenantId());
				clientInfo.setUserId(transactionLog.getUserToId());

				tracker.addEvent(clientInfo, rewardEvent);
			}

			transactionLogDao.updateTransactionLog(transactionLogId, transactionId, status);
		}
	}

	private void onGetAccountBalanceResponse(JsonMessage<GetAccountBalanceResponse> message, ServiceRequestInfo requestInfo) {
		LOGGER.debug("onGetAccountBalanceResponse");
		respondToRequestBeforeUserInteraction(requestInfo, UserInteractionType.BONUS_POINT_ENQUIRY, message.getContent().getAmount());
	}

	private JsonMessageContent onEventMessage(ClientInfo clientInfo, JsonMessage<? extends JsonMessageContent> message,
			EventMessageTypes eventMessageType) {
		if (eventMessageType == EventMessageTypes.NOTIFY_SUBSCRIBER) {
			onNotifyMessage((NotifySubscriberMessageContent) message.getContent());
		} else if (eventMessageType == EventMessageTypes.SUBSCRIBE_RESPONSE) {
			LOGGER.debug("Received SUBSCRIBE_RESPONSE message: {}", message);
		} else {
			LOGGER.error("Unexpected message with type {}", eventMessageType);
		}
		return null;
	}

	private void onNotifyMessage(NotifySubscriberMessageContent notifySubscriberMessageContent) {
		if (ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC.equals(notifySubscriberMessageContent.getTopic())) {
			ProfileMergeEvent event = new ProfileMergeEvent(notifySubscriberMessageContent.getNotificationContent().getAsJsonObject());
			resultEngineUtil.moveResults(event.getSourceUserId(), event.getTargetUserId());
		} else {
			LOGGER.error("Unexpected message with topic {}", notifySubscriberMessageContent.getTopic());
		}
	}

	private void onGetJackpotResponse(RequestContext context) {
			PaymentGetJackpotRequestInfo originalRequestInfo = context.getOriginalRequestInfo();
			JsonMessage<GetJackpotResponse> message = context.getMessage();
			AtomicInteger numberOfExpectedResponsesRemaining = originalRequestInfo.getNumberOfExpectedResponses();
			
			if (context.getMessage().getError() == null) {
				SimpleGameInfo gameToChange=originalRequestInfo.getGameInfo();
				gameToChange.setJackpot(
						new Jackpot(message.getContent().getBalance(), message.getContent().getCurrency()));		
				CompletedGameListResponse resp = (CompletedGameListResponse) originalRequestInfo.getResponseToForward();
				List<CompletedGameHistoryInfo> moddedGames = modifyGameListWithJackpotHistory(resp.getItems() , gameToChange);
				resp.setItems(moddedGames);
				originalRequestInfo.setResponseToForward(resp);
			} else {
				LOGGER.error("Error getting jackpot request code: {}, type: {}", context.getMessage().getError().getCode(),
						context.getMessage().getError().getType());
			}
			if (numberOfExpectedResponsesRemaining != null) {
				int remaining = numberOfExpectedResponsesRemaining.decrementAndGet();
				if (remaining == 0) {
					LOGGER.debug("Forwarding {} to user, all expected payment responses received", originalRequestInfo.getResponseToForward());
					this.sendResponse(originalRequestInfo.getSourceMessage(), originalRequestInfo.getResponseToForward(),
							originalRequestInfo.getSourceSession());
				} else {
					LOGGER.debug("Not forwarding public game data to user, waiting for {} more responses from payment", remaining);
				}
			} else {
				LOGGER.error("Response to user was already sent for message {}", message);
			}
		}
	
	private List<CompletedGameHistoryInfo> modifyGameListWithJackpotHistory(List<CompletedGameHistoryInfo> gameList, SimpleGameInfo gameToChange) {
		List<CompletedGameHistoryInfo> resultArray = new ArrayList<>();
		for (CompletedGameHistoryInfo gameElement : gameList) {
			if (gameElement.getMultiplayerGameInstanceId().equals(gameToChange.getMultiplayerGameInstanceId())){
				gameElement.setJackpot(gameToChange.getJackpot());
				resultArray.add(gameElement);
			} else {
				resultArray.add(gameElement);
			}
		}
		return resultArray;
	}
	
}
