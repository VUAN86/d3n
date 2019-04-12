package de.ascendro.f4m.service.winning.client;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.communicator.RabbitClientSender;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignRequest;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;

public class VoucherServiceCommunicator {

	private final JsonMessageUtil jsonMessageUtil;
//	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
//	private final ServiceRegistryClient serviceRegistryClient;

	@Inject
	public VoucherServiceCommunicator(JsonMessageUtil jsonMessageUtil) {
//			, JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool,
//			ServiceRegistryClient serviceRegistryClient
		this.jsonMessageUtil = jsonMessageUtil;
//		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
//		this.serviceRegistryClient = serviceRegistryClient;
	}

	public void requestUserVoucherAssign(JsonMessage<? extends JsonMessageContent> sourceMessage,
			UserWinningComponent userWinningComponent, String tenantId, String appId) {
		final UserVoucherAssignRequest request = new UserVoucherAssignRequest(userWinningComponent.getWinning().getPrizeId(),
				sourceMessage.getClientInfo().getUserId(), userWinningComponent.getGameInstanceId(), tenantId, appId, "winningComponent");
		
		final JsonMessage<UserVoucherAssignRequest> message = jsonMessageUtil.createNewMessage(VoucherMessageTypes.USER_VOUCHER_ASSIGN, request);
		final RequestInfo requestInfo = new UserWinningComponentRequestInfo(sourceMessage, sourceMessage.getMessageSource(), userWinningComponent, null);
		try {
//			final ServiceConnectionInformation connInfo = serviceRegistryClient.getServiceConnectionInformation(VoucherMessageTypes.SERVICE_NAME);
			RabbitClientSender.sendAsyncMessageWithClientInfo(VoucherMessageTypes.SERVICE_NAME, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send add voucher to profile request to voucher service", e);
		}
	}

}
