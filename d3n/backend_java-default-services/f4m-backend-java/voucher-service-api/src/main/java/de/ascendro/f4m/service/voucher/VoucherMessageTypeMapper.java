package de.ascendro.f4m.service.voucher;

import com.google.common.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.voucher.model.MoveVouchersRequest;
import de.ascendro.f4m.service.voucher.model.VoucherListDeleteExpiredEntriesRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignForTombolaRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignForTombolaResponse;
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

@SuppressWarnings("serial")
public class VoucherMessageTypeMapper extends JsonMessageTypeMapImpl {

	public VoucherMessageTypeMapper() {
		init();
	}

	protected void init() {
		this.register(VoucherMessageTypes.VOUCHER_GET.getMessageName(),
                new TypeToken<VoucherGetRequest>() {}.getType());
		this.register(VoucherMessageTypes.VOUCHER_GET_RESPONSE.getMessageName(),
                new TypeToken<VoucherGetResponse>() {}.getType());

		// Voucher list :
		this.register(VoucherMessageTypes.VOUCHER_LIST.getMessageName(),
                new TypeToken<VoucherListRequest>() {}.getType());
		this.register(VoucherMessageTypes.VOUCHER_LIST_RESPONSE.getMessageName(),
                new TypeToken<VoucherListResponse>() {}.getType());

        // Voucher purchase :
        this.register(VoucherMessageTypes.VOUCHER_PURCHASE.getMessageName(),
                new TypeToken<VoucherPurchaseRequest>() {}.getType());
        this.register(VoucherMessageTypes.VOUCHER_PURCHASE_RESPONSE.getMessageName(),
                new TypeToken<EmptyJsonMessageContent>() {}.getType());

        //Assign user voucher
        this.register(VoucherMessageTypes.USER_VOUCHER_ASSIGN.getMessageName(),
                new TypeToken<UserVoucherAssignRequest>() {}.getType());
        this.register(VoucherMessageTypes.USER_VOUCHER_ASSIGN_RESPONSE.getMessageName(),
                new TypeToken<UserVoucherAssignResponse>() {}.getType());

        //User voucher list
        this.register(VoucherMessageTypes.USER_VOUCHER_LIST.getMessageName(),
                new TypeToken<UserVoucherListRequest>() {}.getType());
        this.register(VoucherMessageTypes.USER_VOUCHER_LIST_RESPONSE.getMessageName(),
                new TypeToken<UserVoucherListResponse>() {}.getType());

        //Get user voucher
        this.register(VoucherMessageTypes.USER_VOUCHER_GET.getMessageName(),
                new TypeToken<UserVoucherGetRequest>() {}.getType());
        this.register(VoucherMessageTypes.USER_VOUCHER_GET_RESPONSE.getMessageName(),
                new TypeToken<UserVoucherGetResponse>() {}.getType());

        //use user voucher
        this.register(VoucherMessageTypes.USER_VOUCHER_USE.getMessageName(),
                new TypeToken<UserVoucherUseRequest>() {}.getType());
        this.register(VoucherMessageTypes.USER_VOUCHER_USE_RESPONSE.getMessageName(),
                new TypeToken<UserVoucherUseResponse>() {}.getType());

        //voucher reserve
        this.register(VoucherMessageTypes.USER_VOUCHER_RESERVE.getMessageName(),
                new TypeToken<UserVoucherReserveRequest>() {}.getType());
        this.register(VoucherMessageTypes.USER_VOUCHER_RESERVE_RESPONSE.getMessageName(),
                new TypeToken<EmptyJsonMessageContent>() {}.getType());

        // Reserve and assign user voucher
        this.register(VoucherMessageTypes.USER_VOUCHER_ASSIGN_FOR_TOMBOLA.getMessageName(),
                new TypeToken<UserVoucherAssignForTombolaRequest>() {}.getType());
        this.register(VoucherMessageTypes.USER_VOUCHER_ASSIGN_FOR_TOMBOLA_RESPONSE.getMessageName(),
                new TypeToken<UserVoucherAssignForTombolaResponse>() {}.getType());

        //voucher relase
        this.register(VoucherMessageTypes.USER_VOUCHER_RELEASE.getMessageName(),
                new TypeToken<UserVoucherReleaseRequest>() {}.getType());
        this.register(VoucherMessageTypes.USER_VOUCHER_RELEASE_RESPONSE.getMessageName(),
                new TypeToken<EmptyJsonMessageContent>() {}.getType());

        // Voucher codes list :
		this.register(VoucherMessageTypes.USER_VOUCHER_LIST_BY_VOUCHER.getMessageName(),
                new TypeToken<UserVoucherListByVoucherRequest>() {}.getType());
		this.register(VoucherMessageTypes.USER_VOUCHER_LIST_BY_VOUCHER_RESPONSE.getMessageName(),
                new TypeToken<UserVoucherListByVoucherResponse>() {}.getType());

        // user Voucher by user :
		this.register(VoucherMessageTypes.USER_VOUCHER_LIST_BY_USER.getMessageName(),
                new TypeToken<UserVoucherListByUserRequest>() {}.getType());
		this.register(VoucherMessageTypes.USER_VOUCHER_LIST_BY_USER_RESPONSE.getMessageName(),
                new TypeToken<UserVoucherListByUserResponse>() {}.getType());
		
		// Move vouchers
		this.register(VoucherMessageTypes.MOVE_VOUCHERS.getMessageName(),
                new TypeToken<MoveVouchersRequest>() {}.getType());
		this.register(VoucherMessageTypes.MOVE_VOUCHERS_RESPONSE.getMessageName(),
                new TypeToken<EmptyJsonMessageContent>() {}.getType());

		// Voucher list delete expired entries
        this.register(VoucherMessageTypes.VOUCHER_LIST_DELETE_EXPIRED_ENTRIES.getMessageName(),
                new TypeToken<VoucherListDeleteExpiredEntriesRequest>() {}.getType());
        this.register(VoucherMessageTypes.VOUCHER_LIST_DELETE_EXPIRED_ENTRIES_RESPONSE.getMessageName(),
                new TypeToken<EmptyJsonMessageContent>() {}.getType());
	}
}
