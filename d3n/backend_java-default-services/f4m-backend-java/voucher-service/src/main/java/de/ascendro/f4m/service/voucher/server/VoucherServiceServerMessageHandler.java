package de.ascendro.f4m.service.voucher.server;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.analytics.model.VoucherUsedEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.voucher.exception.F4MVoucherIsNotExchangeException;
import de.ascendro.f4m.service.voucher.model.MoveVouchersRequest;
import de.ascendro.f4m.service.voucher.model.UserVoucher;
import de.ascendro.f4m.service.voucher.model.UserVoucherResponseModel;
import de.ascendro.f4m.service.voucher.model.Voucher;
import de.ascendro.f4m.service.voucher.model.VoucherListDeleteExpiredEntriesRequest;
import de.ascendro.f4m.service.voucher.model.VoucherResponseModel;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignForTombolaRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherGetRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherGetResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListByUserRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListByUserResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListByVoucherRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListByVoucherResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherReleaseRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherReserveRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherUseRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherUseResponse;
import de.ascendro.f4m.service.voucher.model.voucher.VoucherGetRequest;
import de.ascendro.f4m.service.voucher.model.voucher.VoucherGetResponse;
import de.ascendro.f4m.service.voucher.model.voucher.VoucherListRequest;
import de.ascendro.f4m.service.voucher.model.voucher.VoucherListResponse;
import de.ascendro.f4m.service.voucher.model.voucher.VoucherPurchaseRequest;
import de.ascendro.f4m.service.voucher.util.VoucherUtil;

/**
 * Voucher Service Jetty Server websocket message handler
 */
