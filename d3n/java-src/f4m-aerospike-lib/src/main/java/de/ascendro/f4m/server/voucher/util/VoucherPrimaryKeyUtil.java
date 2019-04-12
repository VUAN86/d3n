package de.ascendro.f4m.server.voucher.util;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;

import javax.inject.Inject;

/**
 * Voucher service aerospike key construct helper
 *
 */
public class VoucherPrimaryKeyUtil extends PrimaryKeyUtil<String> {


	public static final String VOUCHER_LIST_KEY_PREFIX = "tenantId";
	public static final String TOMBOLA_KEY_PREFIX = "tombola";
	public static final String RESERVED_KEY_PREFIX = "reserved";
	@Inject
	public VoucherPrimaryKeyUtil(Config config) {
		super(config);
	}

	/**
	 * Create voucher key by id.
	 * 
	 * @param voucherId
	 * @return key
	 */
	@Override
	public String createPrimaryKey(String voucherId) {
		return VoucherMessageTypes.SERVICE_NAME + KEY_ITEM_SEPARATOR + voucherId;
	}

	public String createTombolaVoucherKey(String voucherId) {
		return VoucherMessageTypes.SERVICE_NAME + KEY_ITEM_SEPARATOR + TOMBOLA_KEY_PREFIX + KEY_ITEM_SEPARATOR +RESERVED_KEY_PREFIX + KEY_ITEM_SEPARATOR + voucherId;
	}

	/**
	 * Create voucher sub-record key based on voucher id and sub-record name.
	 *
	 * @param voucherId
	 *            - profile user id
	 * @param subRecordName
	 *            - sub-record name
	 * @return sub-record key
	 */
	public String createSubRecordKeyByVoucherId(String voucherId, String subRecordName) {
		final String baseRecordKey = createPrimaryKey(voucherId);
		return createSubRecordPrimaryKey(baseRecordKey, subRecordName);
	}

	/**
	 * Create userVoucher sub-record key based on userVoucherId retrieved from API, which is in the form voucherId:instanceId
	 *
	 * @param userVoucherId
	 *            - user voucher ID from API, in the form voucherId:instanceId
	 * @return userVoucher sub-record key
	 */
	public String createUserVoucherKey(String userVoucherId) {
		return VoucherMessageTypes.SERVICE_NAME + KEY_ITEM_SEPARATOR + userVoucherId;
	}

	public String createVoucherKeyFromUserVoucherKey(String userVoucherId) {
		return VoucherMessageTypes.SERVICE_NAME + KEY_ITEM_SEPARATOR + userVoucherId.substring(0, userVoucherId.indexOf(KEY_ITEM_SEPARATOR));
	}

	public String createVoucherListPrimaryKey(String tenantId) {
		return VOUCHER_LIST_KEY_PREFIX + KEY_ITEM_SEPARATOR + tenantId;
	}
}
