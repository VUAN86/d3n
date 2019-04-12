package de.ascendro.f4m.service.payment.model;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;
import de.ascendro.f4m.service.payment.model.internal.TransferBetweenAccountsRequest;
import de.ascendro.f4m.service.payment.model.internal.TransferJackpotRequest;

public class TransferFundsRequestBuilder {
	private String tenantId;
	private String fromProfileId;
	private String toProfileId;
	private BigDecimal amount;
	private Currency currency;
	private PaymentDetails paymentDetails;
	private String multiplayerGameInstanceId;
	
	public TransferFundsRequestBuilder fromProfileToTenant(String fromProfileId, String tenantId) {
		this.fromProfileId = fromProfileId;
		this.tenantId = tenantId;
		return this;
	}

	public TransferFundsRequestBuilder fromTenantToProfile(String tenantId, String toProfileId) {
		this.tenantId = tenantId;
		this.toProfileId = toProfileId;
		return this;
	}

	public TransferFundsRequestBuilder amount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public TransferFundsRequestBuilder amount(BigDecimal amount, Currency currency) {
		this.amount = amount;
		this.currency = currency;
		return this;
	}

	public TransferFundsRequestBuilder currency(Currency currency) {
		this.currency = currency;
		return this;
	}

	public TransferFundsRequestBuilder withPaymentDetails(PaymentDetails paymentDetails) {
		this.paymentDetails = paymentDetails;
		return this;
	}

	public TransferFundsRequestBuilder withMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
		return this;
	}
	

	public PaymentMessageTypes getBuildJackpotBuyInRequestType() {
		return PaymentMessageTypes.TRANSFER_JACKPOT;
	}

	public PaymentMessageTypes getBuildSingleUserPaymentForGameRequestType() {
		if (Currency.MONEY != currency) {
			return PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE;
		} else {
			return PaymentMessageTypes.TRANSFER_BETWEEN_ACCOUNTS;			
		}
	}

	public TransferJackpotRequest buildJackpotBuyInRequest() {
		TransferJackpotRequest request = new TransferJackpotRequest();
		request.setTenantId(tenantId);
		request.setFromProfileId(fromProfileId);
		request.setMultiplayerGameInstanceId(multiplayerGameInstanceId);
		request.setAmount(amount);
		request.setPaymentDetails(paymentDetails);
		return request;
	}
	
	public TransferFundsRequest buildSingleUserPaymentForGame() {
		if (Currency.MONEY != currency) {
			LoadOrWithdrawWithoutCoverageRequest request = new LoadOrWithdrawWithoutCoverageRequest();
			request.setTenantId(tenantId);
			if (StringUtils.isNotEmpty(fromProfileId)) {
				request.setProfileId(fromProfileId);
				request.setAmount(amount.negate()); //should be negative amount to withdraw from profile
			} else {
				request.setProfileId(toProfileId);
				request.setAmount(amount);
			}
			request.setCurrency(currency);
			request.setPaymentDetails(paymentDetails);
			return request;
		} else {
			return buildPaymentBetweenUsers();
		}
	}

	public TransferBetweenAccountsRequest buildPaymentBetweenUsers() {
		TransferBetweenAccountsRequest request = new TransferBetweenAccountsRequest();
		request.setTenantId(tenantId);
		request.setFromProfileId(fromProfileId);
		request.setToProfileId(toProfileId);
		request.setAmount(amount);
		request.setCurrency(currency);
		request.setPaymentDetails(paymentDetails);
		return request;
	}
}
