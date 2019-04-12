package de.ascendro.f4m.service.payment;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.payment.model.config.MergeUsersRequest;
import de.ascendro.f4m.service.payment.model.external.*;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotResponse;
import de.ascendro.f4m.service.payment.model.internal.CreateJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetAccountHistoryRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountHistoryResponse;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotResponse;
import de.ascendro.f4m.service.payment.model.internal.GetTransactionRequest;
import de.ascendro.f4m.service.payment.model.internal.GetTransactionResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountBalanceRequest;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountHistoryRequest;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountHistoryResponse;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.TransferBetweenAccountsRequest;
import de.ascendro.f4m.service.payment.model.internal.TransferJackpotRequest;

public class PaymentMessageTypeMapper extends JsonMessageTypeMapImpl {

	private static final long serialVersionUID = -2612882596176213982L;

	public PaymentMessageTypeMapper() {
        init();
    }

    protected void init() {
        this.register(PaymentMessageTypes.INIT_EXTERNAL_PAYMENT, new TypeToken<InitExternalPaymentRequest>() {});
        this.register(PaymentMessageTypes.INIT_EXTERNAL_PAYMENT_RESPONSE, new TypeToken<InitExternalPaymentResponse>() {});
        this.register(PaymentMessageTypes.GET_EXTERNAL_PAYMENT, new TypeToken<GetExternalPaymentRequest>() {});
        this.register(PaymentMessageTypes.GET_EXTERNAL_PAYMENT_RESPONSE, new TypeToken<GetExternalPaymentResponse>() {});
        this.register(PaymentMessageTypes.INIT_IDENTIFICATION, new TypeToken<InitIdentificationRequest>() {});
        this.register(PaymentMessageTypes.INIT_IDENTIFICATION_RESPONSE, new TypeToken<InitIdentificationResponse>() {});
        this.register(PaymentMessageTypes.INSERT_OR_UPDATE_USER, new TypeToken<InsertOrUpdateUserRequest>() {});
        this.register(PaymentMessageTypes.INSERT_OR_UPDATE_USER_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
        this.register(PaymentMessageTypes.MERGE_USERS, new TypeToken<MergeUsersRequest>() {});
        this.register(PaymentMessageTypes.MERGE_USERS_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
        this.register(PaymentMessageTypes.CONVERT_BETWEEN_CURRENCIES, new TypeToken<ConvertBetweenCurrenciesUserRequest>() {});
        this.register(PaymentMessageTypes.CONVERT_BETWEEN_CURRENCIES_RESPONSE, new TypeToken<ConvertBetweenCurrenciesUserResponse>() {});
        this.register(PaymentMessageTypes.GET_ACCOUNT_BALANCE, new TypeToken<GetAccountBalanceRequest>() {});
        this.register(PaymentMessageTypes.GET_ACCOUNT_BALANCE_RESPONSE, new TypeToken<GetAccountBalanceResponse>() {});
        this.register(PaymentMessageTypes.GET_USER_ACCOUNT_BALANCES, new TypeToken<GetUserAccountBalanceRequest>() {});
        this.register(PaymentMessageTypes.GET_USER_ACCOUNT_BALANCES_RESPONSE, new TypeToken<GetUserAccountBalanceResponse>() {});
        this.register(PaymentMessageTypes.GET_ACCOUNT_HISTORY, new TypeToken<GetAccountHistoryRequest>() {});
        this.register(PaymentMessageTypes.GET_ACCOUNT_HISTORY_RESPONSE, new TypeToken<GetAccountHistoryResponse>() {});
        this.register(PaymentMessageTypes.GET_USER_ACCOUNT_HISTORY, new TypeToken<GetUserAccountHistoryRequest>() {});
        this.register(PaymentMessageTypes.GET_USER_ACCOUNT_HISTORY_RESPONSE, new TypeToken<GetUserAccountHistoryResponse>() {});
        this.register(PaymentMessageTypes.GET_TRANSACTION, new TypeToken<GetTransactionRequest>() {});
        this.register(PaymentMessageTypes.GET_TRANSACTION_RESPONSE, new TypeToken<GetTransactionResponse>() {});
        this.register(PaymentMessageTypes.TRANSFER_BETWEEN_ACCOUNTS, new TypeToken<TransferBetweenAccountsRequest>() {});
        this.register(PaymentMessageTypes.TRANSFER_BETWEEN_ACCOUNTS_RESPONSE, new TypeToken<TransactionId>() {});
        this.register(PaymentMessageTypes.GET_IDENTIFICATION, new TypeToken<GetIdentificationRequest>() {});
        this.register(PaymentMessageTypes.GET_IDENTIFICATION_RESPONSE, new TypeToken<GetIdentificationResponse>() {});
        this.register(PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE, new TypeToken<LoadOrWithdrawWithoutCoverageRequest>() {});
        this.register(PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE_RESPONSE, new TypeToken<TransactionId>() {});
        this.register(PaymentMessageTypes.TRANSFER_JACKPOT, new TypeToken<TransferJackpotRequest>() {});
        this.register(PaymentMessageTypes.TRANSFER_JACKPOT_RESPONSE, new TypeToken<TransactionId>() {});
        this.register(PaymentMessageTypes.GET_EXCHANGE_RATES, new TypeToken<GetExchangeRatesRequest>() {});
        this.register(PaymentMessageTypes.GET_EXCHANGE_RATES_RESPONSE, new TypeToken<GetExchangeRatesResponse>() {});
        this.register(PaymentMessageTypes.CREATE_JACKPOT, new TypeToken<CreateJackpotRequest>() {});
        this.register(PaymentMessageTypes.CREATE_JACKPOT_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
        this.register(PaymentMessageTypes.GET_JACKPOT, new TypeToken<GetJackpotRequest>() {});
        this.register(PaymentMessageTypes.GET_JACKPOT_RESPONSE, new TypeToken<GetJackpotResponse>() {});
        this.register(PaymentMessageTypes.CLOSE_JACKPOT, new TypeToken<CloseJackpotRequest>() {});
        this.register(PaymentMessageTypes.CLOSE_JACKPOT_RESPONSE, new TypeToken<CloseJackpotResponse>() {});
        this.register(PaymentMessageTypes.MOBILE_PURCHASE, new TypeToken<MobilePurchaseRequest>() {});
        this.register(PaymentMessageTypes.MOBILE_PURCHASE_RESPONSE, new TypeToken<MobilePurchaseResponse>() {});
    }
}