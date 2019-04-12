package de.ascendro.f4m.service.tombola.client;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;

import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.tombola.model.TombolaStatus;
import de.ascendro.f4m.service.util.DateTimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.analytics.model.InvoiceEvent;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.tombola.TombolaDrawingEngine;
import de.ascendro.f4m.service.tombola.TombolaManager;
import de.ascendro.f4m.service.tombola.TombolaMessageTypes;
import de.ascendro.f4m.service.tombola.model.Tombola;
import de.ascendro.f4m.service.tombola.model.TombolaWinner;
import de.ascendro.f4m.service.tombola.model.events.TombolaEvents;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.winning.model.UserWinning;
import de.ascendro.f4m.service.winning.model.UserWinningType;

public class TombolaServiceClientMessageHandler extends JsonAuthenticationMessageMQHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(TombolaServiceClientMessageHandler.class);

	private static final String UNEXPECTED_MESSAGE_TYPE = "Unexpected message type";
	private static final String UNRECOGNIZED_MESSAGE_TYPE = "Unrecognized message type";
	private final TombolaManager tombolaManager;
	private final TombolaDrawingEngine tombolaDrawingEngine;
//	private final EventServiceClient eventServiceClient;
	private final Tracker tracker;
	private final DependencyServicesCommunicator dependencyServicesCommunicator;
	private final CommonUserWinningAerospikeDao userWinningDao;

	public TombolaServiceClientMessageHandler(TombolaManager tombolaManager,
//											  EventServiceClient eventServiceUtil,
			TombolaDrawingEngine tombolaDrawingEngine, Tracker tracker,
			DependencyServicesCommunicator dependencyServicesCommunicator,
			CommonUserWinningAerospikeDao userWinningDao) {
//		this.eventServiceClient = eventServiceUtil;
		this.tombolaManager = tombolaManager;
		this.tombolaDrawingEngine = tombolaDrawingEngine;
		this.tracker = tracker;
		this.dependencyServicesCommunicator = dependencyServicesCommunicator;
		this.userWinningDao = userWinningDao;
	}

