package de.ascendro.f4m.service.game.engine.client.voucher;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherReleaseRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherReserveRequest;

public class VoucherCommunicatorImpl implements VoucherCommunicator {

	private final JsonMessageUtil jsonMessageUtil;
	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final ServiceRegistryClient serviceRegistryClient;

	@Inject
	public VoucherCommunicatorImpl(JsonMessageUtil jsonMessageUtil,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, ServiceRegistryClient serviceRegistryClient) {
		this.jsonMessageUtil = jsonMessageUtil;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.serviceRegistryClient = serviceRegistryClient;
	}

	@Override
	public void requestUserVoucherReserve(String voucherId, JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper session) {
		final UserVoucherReserveRequest request = new UserVoucherReserveRequest(voucherId);

		final JsonMessage<UserVoucherReserveRequest> message = jsonMessageUtil.createNewMessage(
				VoucherMessageTypes.USER_VOUCHER_RESERVE, request);
		final RequestInfo requestInfo = new RequestInfoImpl();
		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceSession(session);
		try {
			final ServiceConnectionInformation connInfo = serviceRegistryClient.getServiceConnectionInformation(
					VoucherMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send reserve voucher request to voucher service", e);
		}
	}

	@Override
	public void requestUserVoucherRelease(String voucherId, JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper session) {
		final UserVoucherReleaseRequest request = new UserVoucherReleaseRequest(voucherId);

		final JsonMessage<UserVoucherReleaseRequest> message = jsonMessageUtil.createNewMessage(
				VoucherMessageTypes.USER_VOUCHER_RELEASE, request);
		final RequestInfo requestInfo = new RequestInfoImpl();
		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceSession(session);
		try {
			final ServiceConnectionInformation connInfo = serviceRegistryClient.getServiceConnectionInformation(
					VoucherMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send release voucher request to voucher service", e);
		}
	}
}
