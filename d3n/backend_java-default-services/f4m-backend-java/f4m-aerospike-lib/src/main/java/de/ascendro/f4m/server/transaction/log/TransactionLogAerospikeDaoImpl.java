package de.ascendro.f4m.server.transaction.log;

import com.aerospike.client.Bin;
import com.aerospike.client.Record;
import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.transaction.log.util.TransactionLogPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.util.F4MEnumUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TransactionLogAerospikeDaoImpl extends AerospikeDaoImpl<TransactionLogPrimaryKeyUtil> implements TransactionLogAerospikeDao {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionLogAerospikeDaoImpl.class);


	@Inject
	public TransactionLogAerospikeDaoImpl(Config config, TransactionLogPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public String createTransactionLog(TransactionLog log) {
		Validate.notNull(log);
		
		final String id = primaryKeyUtil.generateId();
		log.setId(id);

		final String key = primaryKeyUtil.createPrimaryKey(id);
		createRecord(getSet(), key,
				new Bin(BIN_NAME_USER_FROM_ID, log.getUserFromId()),
				new Bin(BIN_NAME_TENANT_ID, log.getTenantId()),
				new Bin(BIN_NAME_APP_ID,log.getAppId()),
				new Bin(BIN_NAME_USER_TO_ID, log.getUserToId()),
				new Bin(BIN_NAME_GAME_ID, log.getGameId()),
				new Bin(BIN_NAME_MULTIPLAYER_GAME_INSTANCE_ID, log.getMultiplayerGameInstanceId()),
				new Bin(BIN_NAME_GAME_INSTANCE_ID, log.getGameInstanceId()),
				new Bin(BIN_NAME_AMOUNT, bigDecimalToString(log.getAmount())),
				new Bin(BIN_NAME_RATE, bigDecimalToString(log.getRate())),
				new Bin(BIN_NAME_CURRENCY, enumToString(log.getCurrency())),
				new Bin(BIN_NAME_CURRENCY_TO, enumToString(log.getCurrencyTo())),
				new Bin(BIN_NAME_REASON, log.getReason()),
				new Bin(BIN_NAME_STATUS, enumToString(log.getStatus())));
		if (log.getGameInstanceId() != null) {
			List<String> transactionsList = getTransactionList(log.getGameInstanceId());
			final String gameInstanceIdKey = primaryKeyUtil.createPrimaryKey(log.getGameInstanceId());
			transactionsList.add(id);
			this.<String> createOrUpdateList(getSet(), gameInstanceIdKey, BIN_NAME_TRANSACTION_LIST,
					(readResult, writePolicy) -> transactionsList);
		}


		return id;
	}

	@Override
    public String createTransactionLogPromo(TransactionLog transactionLog) {
        return null;
    }
    public List<String> getTransactionList(String gameInstanceId){
        final String key = primaryKeyUtil.createPrimaryKey(gameInstanceId);
        final List<String> results = readList(getSet(), key, BIN_NAME_TRANSACTION_LIST);
        List<String> transactionsList;
        if (results!=null) {
            transactionsList = results;
        } else {
            transactionsList = new ArrayList<>();
        }
        return transactionsList;
    }
	
	protected String bigDecimalToString(BigDecimal decimal) {
		String result = null;
		if (decimal != null) {
			result = decimal.toPlainString();
		}
		return result;
	}

	protected BigDecimal stringToBigDecimal(String strDecimal) {
		BigDecimal result = null;
		if (strDecimal != null) {
			result = new BigDecimal(strDecimal);
		}
		return result;
	}

	protected String enumToString(Enum<?> enumeration) {
		return enumeration == null ? null : enumeration.name();
	}

	@Override
	public void updateTransactionLog(String id, String transactionId, TransactionStatus status) {
		final String key = primaryKeyUtil.createPrimaryKey(id);
		LOGGER.debug("initiateExternalPayment key {}   getSet {}   ", key, getSet());
		//FIXME: perform update as atomic operation for both bins
		updateString(getSet(), key, BIN_NAME_STATUS, (readValue, writePolicy) -> status == null ? null : status.name());
		updateString(getSet(), key, BIN_NAME_TRANSACTION_ID, (readValue, writePolicy) -> transactionId);
	}

	protected String getSet() {
		return config.getProperty(AerospikeConfigImpl.TRANSACTION_LOG_SET);
	}

	@Override
	public TransactionLog getTransactionLog(String id) {
		final String key = primaryKeyUtil.createPrimaryKey(id);
		Record transactionRecord = readRecord(getSet(), key);
		TransactionLog transactionLog = new TransactionLog();
		transactionLog.setTransactionId(transactionRecord.getString(BIN_NAME_TRANSACTION_ID));
		transactionLog.setUserFromId(transactionRecord.getString(BIN_NAME_USER_FROM_ID));
		transactionLog.setTenantId(transactionRecord.getString(BIN_NAME_TENANT_ID));
		transactionLog.setAppId(transactionRecord.getString(BIN_NAME_APP_ID));
		transactionLog.setUserToId(transactionRecord.getString(BIN_NAME_USER_TO_ID));
		transactionLog.setGameId(transactionRecord.getString(BIN_NAME_GAME_ID));
		transactionLog.setMultiplayerGameInstanceId(transactionRecord.getString(BIN_NAME_MULTIPLAYER_GAME_INSTANCE_ID));
		transactionLog.setGameInstanceId(transactionRecord.getString(BIN_NAME_GAME_INSTANCE_ID));
		transactionLog.setAmount(stringToBigDecimal(transactionRecord.getString(BIN_NAME_AMOUNT)));
		transactionLog.setRate(stringToBigDecimal(transactionRecord.getString(BIN_NAME_RATE)));
		transactionLog.setCurrency(F4MEnumUtils.getEnum(Currency.class, transactionRecord.getString(BIN_NAME_CURRENCY)));
		transactionLog.setCurrencyTo(F4MEnumUtils.getEnum(Currency.class, transactionRecord.getString(BIN_NAME_CURRENCY_TO)));
		transactionLog.setReason(transactionRecord.getString(BIN_NAME_REASON));
		transactionLog.setStatus(F4MEnumUtils.getEnum(TransactionStatus.class, transactionRecord.getString(BIN_NAME_STATUS)));
		return transactionLog;
	}


	@Override
	public List<TransactionLog> getTransactionLogsByGameInstance(String gameInstanceId) {
		List<TransactionLog> transactions = new ArrayList<>();
		for (String transaction : getTransactionList(gameInstanceId)) {
			TransactionLog transactionLog = getTransactionLog(transaction);
			transactions.add(transactionLog);
		}
		return transactions;
	}

}
