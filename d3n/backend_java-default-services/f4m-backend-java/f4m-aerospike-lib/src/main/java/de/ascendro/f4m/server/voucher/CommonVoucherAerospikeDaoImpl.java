package de.ascendro.f4m.server.voucher;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.voucher.util.VoucherPrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.voucher.model.Voucher;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

public class CommonVoucherAerospikeDaoImpl extends AerospikeOperateDaoImpl<VoucherPrimaryKeyUtil> implements CommonVoucherAerospikeDao {

    @Inject
    public CommonVoucherAerospikeDaoImpl(Config config, VoucherPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil, AerospikeClientProvider aerospikeClientProvider) {
        super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
    }

    @Override
    public Voucher getVoucherById(String voucherId) {
        final String voucherPrimaryKey = primaryKeyUtil.createPrimaryKey(voucherId);
        final String voucherJson = readJson(getSet(), voucherPrimaryKey, VOUCHER_BIN_NAME);
        return StringUtils.isNotBlank(voucherJson) ? jsonUtil.fromJson(voucherJson, Voucher.class) : null;
    }

    @Override
    public void cleanTombolaVouchers(String voucherId){
        final String voucherPrimaryKey = primaryKeyUtil.createTombolaVoucherKey(voucherId);
        delete(getUserVoucherSet(),voucherPrimaryKey);
    }

    private String getUserVoucherSet() {
        return config.getProperty(AerospikeConfigImpl.AEROSPIKE_USER_VOUCHER_SET);
    }

    protected String getSet() {
        return config.getProperty(AerospikeConfigImpl.AEROSPIKE_VOUCHER_SET);
    }

}
