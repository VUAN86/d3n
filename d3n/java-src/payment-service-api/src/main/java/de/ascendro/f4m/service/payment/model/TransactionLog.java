package de.ascendro.f4m.service.payment.model;

import static de.ascendro.f4m.service.util.F4MEnumUtils.getEnum;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;

public class TransactionLog extends JsonObjectWrapper {

	public static final String PROPERTY_ID = "id";
	public static final String PROPERTY_USER_FROM_ID = "userFromId";
	public static final String PROPERTY_TENANT_ID = "tenantId";
	public static final String PROPERTY_APP_ID = "appId";
	public static final String PROPERTY_USER_TO_ID = "userToId";
	public static final String PROPERTY_GAME_ID = "gameId";
	public static final String PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID = "multiplayerGameInstanceId";
	public static final String PROPERTY_GAME_INSTANCE_ID = "gameInstanceId";
	public static final String PROPERTY_WINNING_COMPONENT_ID = "winningComponentId";
	public static final String PROPERTY_USER_WINNING_COMPONENT_ID = "userWinningComponentId";
	public static final String PROPERTY_AMOUNT = "amount";
	/**
	 * Optional property for exchange rate, when converting between currencies.
	 */
	public static final String PROPERTY_RATE = "rate";
	public static final String PROPERTY_CURRENCY = "currency";
	/**
	 * Optional property for to currency, when converting between currencies.
	 */
	public static final String PROPERTY_CURRENCY_TO = "currencyTo";
	public static final String PROPERTY_REASON = "reason";
	public static final String PROPERTY_STATUS = "status";
	public static final String PROPERTY_TRANSACTION_ID = "transactionId";

	/*
	* Used by invoice
	 */
	public static final String PROPERTY_AMOUNT_TO = "amountTo";

	public TransactionLog() {
		// Initialize empty object
	}
	
