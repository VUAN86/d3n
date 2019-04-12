package de.ascendro.f4m.service.winning.client;

import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.exception.PaymentServiceExceptionCodes;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.result.engine.model.get.GetResultsResponse;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignResponse;
import de.ascendro.f4m.service.winning.WinningMessageTypes;
import de.ascendro.f4m.service.winning.dao.SuperPrizeAerospikeDao;
import de.ascendro.f4m.service.winning.manager.WinningComponentManager;
import de.ascendro.f4m.service.winning.manager.WinningManager;
import de.ascendro.f4m.service.winning.model.*;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentAssignRequest;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentAssignResponse;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentStartResponse;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentStopRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class WinningServiceClientMessageHandler extends DefaultJsonMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(WinningServiceClientMessageHandler.class);

	private final WinningComponentManager winningComponentManager;
	private final CommonUserWinningAerospikeDao userWinningDao;
	private final PaymentServiceCommunicator paymentServiceCommunicator;
	private final ResultEngineCommunicator resultEngineCommunicator;
	private final TransactionLogAerospikeDao transactionLogAerospikeDao;
	private final EventServiceClient eventServiceClient;
	private final Tracker tracker;
	private final WinningManager winningManager;
	private final CommonProfileAerospikeDao profileDao;
	private final CommonGameInstanceAerospikeDao gameInstanceDao;
	private final UserMessageServiceCommunicator userMessageServiceCommunicator;
	private final SuperPrizeAerospikeDao superPrizeAerospikeDao;

    public WinningServiceClientMessageHandler(WinningComponentManager winningComponentManager, PaymentServiceCommunicator paymentServiceCommunicator, ResultEngineCommunicator resultEngineCommunicator, TransactionLogAerospikeDao transactionLogAerospikeDao, CommonUserWinningAerospikeDao userWinningDao, EventServiceClient eventServiceClient, Tracker tracker, WinningManager winningManager, CommonProfileAerospikeDao profileDao, CommonGameInstanceAerospikeDao gameInstanceDao, UserMessageServiceCommunicator userMessageServiceCommunicator, SuperPrizeAerospikeDao superPrizeAerospikeDao) {
        this.winningComponentManager = winningComponentManager;
        this.paymentServiceCommunicator = paymentServiceCommunicator;
        this.resultEngineCommunicator = resultEngineCommunicator;
        this.transactionLogAerospikeDao = transactionLogAerospikeDao;
        this.userWinningDao = userWinningDao;
        this.tracker = tracker;
        this.eventServiceClient = eventServiceClient;
        this.winningManager = winningManager;
        this.profileDao = profileDao;
        this.gameInstanceDao = gameInstanceDao;
        this.userMessageServiceCommunicator = userMessageServiceCommunicator;
        this.superPrizeAerospikeDao = superPrizeAerospikeDao;
    }

	@Override
	protected void onEventServiceRegistered() {
		eventServiceClient.subscribe(true, WinningMessageTypes.SERVICE_NAME, ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC);
	}

	@Override
	public JsonMessageContent onUserMessage(RequestContext context) {
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		final ResultEngineMessageTypes resultEngineMessageType;
		final PaymentMessageTypes paymentMessageType;
		final VoucherMessageTypes voucherMessageType;

		if ((resultEngineMessageType = message.getType(ResultEngineMessageTypes.class)) != null) {
			onResultEngineMessage(resultEngineMessageType, context);
		} else if ((paymentMessageType = message.getType(PaymentMessageTypes.class)) != null) {
			onPaymentMessage(paymentMessageType, context);
		} else if ((voucherMessageType = message.getType(VoucherMessageTypes.class)) != null) {
			onVoucherMessage(voucherMessageType, context);
		} else if ((message.getType(EventMessageTypes.class)) != null) {
			onEventServiceMessage(message, message.getType(EventMessageTypes.class));
		}
		return null;
	}

	@Override
	public void onUserErrorMessage(RequestContext ctx) {
		if (PaymentMessageTypes.isTransferFundsResponseWithTransactionId(ctx.getMessage().getType(PaymentMessageTypes.class))) {
			PaymentRequestInfo originalRequest = ctx.getOriginalRequestInfo();
			transactionLogAerospikeDao.updateTransactionLog(originalRequest.getTransactionLogId(),
					 null, TransactionStatus.ERROR);
		}

    }

    @Override
    protected void handleUserErrorMessage(RequestContext ctx) {
        if (ctx.getOriginalRequestInfo() != null && ctx.getOriginalRequestInfo().getSourceMessage() != null && ctx.getOriginalRequestInfo().getSourceMessage().getType(WinningMessageTypes.class) == WinningMessageTypes.USER_WINNING_COMPONENT_STOP && ctx.getMessage().getError() != null && ctx.getMessage().getError().getCode().equals(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS)) {
            ctx.getMessage().setError(new JsonMessageError(new F4MFatalErrorException("Insufficient funds in paydent account while transferring from tenant ")));
        }
        super.handleUserErrorMessage(ctx);
    }

	private void onResultEngineMessage(ResultEngineMessageTypes messageType, RequestContext context) {
		switch (messageType) {
		case GET_RESULTS_INTERNAL_RESPONSE:
			onGetResultsResponse(context.getMessage(), context.getOriginalRequestInfo());
			break;
		case STORE_USER_WINNING_COMPONENT_RESPONSE:
			onUserWinningComponentAssignGotStoreWinningComponentResponse(context.getOriginalRequestInfo());
			break;
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + messageType + "]");
		}
	}

	private void onPaymentMessage(PaymentMessageTypes messageType, RequestContext context) {
		if (PaymentMessageTypes.isTransferFundsResponseWithTransactionId(messageType)) {
			onTransferBetweenAccountsResponse(context.getMessage(), context.getOriginalRequestInfo());
		} else if(PaymentMessageTypes.GET_ACCOUNT_BALANCE_RESPONSE.equals(messageType)) {
			LOGGER.debug("onPaymentMessage context {} ", context);
			onAccountBalanceResponse(context.getMessage(), context.getOriginalRequestInfo());
		} else {
			throw new F4MValidationFailedException("Unsupported message type[" + messageType + "]");
		}
	}

	private void saveRewardEvent(ClientInfo clientInfo, WinningOption winning, UserWinningComponent userWinningComponent) {
		RewardEvent rewardEvent =  new RewardEvent();
		final WinningOptionType winningOptionType = winning.getType();
		if (userWinningComponent.getType() == WinningComponentType.PAID) {
			rewardEvent.setPaidWinningComponentsPlayed(true);
		} else if (userWinningComponent.getType() == WinningComponentType.FREE) {
			rewardEvent.setFreeWinningComponentsPlayed(true);
		}

		if (winningOptionType == WinningOptionType.SUPER) {
			rewardEvent.setSuperPrizeWon(true);
		} else if (winningOptionType == WinningOptionType.CREDITS) {
			rewardEvent.setCreditWon(winning.getAmount().longValue());
		} else if (winningOptionType == WinningOptionType.BONUS) {
			rewardEvent.setBonusPointsWon(winning.getAmount().longValue());
		} else if (winningOptionType == WinningOptionType.MONEY) {
			rewardEvent.setMoneyWon(winning.getAmount());
		}

		tracker.addEvent(clientInfo, rewardEvent);
	}

    private void saveRewardEvent(ClientInfo clientInfo, String voucherId, UserWinningComponent userWinningComponent) {
        RewardEvent rewardEvent =  new RewardEvent();
        rewardEvent.setVoucherId(Long.valueOf(voucherId));
        rewardEvent.setVoucherWon(true);
        if (userWinningComponent!=null && StringUtils.isNotBlank(userWinningComponent.getGameId())) {
			rewardEvent.setGameId(userWinningComponent.getGameId());
		}
        tracker.addEvent(clientInfo, rewardEvent);
    }

    private void onVoucherMessage(VoucherMessageTypes messageType, RequestContext context) {
        UserWinningComponentRequestInfo requestInfo = context.getOriginalRequestInfo();
        if (VoucherMessageTypes.USER_VOUCHER_ASSIGN_RESPONSE.equals(messageType)) {
            final UserVoucherAssignResponse userVoucherAssignResponse = (UserVoucherAssignResponse) context.getMessage().getContent();
            saveRewardEvent(requestInfo.getSourceMessage().getClientInfo(), userVoucherAssignResponse.getVoucherId(), requestInfo.getUserWinningComponent());
            sendResponseToWinningComponentStop(requestInfo);
        } else {
            throw new F4MValidationFailedException("Unsupported message type[" + messageType + "]");
        }
    }

	@SuppressWarnings("unchecked")
    private void onGetResultsResponse(JsonMessage<GetResultsResponse> message, RequestInfo requestInfo) {
        final JsonMessage<? extends JsonMessageContent> sourceMessage = requestInfo.getSourceMessage();
        final WinningMessageTypes sourceMessageType = sourceMessage.getType(WinningMessageTypes.class);
        switch (sourceMessageType) {
            case USER_WINNING_COMPONENT_ASSIGN:
                final GetResultsResponse responseContent = message.getContent();
                final Results results = new Results(responseContent.getResults());
                onWinningComponentAssignGotGetResultsResponse((JsonMessage<UserWinningComponentAssignRequest>) sourceMessage,
															  requestInfo.getSourceSession(),
															  results);
                break;
            default:
                throw new F4MValidationFailedException("Unsupported source message type[" + sourceMessageType + "]");
        }
    }


	private void onWinningComponentAssignGotGetResultsResponse(JsonMessage<UserWinningComponentAssignRequest> sourceMessage, SessionWrapper sourceSession,
															   Results results)
	{
		LOGGER.debug("onWinningComponentAssignGotGetResultsResponse sourceMessage {} ", sourceMessage);
		final boolean isEligibleToWinnings;
		final String freeWinningComponentId;
		final String paidWinningComponentId;
		final String gameInstanceId = sourceMessage.getContent().getGameInstanceId();
		final Game game = gameInstanceDao.getGameByInstanceId(gameInstanceId);
		if (game.getType() == GameType.TIPP_TOURNAMENT) {
			// in TIPP_TOURNAMENT results = = null, because the calculation of the results takes place after the end of the tournament,
			// for TIPP_TOURNAMENT in any case, a spinner is given.
			isEligibleToWinnings = true;
			freeWinningComponentId = null;
			paidWinningComponentId = null;
		}else{
			assert results != null;
			isEligibleToWinnings= results.isEligibleToWinnings();
			freeWinningComponentId = results.getFreeWinningComponentId();
			paidWinningComponentId = results.getPaidWinningComponentId();
		}

		final String winningComponentId = winningManager.getWinningComponentId(sourceMessage, freeWinningComponentId, paidWinningComponentId);

		winningManager.assignWinningComponentPayment(winningComponentId, game, sourceMessage, sourceSession, isEligibleToWinnings, gameInstanceId);
	}

    @SuppressWarnings("unchecked")
    private void onTransferBetweenAccountsResponse(JsonMessage<TransactionId> message, PaymentRequestInfo requestInfo) {
        final TransactionId transactionId = message.getContent();
        transactionLogAerospikeDao.updateTransactionLog(requestInfo.getTransactionLogId(), transactionId.getTransactionId(),
														message.getError() == null ? TransactionStatus.COMPLETED : TransactionStatus.ERROR);

        final JsonMessage<? extends JsonMessageContent> sourceMessage = requestInfo.getSourceMessage();
        final WinningMessageTypes sourceMessageType = sourceMessage.getType(WinningMessageTypes.class);
		LOGGER.debug("onTransferBetweenAccountsResponse requestInfo.isEligibleToComponent()={}",requestInfo.isEligibleToComponent());
        switch (sourceMessageType) {
            case USER_WINNING_COMPONENT_ASSIGN:
                    final UserWinningComponentAssignRequest req = (UserWinningComponentAssignRequest) sourceMessage.getContent();
                    final String winningComponentId = requestInfo.getWinningComponentId();
					LOGGER.debug("onTransferBetweenAccountsResponse winningComponentId={}",winningComponentId);
					final WinningComponent winningComponent = message.getError() == null
							? winningComponentManager.getWinningComponent(sourceMessage.getClientInfo().getTenantId(), winningComponentId)
							: null;
                    GameWinningComponentListItem winningComponentConfiguration = null;
                    if (winningComponent != null) {
                        final Game game = gameInstanceDao.getGameByInstanceId(req.getGameInstanceId());
                        winningComponentConfiguration = game.getWinningComponent(winningComponentId);
                    }
                    winningManager.onWinningComponentAssignGotTransferBetweenAccountsResponse(
                    		true,
							(JsonMessage<UserWinningComponentAssignRequest>) sourceMessage,
							requestInfo.getSourceSession(),
							winningComponent,
							winningComponentConfiguration,
							requestInfo.isEligibleToWinnings(),
							requestInfo.isEligibleToComponent(),
							message.getError());
					break;
            case USER_WINNING_COMPONENT_STOP:
                sendResponseToWinningComponentStop((UserWinningComponentRequestInfo) requestInfo);
                break;
            default:
                throw new F4MValidationFailedException("Unsupported source message type[" + sourceMessageType + "]");
        }
    }


	private void onAccountBalanceResponse(JsonMessage<GetAccountBalanceResponse> message, WinningComponentMoneyCheckRequestInfo requestInfo) {
		respondToAccountBalanceRequest(requestInfo, message.getContent().getAmount());
	}

	private void respondToAccountBalanceRequest(WinningComponentMoneyCheckRequestInfo requestInfo, BigDecimal amount) {
		String userId = requestInfo.getSourceMessage().getUserId();
		String userWinningComponentId = requestInfo.getUserWinningComponentId();
		String winningComponentId = requestInfo.getWinningComponentId();
		String winningOptionId = requestInfo.getWinningOptionId();
		String tenantId = requestInfo.getSourceMessage().getTenantId();
		String appId = requestInfo.getSourceMessage().getAppId();

        WinningComponent winningComponent = winningComponentManager.getWinningComponent(tenantId, winningComponentId);
        UserWinningComponent userWinningComponent = winningComponentManager.getUserWinningComponent(appId, userId, userWinningComponentId);
		WinningOption winning = winningComponent.getWinningOptions()
												.stream()
												.filter(o -> o.getWinningOptionId().equals(winningOptionId))
												.findFirst()
												.orElse(new WinningOption());

		BigDecimal winningAmount = null;
		if(winning.getType() == WinningOptionType.SUPER) {
			SuperPrize superPrize = superPrizeAerospikeDao.getSuperPrize(winning.getPrizeId());
			winningAmount = BigDecimal.valueOf(superPrize.getWinning());
		} else if(winning.getType() == WinningOptionType.MONEY) {
			winningAmount = winning.getAmount();
		}
		if (winningAmount != null && winningAmount.compareTo(amount) > 0) {
			// draw a new winning option, tenant does not have money to pay this out
			winning = winningManager.determineWinning(userId, winningComponent, true);
			// inform the administrator that tenant no longer has money in paydent account :
			userMessageServiceCommunicator.notifyAdministratorInsufficientTenantFunds(userId, winningComponent, tenantId);
		}
		if (winning != null) {

			saveRewardEvent(requestInfo.getSourceMessage().getClientInfo(), winning, userWinningComponent);
			winningComponentManager.saveUserWinningComponentWinningOption(requestInfo.getSourceMessage().getAppId(), userId, userWinningComponentId, winning);
		}

		sendResponse(requestInfo.getSourceMessage(), new UserWinningComponentStartResponse(winning), requestInfo.getSourceSession());
	}

    private void onUserWinningComponentAssignGotStoreWinningComponentResponse(UserWinningComponentRequestInfo requestInfo) {
        UserWinningComponent userWinningComponent = requestInfo.getUserWinningComponent();
        UserWinningComponentAssignResponse response = userWinningComponent == null ? new UserWinningComponentAssignResponse() : new UserWinningComponentAssignResponse(userWinningComponent);
        sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
    }

    private void sendResponseToWinningComponentStop(UserWinningComponentRequestInfo requestInfo) {
        final ClientInfo clientInfo = requestInfo.getSourceMessage().getClientInfo();
        final String userId = clientInfo.getUserId();
        final String appId = clientInfo.getAppId();
        if (requestInfo.getUserWinningComponent().getWinning().getType() != WinningOptionType.VOUCHER) {
            userWinningDao.saveUserWinning(appId, userId, new UserWinning(requestInfo.getUserWinningComponent(), requestInfo.getSuperPrize()));
        }
        winningComponentManager.markUserWinningComponentFiled(appId, userId, requestInfo.getUserWinningComponent().getUserWinningComponentId());
        sendResponse(requestInfo.getSourceMessage(), new UserWinningComponentStopRequest(requestInfo.getUserWinningComponent().getUserWinningComponentId()), requestInfo.getSourceSession());
    }

    private void onEventServiceMessage(JsonMessage<? extends JsonMessageContent> message, EventMessageTypes type) {
        if (type == EventMessageTypes.NOTIFY_SUBSCRIBER) {
            onNotifyMessage((NotifySubscriberMessageContent) message.getContent());
        } else if (type == EventMessageTypes.SUBSCRIBE_RESPONSE) {
            LOGGER.debug("Received SUBSCRIBE_RESPONSE message: {}", message);
        } else {
            LOGGER.error("Unexpected message with type {}", type);
        }
    }

	private void onNotifyMessage(NotifySubscriberMessageContent notifySubscriberMessageContent) {
		if (ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC.equals(notifySubscriberMessageContent.getTopic())) {
			ProfileMergeEvent event = new ProfileMergeEvent(notifySubscriberMessageContent.getNotificationContent().getAsJsonObject());
			Profile profile = profileDao.getProfile(event.getSourceUserId());
			if (profile == null) {
				profile = profileDao.getProfile(event.getTargetUserId());
			}
			winningComponentManager.moveWinningComponents(event.getSourceUserId(), event.getTargetUserId(), profile.getApplications());
			winningManager.moveWinnings(event.getSourceUserId(), event.getTargetUserId(), profile.getApplications());
		} else {
    		LOGGER.error("Unexpected message with topic {}", notifySubscriberMessageContent.getTopic());
		}
	}

}
