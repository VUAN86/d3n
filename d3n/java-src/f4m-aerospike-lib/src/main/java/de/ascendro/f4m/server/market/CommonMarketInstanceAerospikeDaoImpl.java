package de.ascendro.f4m.server.market;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.market.util.MarketPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;

import javax.inject.Inject;

public class CommonMarketInstanceAerospikeDaoImpl extends AerospikeDaoImpl<MarketPrimaryKeyUtil> implements CommonMarketInstanceAerospikeDao  {

    private static final String AEROSPIKE_MULTIPLAYER_INDEX_SET = "market";
    private static final String CLIENT_ID = "clientId";
    private static final String ORDER_ID = "orderId";
    private static final String USER_ID = "userId";
    private static final String DATES = "date";
    private static final String USER = "userError";
    private static final String DATE = "dateError";
    private static final String PACKAGE_NAME = "packageName";
    private static final String PRODUCT_ID = "productId";
    private static final String PURCHASE_TIME= "purchaseTime";
    private static final String PURCHASE_STATE = "purchaseState";
    private static final String PURCHASE_TOKEN = "purchaseToken";
    private static final String TIMESTAMP = "timestamp";
    private static final String RECEIPT = "receipt";
    private static final String TENANT_ID = "tenantId";
    private static final String APP_ID = "appId";
    private static final String TRANSACTION_ID = "transactionId";
    private static final String DATE_TIME = "dateTime";


    private static final String ERROR = "Errors";

    @Inject
    public CommonMarketInstanceAerospikeDaoImpl(Config config, MarketPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil, AerospikeClientProvider aerospikeClientProvider) {
        super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
    }

    @Override
    public void createPurchaseCreditInstanceAndroid(String className, String orderId, String userId, String tenantId, String appId, String date,
                                                    String packageName, String productId, String purchaseTime, String purchaseState, String purchaseToken) {
        final String metaKey = primaryKeyUtil.createMetaKey(className, orderId);
        createRecord(getIndexSet(), metaKey,
                getStringBin(USER_ID, userId),
                getStringBin(TENANT_ID, tenantId),
                getStringBin(APP_ID, appId),
                getStringBin(ORDER_ID, orderId),
                getStringBin(DATES, date),
                getStringBin(PACKAGE_NAME, packageName),
                getStringBin(PRODUCT_ID, productId),
                getStringBin(PURCHASE_TIME, purchaseTime),
                getStringBin(PURCHASE_STATE, purchaseState),
                getStringBin(PURCHASE_TOKEN, purchaseToken));
    }

    public void createPurchaseCreditInstanceIos(String className, String transactionId, String userId, String tenantId, String appId, String date, String productId, String dateTime, String receipt) {
        final String metaKey = primaryKeyUtil.createMetaKey(className, transactionId);
        createRecord(getIndexSet(), metaKey,
                getStringBin(USER_ID, userId),
                getStringBin(TENANT_ID, tenantId),
                getStringBin(APP_ID, appId),
                getStringBin(TRANSACTION_ID, transactionId),
                getStringBin(DATES, date),
                getStringBin(PRODUCT_ID, productId),
                getStringBin(DATE_TIME, dateTime),
                getStringBin(RECEIPT, receipt));
    }

    @Override
    public Boolean isPurchaseCreditInstance(String className, String id) {
        final String metaKey = primaryKeyUtil.createMetaKey(className, id);
        try {
            return readString(getIndexSet(), metaKey, USER_ID) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public void createPurchaseCreditInstanceError(String className, String userId,String tenantId, String appId, String receipt, String date, Long timestamp, String error) {
        final String metaKey = primaryKeyUtil.createMetaKey(className, userId);
        createRecord(getIndexSetError(), metaKey,
                getStringBin(USER, userId),
                getStringBin(TENANT_ID, tenantId),
                getStringBin(APP_ID, appId),
                getLongBin(TIMESTAMP, timestamp),
                getStringBin(DATE, date),
                getStringBin(RECEIPT, receipt),
                getStringBin(ERROR, error));
    }


    private String getIndexSet() {
        return AEROSPIKE_MULTIPLAYER_INDEX_SET;
    }

    private String getIndexSetError() {
        return AEROSPIKE_MULTIPLAYER_INDEX_SET + ERROR;
    }
}
