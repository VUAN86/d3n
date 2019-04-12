package de.ascendro.f4m.service.payment.dao;

import javax.inject.Inject;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.payment.manager.PaymentUserIdCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PendingIdentificationAerospikeDaoImpl implements PendingIdentificationAerospikeDao {


    private static final String PENDING_IDENTIFICATION_SET_NAME = "pendingIdent";
    private static final String PENDING_IDENTIFICATION_BIN_NAME = "value";
    private static final String PENDING_IDENTIFICATION_KEY_PREFIX = "pendingIdent" +  PrimaryKeyUtil.KEY_ITEM_SEPARATOR;

    private AerospikeDao aerospikeDao;
    private JsonUtil jsonUtil;

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingIdentificationAerospikeDaoImpl.class);

    @Inject
    public PendingIdentificationAerospikeDaoImpl(JsonUtil jsonUtil, AerospikeDao aerospikeDao) {
        this.jsonUtil = jsonUtil;
        this.aerospikeDao = aerospikeDao;
    }

    @Override
    public PendingIdentification getPendingIdentification(String tenantId, String profileId) {
        String record = aerospikeDao.readJson(PENDING_IDENTIFICATION_SET_NAME, createKey(PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId)), PENDING_IDENTIFICATION_BIN_NAME);
        return jsonUtil.fromJson(record, PendingIdentification.class);
    }

    @Override
    public void deletePendingIdentification(String tenantId, String profileId) {
        aerospikeDao.delete(PENDING_IDENTIFICATION_SET_NAME, createKey(PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId)));
    }

    @Override
    public void createPendingIdentification(PendingIdentification pendingIdentification) {
        String paymentUserId = PaymentUserIdCalculator.calcPaymentUserId(pendingIdentification.getTenantId(), pendingIdentification.getProfileId());
        try {
            aerospikeDao.createJson(PENDING_IDENTIFICATION_SET_NAME, createKey(paymentUserId), PENDING_IDENTIFICATION_BIN_NAME, jsonUtil.toJson(pendingIdentification));
        } catch (Exception e){
            LOGGER.error("CreatePendingIdentification aerospikeDao.createJson error {}", e.getMessage());
        }
    }

    private String createKey(String paymentUserId) {
        return PENDING_IDENTIFICATION_KEY_PREFIX + paymentUserId;
    }
}
