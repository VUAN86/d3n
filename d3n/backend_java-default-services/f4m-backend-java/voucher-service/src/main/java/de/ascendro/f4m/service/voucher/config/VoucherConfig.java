package de.ascendro.f4m.service.voucher.config;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class VoucherConfig extends F4MConfigImpl {


	public static final String AEROSPIKE_VOUCHER_LIST_SET = "voucherList.aerospike.set";
	public static final String AEROSPIKE_USER_VOUCHER_SET = "userVoucher.aerospike.set";

	public VoucherConfig() {
		super(new AerospikeConfigImpl());

		setProperty(AEROSPIKE_VOUCHER_LIST_SET, "voucherList");
		setProperty(AEROSPIKE_USER_VOUCHER_SET, "userVoucher");
		loadProperties();
	}
}
