package de.ascendro.f4m.service.voucher.dao;

import java.util.List;

import de.ascendro.f4m.server.voucher.CommonVoucherAerospikeDao;
import de.ascendro.f4m.service.voucher.model.UserVoucher;
import de.ascendro.f4m.service.voucher.model.UserVoucherType;
import de.ascendro.f4m.service.voucher.model.Voucher;

/**
 * Voucher Data Access Object interface.
 */
public interface VoucherAerospikeDao extends CommonVoucherAerospikeDao {

    /**
     * Get User Voucher.
     *
     * @param voucherId the voucher Id
     * @param userVoucherId the user voucher Id
     * @return the User Voucher
     */
    UserVoucher getUserVoucher(String voucherId, String userVoucherId);

    /**
     * Update voucher user ID to target user ID.
     */
    void updateVoucherUser(String voucherId, String userVoucherId, String targetUserId);

    /**
	 * Assign a UserVoucher to a user
	 *
	 * @param voucherId the Voucher ID for which to assign a UserVoucher
	 * @param clientInfo the Client info to assign
	 * @param gameInstanceId GameInstance ID to reference in assigned UserVoucher
	 * @param tombolaId Tombola ID to reference in assigned UserVoucher
	 * @return the ID of the assigned UserVoucher
	 */
	String assignVoucherToUser(String voucherId, String userId, String gameInstanceId, String tombolaId, String reason);

	/**
	 * Returns total number of UserVouchers based on Voucher ID and type
	 * @param voucherId the Voucher ID
	 * @param type type of User Voucher
	 * @return total number of UserVouchers
	 */
	long getTotalUserVouchersCountByType(String voucherId, UserVoucherType type);

	/**
	 * Get a list of UserVouchers based on Voucher ID and type
	 *
	 * @param limit pagination related limit
	 * @param offset pagination related offset
	 * @param voucherId the Voucher ID
	 * @param type type of User Voucher
	 * @return List of UserVouchers
	 */
	List<UserVoucher> getUserVoucherListByVoucherId(int limit, long offset, String voucherId, UserVoucherType type);

    List<Voucher> getVouchersByTenantId(int limit, long offset, String tenantId);

    UserVoucher getUserVoucherById(String userVoucherId);

    Voucher getVoucherByUserVoucherId(String userVoucherId);

    /**
     * Marks the user voucher as used
     * @param userVoucherId Full user voucher ID, in the format 'voucherId:instanceId'
	 * @param userId The ID of the user making the request
     */
    void useVoucherForUser(String userVoucherId, String userId);

    /**
     * Reserves a voucher
     *
     * @param voucherId the Voucher ID
     */
	void reserveVoucher(String voucherId);

    /**
     * Release a voucher
     *
     * @param voucherId the Voucher ID
     */
    void releaseVoucher(String voucherId);

	/**
	 * Deletes the expired vouchers from the tenants' voucher list
	 *
	 * @param tenantId
	 */
	void removeExpiredVouchersFromVoucherList(String tenantId);
    
}
