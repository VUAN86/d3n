package de.ascendro.f4m.service.promocode.client;

import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.promocode.model.UserPromocode;
import de.ascendro.f4m.service.promocode.util.PromocodeManager;

public class PromocodeServiceClientMessageHandler extends JsonAuthenticationMessageMQHandler  {

	private PromocodeManager promocodeManager;
	private TransactionLogAerospikeDao transactionLogAerospikeDao;

	public PromocodeServiceClientMessageHandler(PromocodeManager promocodeManager, TransactionLogAerospikeDao transactionLogAerospikeDao) {
		this.promocodeManager = promocodeManager;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
	}

	@Override
	public JsonMessageContent onUserMessage(RequestContext context) {
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		if (PaymentMessageTypes.isTransferFundsResponseWithTransactionId(message.getType(PaymentMessageTypes.class))) {
			processReceivedTransactionId(message, context);
		}
		return null;
	}
	
	@Override
	public void onUserErrorMessage(RequestContext context) {
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = context.getMessage();
		if (PaymentMessageTypes.isTransferFundsResponseWithTransactionId(originalMessageDecoded.getType(PaymentMessageTypes.class))) {
			onReceivedTransactionIdWithError(context.getOriginalRequestInfo());
		}
	}

	private void processReceivedTransactionId(JsonMessage<? extends JsonMessageContent> message, RequestContext context) {
		PromocodeRequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		TransactionId transactionId = (TransactionId) message.getContent();
		if (StringUtils.isNotEmpty(transactionId.getTransactionId())) {
			onReceivedTransactionIdSuccess(message, originalRequestInfo, transactionId);
		} else {
			onReceivedTransactionIdWithError(context.getOriginalRequestInfo());
		}
	}

	private void onReceivedTransactionIdWithError(PromocodeRequestInfo requestInfo) {
		transactionLogAerospikeDao.updateTransactionLog(requestInfo.getTransactionLogId(), null, TransactionStatus.ERROR);
	}

	private void onReceivedTransactionIdSuccess(JsonMessage<? extends JsonMessageContent> message,
												PromocodeRequestInfo originalRequestInfo, TransactionId transactionId) {
		UserPromocode userPromocode = promocodeManager.getUserPromocodeById(originalRequestInfo.getPromocodeId());
												if(transactionId!=null&&userPromocode!=null){
		switch (originalRequestInfo.getCurrency()) {
			case BONUS:
				userPromocode.setBonuspointsTransactionId(transactionId.getTransactionId());
				break;
			case CREDIT:
				userPromocode.setCreditTransactionId(transactionId.getTransactionId());
				break;
			case MONEY:
				userPromocode.setMoneyTransactionId(transactionId.getTransactionId());
				break;
		}
		promocodeManager.updateUserPromocode(userPromocode);
	}
	}

}