public class VoucherServiceServerMessageHandler extends DefaultJsonMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoucherServiceServerMessageHandler.class);
    private static final String NO_USER_ID_SPECIFIED = "No user id specified";
    private static final String NO_TENANT_ID_SPECIFIED = "No tenant id specified";

    private VoucherUtil voucherUtil;
	private DependencyServicesCommunicator dependencyServicesCommunicator;
	private final Tracker tracker;

	public VoucherServiceServerMessageHandler(VoucherUtil voucherUtil,
			DependencyServicesCommunicator dependencyServicesCommunicator, Tracker tracker) {
		this.voucherUtil = voucherUtil;
		this.dependencyServicesCommunicator = dependencyServicesCommunicator;
		this.tracker = tracker;
	}

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) {
		final VoucherMessageTypes voucherMessageType;
		if ((voucherMessageType = message.getType(VoucherMessageTypes.class)) != null) {
			return onVoucherMessage(message.getClientInfo(), message, voucherMessageType);
		} else {
			throw new F4MValidationFailedException("Unrecognized message type");
		}
	}

	@SuppressWarnings("unchecked")
	private JsonMessageContent onVoucherMessage(ClientInfo clientInfo,
			JsonMessage<? extends JsonMessageContent> message, VoucherMessageTypes voucherMessageType) {
		switch (voucherMessageType) {
        case VOUCHER_GET:
            return onVoucherGet((VoucherGetRequest) message.getContent());
        case VOUCHER_LIST:
            return onVoucherList(clientInfo, (VoucherListRequest) message.getContent());
        case VOUCHER_PURCHASE:
            return onVoucherPurchase(clientInfo, message);
        case USER_VOUCHER_ASSIGN:
			return onUserVoucherAssign(message);
        case USER_VOUCHER_LIST:
        	return onUserVoucherList(clientInfo, (UserVoucherListRequest) message.getContent());
        case USER_VOUCHER_GET:
        	return onUserVoucherGet((UserVoucherGetRequest) message.getContent());
        case USER_VOUCHER_USE:
        	return onUserVoucherUse(clientInfo, (UserVoucherUseRequest) message.getContent());
        case USER_VOUCHER_RESERVE:
        	return onVoucherReserve((UserVoucherReserveRequest) message.getContent());
		case USER_VOUCHER_ASSIGN_FOR_TOMBOLA:
			return onUserVoucherAssignForTombola(message);
        case USER_VOUCHER_RELEASE:
        	return onVoucherRelease((UserVoucherReleaseRequest) message.getContent());
		case USER_VOUCHER_LIST_BY_VOUCHER:
			return onVoucherCodesList((JsonMessage<UserVoucherListByVoucherRequest>) message);
		case USER_VOUCHER_LIST_BY_USER:
			return onUserVoucherListByUser((JsonMessage<UserVoucherListByUserRequest>) message);
		case MOVE_VOUCHERS:
			return onMoveVouchers((MoveVouchersRequest) message.getContent());
		case VOUCHER_LIST_DELETE_EXPIRED_ENTRIES:
			return onVoucherListDeleteExpiredEntries(clientInfo, (VoucherListDeleteExpiredEntriesRequest) message.getContent());
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + voucherMessageType + "]");
		}
	}

	private JsonMessageContent onUserVoucherAssignForTombola(JsonMessage<? extends JsonMessageContent> message) {
		UserVoucherAssignForTombolaRequest request = (UserVoucherAssignForTombolaRequest) message.getContent();
		String voucherId = request.getVoucherId();
		String userId = request.getUserId();
		String tombolaId = request.getTombolaId();
		String tombolaName = request.getTombolaName();
		String userVoucherId = voucherUtil.assignVoucherToUser(voucherId, userId, null, tombolaId, "tombola");
		String fullUserVoucherId = VoucherUtil.createUserVoucherIdForResponse(voucherId, userVoucherId);
		UserVoucher userVoucher = voucherUtil.getUserVoucherById(fullUserVoucherId);
		Voucher voucher = voucherUtil.getVoucherById(voucherId);

		// Send e-mail
		dependencyServicesCommunicator.sendWonVoucherInTombolaAssignedEmailToUser(userId, userVoucher,
				voucher, tombolaName, request.getTenantId(), request.getAppId());

		return new UserVoucherAssignResponse(fullUserVoucherId,voucherId);
	}

	private JsonMessageContent onVoucherPurchase(ClientInfo clientInfo, JsonMessage<? extends JsonMessageContent> message) {
		VoucherPurchaseRequest request = (VoucherPurchaseRequest) message.getContent();

		if (clientInfo == null || clientInfo.getUserId() == null) {
			throw new F4MInsufficientRightsException(NO_USER_ID_SPECIFIED);
		}

		Voucher voucher = voucherUtil.getVoucherById(request.getVoucherId());
		if(voucher == null) {
			throw new F4MEntryNotFoundException();
		}

		if (!voucher.isExchange()) {
			throw new F4MVoucherIsNotExchangeException("Voucher has isExchange set to false, buy not allowed");
		}

		voucherUtil.reserveVoucher(request.getVoucherId());

		dependencyServicesCommunicator.initiateUserVoucherPayment(message,
				getSessionWrapper(), request.getVoucherId(),
				BigDecimal.valueOf(voucher.getBonuspointsCosts()),
				Currency.BONUS);

		return null;
	}

	private VoucherListResponse onVoucherList(ClientInfo clientInfo, VoucherListRequest request) {
		final String tenantId = clientInfo != null ? clientInfo.getTenantId() : request.getTenantId();

		if (tenantId == null) {
			throw new F4MInsufficientRightsException(NO_TENANT_ID_SPECIFIED);
		}

        request.validateFilterCriteria(VoucherListRequest.MAX_LIST_LIMIT);

		if (request.getLimit() == 0) {
			return new VoucherListResponse(request.getLimit(), request.getOffset()); // empty response
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Getting vouchers for tenant <{}>", tenantId);
		}
		return voucherUtil.getVouchersByTenantId(request, tenantId);
	}

	private VoucherGetResponse onVoucherGet(VoucherGetRequest request) {

		VoucherResponseModel voucher = voucherUtil.getVoucherResponseObjectById(request.getVoucherId());
		if (voucher == null) {
			throw new F4MEntryNotFoundException();
		}
		return new VoucherGetResponse(voucher);
	}

	private JsonMessageContent onUserVoucherAssign(JsonMessage<? extends JsonMessageContent> message) {
		UserVoucherAssignRequest request = (UserVoucherAssignRequest) message.getContent();
		String voucherId = request.getVoucherId();
		String userId = request.getUserId();
		String userVoucherId = voucherUtil.assignVoucherToUser(voucherId, userId,
				request.getGameInstanceId(), null, request.getReason());
		String fullUserVoucherId = VoucherUtil.createUserVoucherIdForResponse(voucherId, userVoucherId);
		UserVoucher userVoucher = voucherUtil.getUserVoucherById(fullUserVoucherId);
		Voucher voucher = voucherUtil.getVoucherById(voucherId);

		// Send e-mail
		dependencyServicesCommunicator.sendWonVoucherAssignedEmailToUser(userId, userVoucher, voucher,
				request.getTenantId(), request.getAppId());

		return new UserVoucherAssignResponse(fullUserVoucherId,voucherId);
	}

	private UserVoucherListResponse onUserVoucherList(ClientInfo clientInfo, UserVoucherListRequest request) {

		final String userId = getUserId(clientInfo, request);

		if (StringUtils.isBlank(userId)) {
			throw new F4MInsufficientRightsException(NO_USER_ID_SPECIFIED);
		}
        request.validateFilterCriteria(UserVoucherListRequest.MAX_LIST_LIMIT);

		if (request.getLimit() == 0) {
			return new UserVoucherListResponse(0,0); // empty
		}

		return voucherUtil.getUnusedUserVouchersByUserId(userId, request);
	}


	private UserVoucherGetResponse onUserVoucherGet(UserVoucherGetRequest request){
		UserVoucherResponseModel userVoucher = voucherUtil.getUserVoucherResponseModelJsonById(request.getUserVoucherId());
		return new UserVoucherGetResponse(userVoucher);
	}

	private UserVoucherUseResponse onUserVoucherUse(ClientInfo clientInfo, UserVoucherUseRequest request){
		if (clientInfo == null || clientInfo.getUserId() == null) {
			throw new F4MInsufficientRightsException(NO_USER_ID_SPECIFIED);
		}

		voucherUtil.useVoucherForUser(request.getUserVoucherId(), clientInfo.getUserId());

		VoucherUsedEvent voucherUsedEvent = new VoucherUsedEvent();
		voucherUsedEvent.setVoucherId(voucherUtil.getVoucherIdByUserVoucherId(request.getUserVoucherId()));
		voucherUsedEvent.setVoucherInstanceId(request.getUserVoucherId());
		tracker.addEvent(clientInfo, voucherUsedEvent);

		return new UserVoucherUseResponse(request.getUserVoucherId());
	}

	private EmptyJsonMessageContent onVoucherReserve(UserVoucherReserveRequest request){
		voucherUtil.reserveVoucher(request.getVoucherId());
		return new EmptyJsonMessageContent();
	}

	private EmptyJsonMessageContent onVoucherRelease(UserVoucherReleaseRequest request){
		voucherUtil.releaseVoucher(request.getVoucherId());
		return new EmptyJsonMessageContent();
	}

	private UserVoucherListByVoucherResponse onVoucherCodesList(JsonMessage<UserVoucherListByVoucherRequest> message) {
		if (!message.isAuthenticated()) {
			throw new F4MInsufficientRightsException("User has to be authenticated.");
		}
		return voucherUtil.getUserVouchersByVoucherId(message.getContent());
	}

	private UserVoucherListByUserResponse onUserVoucherListByUser(JsonMessage<UserVoucherListByUserRequest> message) {
		if (!message.isAuthenticated()) {
			throw new F4MInsufficientRightsException("User has to be authenticated.");
		}
		UserVoucherListByUserRequest request = message.getContent();
		request.validateFilterCriteria(UserVoucherListByUserRequest.MAX_LIST_LIMIT);
		return voucherUtil.getUserVouchersByUserId(message.getContent());
	}

	private JsonMessageContent onMoveVouchers(MoveVouchersRequest request) {
		voucherUtil.moveVouchers(request.getSourceUserId(), request.getTargetUserId());
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onVoucherListDeleteExpiredEntries(ClientInfo clientInfo, VoucherListDeleteExpiredEntriesRequest request) {
		final String tenantId = clientInfo != null ? clientInfo.getTenantId() : request.getTenantId();

		if (tenantId == null) {
			throw new F4MInsufficientRightsException(NO_TENANT_ID_SPECIFIED);
		}

		voucherUtil.deleteExpiredVoucherListEntries(tenantId);
		return new EmptyJsonMessageContent();
	}

}
