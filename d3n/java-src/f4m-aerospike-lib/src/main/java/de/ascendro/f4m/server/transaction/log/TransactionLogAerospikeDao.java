package de.ascendro.f4m.server.transaction.log;

import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransactionStatus;

import java.util.List;

public interface TransactionLogAerospikeDao {

	public static final String BIN_NAME_USER_FROM_ID = "userFromId";
	public static final String BIN_NAME_TENANT_ID = "fromTenantId";
	public static final String BIN_NAME_APP_ID = "appId";
	public static final String BIN_NAME_USER_TO_ID = "userToId";
	public static final String BIN_NAME_GAME_ID = "gameId";
	public static final String BIN_NAME_MULTIPLAYER_GAME_INSTANCE_ID = "mgiId";
	public static final String BIN_NAME_GAME_INSTANCE_ID = "gameInstanceId";
	public static final String BIN_NAME_AMOUNT = "amount";
	public static final String BIN_NAME_RATE = "rate";
	public static final String BIN_NAME_CURRENCY = "currency";
	public static final String BIN_NAME_CURRENCY_TO = "currencyTo";
	public static final String BIN_NAME_REASON = "reason";
	public static final String BIN_NAME_STATUS = "status";
	public static final String BIN_NAME_TRANSACTION_ID = "transactionId";
	public static final String BIN_NAME_TRANSACTION_LIST = "transactions";
	/**
	 * Store a transaction log entry.
	 */
	public String createTransactionLog(TransactionLog transactionLog);

    public String createTransactionLogPromo(TransactionLog transactionLog);
	/**
	 * Update transaction status.
	 */
	public void updateTransactionLog(String id, String transactionId, TransactionStatus status);
	
	public TransactionLog getTransactionLog(String id);

	public List<TransactionLog> getTransactionLogsByGameInstance(String gameInstanceId);

}