//	@Override //TODO
//	protected void onEventServiceRegistered() {
//		eventServiceClient.subscribe(true, TombolaMessageTypes.SERVICE_NAME, ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC);
//		eventServiceClient.subscribe(true, TombolaMessageTypes.SERVICE_NAME, TombolaEvents.getDrawEventTopic());
//		eventServiceClient.subscribe(true, TombolaMessageTypes.SERVICE_NAME, TombolaEvents.getOpenCheckoutTopic());
//		eventServiceClient.subscribe(true, TombolaMessageTypes.SERVICE_NAME, TombolaEvents.getCloseCheckoutTopic());
//	}

	@Override
	public JsonMessageContent onUserMessage(RequestContext context) {

		final EventMessageTypes eventMessageType;
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		RequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		if (PaymentMessageTypes.isTransferFundsResponseWithTransactionId(message.getType(PaymentMessageTypes.class)) &&
				originalRequestInfo != null) {
			if (originalRequestInfo instanceof PrizePayoutRequestInfo) {
				processReceivedTransactionIdForPrizePayout(message, context);
			} else if (originalRequestInfo instanceof TicketPurchaseRequestInfo) {
				processReceivedTransactionIdForTicketBuy(message, context);
			} else {
				throw new F4MValidationFailedException(UNEXPECTED_MESSAGE_TYPE);
			}
		} else if (VoucherMessageTypes.USER_VOUCHER_ASSIGN_FOR_TOMBOLA_RESPONSE ==
				message.getType(VoucherMessageTypes.class)) {
			if (originalRequestInfo instanceof PrizePayoutRequestInfo) {
				processPrizePayoutVoucherAssignResponse(message, (PrizePayoutRequestInfo) originalRequestInfo);
			} else {
				throw new F4MValidationFailedException(UNEXPECTED_MESSAGE_TYPE);
			}
		} else if (VoucherMessageTypes.USER_VOUCHER_RELEASE_RESPONSE == message.getType(VoucherMessageTypes.class)) {
			// nothing to do
		} else if ((eventMessageType = message.getType(EventMessageTypes.class)) != null) {
			onEventMessage(message, eventMessageType);
		} else if(UserMessageMessageTypes.SEND_TOPIC_PUSH_RESPONSE == message.getType(UserMessageMessageTypes.class)) {
			// nothing to do
		} else if(UserMessageMessageTypes.SEND_EMAIL_RESPONSE == message.getType(UserMessageMessageTypes.class)) {
			// nothing to do
		} else {
			throw new F4MValidationFailedException(UNRECOGNIZED_MESSAGE_TYPE);
		}
		return null;
	}

	@Override
	public void onUserErrorMessage(RequestContext context) {
		RequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = context.getMessage();
		if(PaymentMessageTypes.isTransferFundsResponseWithTransactionId(
				originalMessageDecoded.getType(PaymentMessageTypes.class))) {
			if (originalRequestInfo instanceof PrizePayoutRequestInfo) {
				onReceivedTransactionIdWithErrorForPrizePayout(context.getOriginalRequestInfo(),
						originalMessageDecoded.getError().getCode());
			} else if (originalRequestInfo instanceof TicketPurchaseRequestInfo) {
				onReceivedTransactionIdWithErrorForTicketBuy(context.getOriginalRequestInfo());
			}
		} else if (VoucherMessageTypes.USER_VOUCHER_ASSIGN_FOR_TOMBOLA_RESPONSE ==
				originalMessageDecoded.getType(VoucherMessageTypes.class)) {
			if (originalRequestInfo instanceof PrizePayoutRequestInfo) {
				onReceivedVoucherAssignResponseWithErrorForPrizePayout(context.getOriginalRequestInfo(),
						originalMessageDecoded.getError().getCode(), originalMessageDecoded);
			} else {
				throw new F4MValidationFailedException(UNEXPECTED_MESSAGE_TYPE);
			}
		}
	}

	@Override
	protected boolean forwardErrorMessageToOriginIfExists(String originalMessageEncoded, RequestContext requestContext, Throwable e, JsonMessageError error) {
		boolean forwarded = false;
		RequestInfo originalRequestInfo = requestContext.getOriginalRequestInfo();
		// Don't forward prize payout errors to origin, they will be sent as emails to admin
		if ( !(originalRequestInfo instanceof PrizePayoutRequestInfo)) {
			forwarded = super.forwardErrorMessageToOriginIfExists(originalMessageEncoded, requestContext, e, error);
		}
		return forwarded;
	}

	private void processReceivedTransactionIdForTicketBuy(JsonMessage<? extends JsonMessageContent> message,
														  RequestContext context) {
		TicketPurchaseRequestInfo originalRequestInfo = context.getOriginalRequestInfo();

		TransactionId transactionId = (TransactionId) message.getContent();
		if (StringUtils.isNotEmpty(transactionId.getTransactionId())) {
			processTicketPurchaseReceived(message, originalRequestInfo);
		} else {
			onReceivedTransactionIdWithErrorForTicketBuy(context.getOriginalRequestInfo());
		}
	}

	private void processReceivedTransactionIdForPrizePayout(JsonMessage<? extends JsonMessageContent> message,
															RequestContext context) {
		PrizePayoutRequestInfo originalRequestInfo = context.getOriginalRequestInfo();

		TransactionId transactionId = (TransactionId) message.getContent();
		if (StringUtils.isNotEmpty(transactionId.getTransactionId())) {
			processPrizePayoutReceivedTransactionId(message, originalRequestInfo);
		} else {
			onReceivedTransactionIdWithErrorForPrizePayout(context.getOriginalRequestInfo(),
					"Transaction ID empty on response");
		}
	}

	private void processTicketPurchaseReceived(JsonMessage<? extends JsonMessageContent> message,
											   TicketPurchaseRequestInfo originalRequestInfo) {
		ClientInfo clientInfo = originalRequestInfo.getSourceMessage().getClientInfo();

		addInvoiceEvent(clientInfo, originalRequestInfo.getPrice(), originalRequestInfo.getCurrency());

		tombolaManager.buyTickets(originalRequestInfo.getNumberOfTicketsBought(), clientInfo.getUserId(),
				originalRequestInfo.getTombolaId(), originalRequestInfo.getPrice(), originalRequestInfo.getCurrency(),
				originalRequestInfo.getBundleImageId(), originalRequestInfo.getAppId(),
				originalRequestInfo.getTenantId());

		Tombola tombola = tombolaManager.getTombola(originalRequestInfo.getTombolaId());

//		CON-89 dont stop tombola in tickets is empty
//		if (!tombola.isInfinityTickets() && tombola.getTotalTicketsAmount() <= tombola.getPurchasedTicketsAmount()) {
//			dependencyServicesCommunicator.requestTombolaDraw(message, getSessionWrapper(),
//					originalRequestInfo.getTombolaId(), clientInfo.getTenantId());
//		}

		JsonMessage<? extends JsonMessageContent> original = originalRequestInfo.getSourceMessage();
		EmptyJsonMessageContent response = new EmptyJsonMessageContent();
		this.sendResponse(original, response, originalRequestInfo.getSourceMessageSource());

	}

	private void processPrizePayoutReceivedTransactionId(JsonMessage<? extends JsonMessageContent> message,
														 PrizePayoutRequestInfo originalRequestInfo) {
		Tombola tombola = originalRequestInfo.getTombola();
		TombolaWinner winner = originalRequestInfo.getTombolaWinner();

		UserWinning userWinning = new UserWinning(tombola.getName(), UserWinningType.TOMBOLA, winner.getAmount(), winner.getType().toCurrency(),
				tombola.getDescription(), tombola.getImageId(), null);
		userWinning.setTombolaId(tombola.getId());
		userWinningDao.saveUserWinning(originalRequestInfo.getAppId(), originalRequestInfo.getUserId(), userWinning);

		RewardEvent rewardEvent = new RewardEvent();
		rewardEvent.setPrizeWon(originalRequestInfo.getAmount(), originalRequestInfo.getCurrency());
		ClientInfo clientInfo = Optional.ofNullable(message.getClientInfo()).orElse(new ClientInfo());
		clientInfo.setTenantId(originalRequestInfo.getTenantId());
		clientInfo.setAppId(originalRequestInfo.getAppId());
		clientInfo.setUserId(originalRequestInfo.getUserId());
		tracker.addEvent(clientInfo, rewardEvent);
	}

	private void processPrizePayoutVoucherAssignResponse(JsonMessage<? extends JsonMessageContent> message,
														 PrizePayoutRequestInfo originalRequestInfo) {
		RewardEvent rewardEvent = new RewardEvent();
		rewardEvent.setVoucherWon(true);
		rewardEvent.setVoucherId(originalRequestInfo.getVoucherId());
		rewardEvent.setTombolaId(originalRequestInfo.getTombolaId());
		ClientInfo clientInfo = Optional.ofNullable(message.getClientInfo()).orElse(new ClientInfo());
		clientInfo.setTenantId(originalRequestInfo.getTenantId());
		clientInfo.setAppId(originalRequestInfo.getAppId());
		clientInfo.setUserId(originalRequestInfo.getUserId());
		tracker.addEvent(clientInfo, rewardEvent);
	}

	private void addInvoiceEvent(ClientInfo clientInfo, BigDecimal amount, Currency currency) {
		InvoiceEvent invoiceEvent = new InvoiceEvent();
		invoiceEvent.setPaymentType(InvoiceEvent.PaymentType.TOMBOLA);
		invoiceEvent.setPaymentAmount(amount);
		invoiceEvent.setCurrency(currency);
		tracker.addEvent(clientInfo, invoiceEvent);
	}

	private void onReceivedTransactionIdWithErrorForTicketBuy(TicketPurchaseRequestInfo requestInfo) {
		// error occurred, release lock :
		ClientInfo clientInfo = requestInfo.getSourceMessage().getClientInfo();
		tombolaManager.cancelBuyTickets(requestInfo.getNumberOfTicketsBought(), clientInfo.getUserId(),
				requestInfo.getTombolaId());
	}

	private void onReceivedTransactionIdWithErrorForPrizePayout(PrizePayoutRequestInfo requestInfo,
																String errorMessage) {
		tombolaManager.sendEmailToAdmin(requestInfo, errorMessage);
	}

	private void onReceivedVoucherAssignResponseWithErrorForPrizePayout(PrizePayoutRequestInfo requestInfo,
																		String errorMessage, JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		dependencyServicesCommunicator.requestUserVoucherRelease(originalMessageDecoded, requestInfo.getVoucherId());
		tombolaManager.sendEmailToAdmin(requestInfo, errorMessage);
	}

	@SuppressWarnings("unchecked")
	private void onEventMessage(JsonMessage<? extends JsonMessageContent> message,
								EventMessageTypes eventMessageType) {
		if (eventMessageType == EventMessageTypes.NOTIFY_SUBSCRIBER) {
			onNotifyMessage((JsonMessage<NotifySubscriberMessageContent>) message);
		} else if (eventMessageType == EventMessageTypes.SUBSCRIBE_RESPONSE) {
			LOGGER.debug("Received SUBSCRIBE_RESPONSE message: {}", message);
		} else {
			LOGGER.error("Unexpected message with type {}", eventMessageType);
		}
	}

	private void onNotifyMessage(JsonMessage<NotifySubscriberMessageContent> message) {
		NotifySubscriberMessageContent messageContent = message.getContent();
		String topic = messageContent.getTopic();
		if (ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC.equals(topic)) {
			onProfileMergeEvent(messageContent);
		} else if (TombolaEvents.isDrawEventTopic(topic)) {
			onTombolaDrawEvent(message);
		} else if (TombolaEvents.isOpenCheckoutTopic(topic)) {
			onTombolaOpenCheckoutEvent(messageContent);
		} else if (TombolaEvents.isCloseCheckoutTopic(topic)) {
			onTombolaCloseCheckoutEvent(messageContent);
		} else if (TombolaEvents.isPreCloseCheckoutTopic(topic)) {
			onTombolaPreCloseCheckoutEvent(messageContent);
		} else {
			LOGGER.error("Unexpected message with topic {}", messageContent.getTopic());
		}
	}

	private void onTombolaCloseCheckoutEvent(NotifySubscriberMessageContent messageContent) {
		TombolaEvents closeCheckoutEvent = new TombolaEvents(messageContent.getNotificationContent().getAsJsonObject());
		tombolaManager.handleCloseCheckoutEvent(closeCheckoutEvent.getTombolaId());
	}

	private void onTombolaPreCloseCheckoutEvent(NotifySubscriberMessageContent messageContent) {
		TombolaEvents event = new TombolaEvents(messageContent.getNotificationContent().getAsJsonObject());
		tombolaManager.handlePreCloseCheckoutEvent(event.getTombolaId(), event.getMinutesToCheckout());
	}

	private void onTombolaOpenCheckoutEvent(NotifySubscriberMessageContent messageContent) {
		TombolaEvents openCheckoutEvent = new TombolaEvents(messageContent.getNotificationContent().getAsJsonObject());
		Tombola tombola = tombolaManager.getTombola(openCheckoutEvent.getTombolaId());
		ZonedDateTime endDate = DateTimeUtil.parseISODateTimeString(tombola.getEndDate());
		if (!endDate.isBefore(ZonedDateTime.now())) {
			tombolaManager.handleOpenCheckoutEvent(openCheckoutEvent.getTombolaId());
			tombola.setStatus(TombolaStatus.ACTIVE);
			tombolaManager.sendPushNotificationOnOpenTombola(tombola);
		} else {
			LOGGER.warn("Stoped OpenCheckout expired Tombola id={}, endDate={}",tombola.getId(),tombola.getEndDate());
		}
	}

	private void onTombolaDrawEvent(JsonMessage<NotifySubscriberMessageContent> message) {
		NotifySubscriberMessageContent messageContent = message.getContent();
		TombolaEvents drawEvent = new TombolaEvents(messageContent.getNotificationContent().getAsJsonObject());
		tombolaDrawingEngine.tombolaDraw(drawEvent.getTombolaId(), drawEvent.getTenantId(),message);
	}

	private void onProfileMergeEvent(NotifySubscriberMessageContent messageContent) {
		ProfileMergeEvent event = new ProfileMergeEvent(messageContent.getNotificationContent().getAsJsonObject());
		tombolaManager.moveTombolas(event.getSourceUserId(), event.getTargetUserId());
	}

}
