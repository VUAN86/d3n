package de.ascendro.f4m.server.voucher;

import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.service.voucher.model.Voucher;

public interface CommonVoucherAerospikeDao extends AerospikeOperateDao {

    String VOUCHER_BIN_NAME = "voucher";

    /**
     * Get Voucher Voucher By Id.
     *
     * @param voucherId the voucher ID
     * @return Voucher
     */
    Voucher getVoucherById(String voucherId);

    void cleanTombolaVouchers(String voucherId);
}