	public TransactionLog(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	public TransactionLog(String userFromId, String tenantId, String userToId,
						  String gameId, String multiplayerGameInstanceId, String gameInstanceId,
						  BigDecimal amount, Currency currency, String reason, String appId) {
		setUserFromId(userFromId);
		setUserToId(userToId);
		setTenantId(tenantId);
		setAppId(appId);
		setGameId(gameId);
		setMultiplayerGameInstanceId(multiplayerGameInstanceId);
		setGameInstanceId(gameInstanceId);
		setAmount(amount);
		setCurrency(currency);
		setReason(reason);
		setStatus(TransactionStatus.INITIATED);
	}
	
	public TransactionLog(TransferFundsRequest transactionInfo, Currency currency, String reason, String appId){
		setUserFromId(transactionInfo.getFromProfileId());
		setUserToId(transactionInfo.getToProfileId());
		setTenantId(transactionInfo.getTenantId());
		setAppId(appId);
		setAmount(transactionInfo.getAmount());
		setCurrency(currency);
		setReason(reason);
		setStatus(TransactionStatus.INITIATED);
		
		final PaymentDetails paymentDetails = transactionInfo.getPaymentDetails();
		if(paymentDetails != null){
			setGameId(paymentDetails.getGameId());
			setMultiplayerGameInstanceId(paymentDetails.getMultiplayerGameInstanceId());
			setGameInstanceId(paymentDetails.getGameInstanceId());
			setWinningComponentId(paymentDetails.getWinningComponentId());
			setUserWinningComponentId(paymentDetails.getUserWinningComponentId());
		}
	}
	
	public String getUserFromIdOrUserToId() {
		String userId = getUserFromId();
		if (StringUtils.isEmpty(userId)) {
			userId = getUserToId();
		}
		return userId;
	}
	
	public String getId() {
		return getPropertyAsString(PROPERTY_ID);
	}
	
	public void setId(String id) {
		setProperty(PROPERTY_ID, id);
	}

	public String getUserFromId() {
		return getPropertyAsString(PROPERTY_USER_FROM_ID);
	}

	public void setUserFromId(String userFromId) {
		setProperty(PROPERTY_USER_FROM_ID, userFromId);
	}

	public String getUserToId() {
		return getPropertyAsString(PROPERTY_USER_TO_ID);
	}

	public void setUserToId(String userToId) {
		setProperty(PROPERTY_USER_TO_ID, userToId);
	}

	public String getTenantId() {
		return getPropertyAsString(PROPERTY_TENANT_ID);
	}

	public void setTenantId(String tenantId) {
		setProperty(PROPERTY_TENANT_ID, tenantId);
	}

	public String getAppId() {
		return getPropertyAsString(PROPERTY_APP_ID);
	}

	public void setAppId(String appId) {
		setProperty(PROPERTY_APP_ID, appId);
	}

	public String getGameId() {
		return getPropertyAsString(PROPERTY_GAME_ID);
	}

	public void setGameId(String gameId) {
		setProperty(PROPERTY_GAME_ID, gameId);
	}

	public String getMultiplayerGameInstanceId() {
		return getPropertyAsString(PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID);
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		setProperty(PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID, multiplayerGameInstanceId);
	}

	public String getGameInstanceId() {
		return getPropertyAsString(PROPERTY_GAME_INSTANCE_ID);
	}

	public void setGameInstanceId(String gameInstanceId) {
		setProperty(PROPERTY_GAME_INSTANCE_ID, gameInstanceId);
	}

	public String getWinningComponentId() {
		return getPropertyAsString(PROPERTY_WINNING_COMPONENT_ID);
	}

	public void setWinningComponentId(String winningComponentId) {
		setProperty(PROPERTY_WINNING_COMPONENT_ID, winningComponentId);
	}

	public String getUserWinningComponentId() {
		return getPropertyAsString(PROPERTY_USER_WINNING_COMPONENT_ID);
	}

	public void setUserWinningComponentId(String userWinningComponentId) {
		setProperty(PROPERTY_USER_WINNING_COMPONENT_ID, userWinningComponentId);
	}

	public BigDecimal getAmount() {
		return getPropertyAsBigDecimal(PROPERTY_AMOUNT);
	}

	public void setAmount(BigDecimal amount) {
		setProperty(PROPERTY_AMOUNT, amount);
	}

	public BigDecimal getRate() {
		return getPropertyAsBigDecimal(PROPERTY_RATE);
	}

	public void setRate(BigDecimal rate) {
		setProperty(PROPERTY_RATE, rate);
	}

	public Currency getCurrency() {
		return getEnum(Currency.class, getPropertyAsString(PROPERTY_CURRENCY));
	}

	public void setCurrency(Currency currency) {
		setProperty(PROPERTY_CURRENCY, currency == null ? null : currency.name());
	}

	public Currency getCurrencyTo() {
		return getEnum(Currency.class, getPropertyAsString(PROPERTY_CURRENCY_TO));
	}

	public void setCurrencyTo(Currency currencyTo) {
		setProperty(PROPERTY_CURRENCY_TO, currencyTo == null ? null : currencyTo.name());
	}

	public String getReason() {
		return getPropertyAsString(PROPERTY_REASON);
	}

	public void setReason(String reason) {
		setProperty(PROPERTY_REASON, reason);
	}

	public TransactionStatus getStatus() {
		return getEnum(TransactionStatus.class, getPropertyAsString(PROPERTY_STATUS));
	}
	
	public void setStatus(TransactionStatus status) {
		setProperty(PROPERTY_STATUS, status == null ? null : status.name());
	}
	
	public String getTransactionId() {
		return getPropertyAsString(PROPERTY_TRANSACTION_ID);
	}

	public void setTransactionId(String transactionId) {
		setProperty(PROPERTY_TRANSACTION_ID, transactionId);
	}

	public BigDecimal getAmountTo() {
		return getPropertyAsBigDecimal(PROPERTY_AMOUNT_TO);
	}

	public void setAmountTo(BigDecimal amountTo) {
		setProperty(PROPERTY_AMOUNT_TO, amountTo);
	}
}
