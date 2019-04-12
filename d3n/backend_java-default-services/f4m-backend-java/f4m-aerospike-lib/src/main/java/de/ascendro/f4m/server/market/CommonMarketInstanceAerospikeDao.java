package de.ascendro.f4m.server.market;

/**
 *  Recording to the database of operations with goods in Google in the Apple shop
 */

public interface CommonMarketInstanceAerospikeDao {

    void createPurchaseCreditInstanceAndroid(String className, String orderId, String userId, String tenantId, String appId, String date, String packageName, String productId, String purchaseTime, String purchaseState, String purchaseToken);

    void createPurchaseCreditInstanceIos(String className, String transactionId, String userId, String tenantId, String appId, String date, String productId, String dateTime, String receipt);

    Boolean isPurchaseCreditInstance(String className, String id);

    void createPurchaseCreditInstanceError(String className, String userId,String tenantId, String appId, String receipt, String date, Long timestamp, String error);
}
