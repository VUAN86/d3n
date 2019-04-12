package de.ascendro.f4m.service.voucher.client;

import java.math.BigDecimal;

import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.analytics.model.InvoiceEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperResponse;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.UserVoucher;
import de.ascendro.f4m.service.voucher.model.Voucher;
import de.ascendro.f4m.service.voucher.model.voucher.VoucherPurchaseRequest;
import de.ascendro.f4m.service.voucher.util.VoucherUtil;

public class VoucherServiceClientMessageHandler extends JsonAuthenticationMessageMQHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(VoucherServiceClientMessageHandler.class);
	private VoucherUtil voucherUtil;
    private DependencyServicesCommunicator dependencyServicesCommunicator;
	private TransactionLogAerospikeDao transactionLogDao;
//	private final EventServiceClient eventServiceClient;
	private final Tracker tracker;

    public VoucherServiceClientMessageHandler(DependencyServicesCommunicator dependencyServicesCommunicator, VoucherUtil voucherUtil,
//    		EventServiceClient eventServiceClient,
											  TransactionLogAerospikeDao transactionLogDao, Tracker tracker) {
        this.dependencyServicesCommunicator = dependencyServicesCommunicator;
        this.voucherUtil = voucherUtil;
//		this.eventServiceClient = eventServiceClient;
		this.transactionLogDao = transactionLogDao;
		this.tracker = tracker;
	}

//	@Override	TODO
//	protected void onEventServiceRegistered() {
//		eventServiceClient.subscribe(true, VoucherMessageTypes.SERVICE_NAME, ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC);
//	}

	@Override
	public JsonMessageContent onUserMessage(RequestContext context) {
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		VoucherRequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		EventMessageTypes eventMessageType;
		if ((eventMessageType = message.getType(EventMessageTypes.class)) != null) {
			return onEventMessage(message, eventMessageType);
		} else if (originalRequestInfo != null) {
			if (message.getContent() instanceof SendEmailWrapperResponse) {
				// Nothing to do here
			} else if (message.getContent() instanceof TransactionId) {
				processReceivedTransactionId(message, originalRequestInfo);
			} else {
				LOGGER.error("Unrecognized response message received {}", message);
			}
		}
		return null;
	}

	@Override
	public void onUserErrorMessage(RequestContext requestContext) {
		PaymentMessageTypes paymentMessageType = requestContext.getMessage().getType(PaymentMessageTypes.class);
		if (PaymentMessageTypes.isTransferFundsResponseWithTransactionId(paymentMessageType)
				&& requestContext.getOriginalRequestInfo() != null) {
			if (requestContext.getOriginalRequestInfo() instanceof VoucherRequestInfo) {
				VoucherRequestInfo originalRequestInfo = (VoucherRequestInfo) requestContext.getOriginalRequestInfo();
				voucherUtil.releaseVoucher(originalRequestInfo.getVoucherId());
				transactionLogDao.updateTransactionLog(originalRequestInfo.getTransactionLogId(),  null, TransactionStatus.ERROR);
			} else {
				LOGGER.warn("Cannot release voucher, instead of {} received {}", VoucherRequestInfo.class, requestContext.getOriginalRequestInfo());
			}
		}
	}

	private void processReceivedTransactionId(JsonMessage<? extends JsonMessageContent> message, RequestInfo originalRequestInfo) {
		VoucherRequestInfo voucherRequestInfo = (VoucherRequestInfo) originalRequestInfo;
		if (voucherRequestInfo.getSourceMessage() != null && voucherRequestInfo.getSourceMessage().getContent() instanceof VoucherPurchaseRequest) {
			processVoucherPurchaseReceivedTransactionId(message, voucherRequestInfo);
		} else {
			LOGGER.error("Unknown original message: {}", voucherRequestInfo.getSourceMessage());
		}
	}

	private void processVoucherPurchaseReceivedTransactionId(JsonMessage<? extends JsonMessageContent> message, VoucherRequestInfo voucherRequestInfo) {
		ClientInfo clientInfo = voucherRequestInfo.getSourceMessage().getClientInfo();
		TransactionId transactionId = (TransactionId) message.getContent();
		if(StringUtils.isNotEmpty(transactionId.getTransactionId())) {
            transactionLogDao.updateTransactionLog(voucherRequestInfo.getTransactionLogId(), null, TransactionStatus.ERROR);
			// add the voucher to the user profile :
    		String voucherId = voucherRequestInfo.getVoucherId();
    		String userVoucherId = voucherUtil.assignVoucherToUser(voucherRequestInfo.getVoucherId(), clientInfo.getUserId(), null, null, "purchase");
    		String fullUserVoucherId = VoucherUtil.createUserVoucherIdForResponse(voucherId, userVoucherId);
    		UserVoucher userVoucher = voucherUtil.getUserVoucherById(fullUserVoucherId);
    		Voucher voucher = voucherUtil.getVoucherById(voucherId);

			addInvoiceEvent(clientInfo, BigDecimal.valueOf(voucher.getBonuspointsCosts()));

    		// Send e-mail
    		dependencyServicesCommunicator.sendBoughtVoucherAssignedEmailToUser(clientInfo.getUserId(), userVoucher, voucher,
					clientInfo.getTenantId(), clientInfo.getAppId());
            
    		sendResponse(voucherRequestInfo.getSourceMessage(), new EmptyJsonMessageContent(), voucherRequestInfo.getSourceMessageSource());
        } else {
            // error occurred, release lock :
            voucherUtil.releaseVoucher(voucherRequestInfo.getVoucherId());
            throw new F4MFatalErrorException("No transactionId in successful payment response");
        }
	}

	private void addInvoiceEvent(ClientInfo clientInfo, BigDecimal amount) {
		InvoiceEvent invoiceEvent = new InvoiceEvent();
		invoiceEvent.setPaymentType(InvoiceEvent.PaymentType.VOUCHER);
		invoiceEvent.setPaymentAmount(amount);
		invoiceEvent.setCurrency(Currency.BONUS.name());
		tracker.addEvent(clientInfo, invoiceEvent);
	}
	

	private JsonMessageContent onEventMessage(JsonMessage<? extends JsonMessageContent> message,
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
			voucherUtil.moveVouchers(event.getSourceUserId(), event.getTargetUserId());
		} else {
			LOGGER.error("Unexpected message with topic {}", notifySubscriberMessageContent.getTopic());
		}
	}
}
