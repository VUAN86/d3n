package de.ascendro.f4m.service.payment.payment.system.manager.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.exception.*;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.TransactionFilterType;
import de.ascendro.f4m.service.payment.model.TransferFundsRequest;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.payment.model.external.*;
import de.ascendro.f4m.service.payment.model.internal.*;
import de.ascendro.f4m.service.payment.payment.system.manager.AccountBalanceManager;
import de.ascendro.f4m.service.payment.payment.system.model.*;
import de.ascendro.f4m.service.payment.payment.system.repository.*;
import de.ascendro.f4m.service.payment.rest.model.TransactionRestInsert;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AccountBalanceManagerImpl implements AccountBalanceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountBalanceManagerImpl.class);
    private static final ZoneOffset TIMEZONE = ZoneOffset.UTC;
    private Gson gson;
    @Inject
    @PersistenceContext
    private EntityManager entityManager;
    private AccountBalanceRepository accountBalanceRepository;
    private CurrenciesRepository currenciesRepository;
    private GameBalanceRepository gameBalanceRepository;
    private TenantBalanceRepository tenantBalanceRepository;
    private TransactionRepository transactionRepository;
    private TenantRepository tenantRepository;

    @Inject
    public AccountBalanceManagerImpl(AccountBalanceRepository accountBalanceRepository, CurrenciesRepository currenciesRepository,
                                     GameBalanceRepository gameBalanceRepository, TenantBalanceRepository tenantBalanceRepository,
                                     TransactionRepository transactionRepository, TenantRepository tenantRepository) {
        this.accountBalanceRepository = accountBalanceRepository;
        this.currenciesRepository = currenciesRepository;
        this.gameBalanceRepository = gameBalanceRepository;
        this.tenantBalanceRepository = tenantBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.tenantRepository = tenantRepository;
        gson = new GsonBuilder().create();

    }


    @Override
    public synchronized GetUserAccountBalanceResponse getUserAccountBalance(String profileId, String tenantId) {
        int tenantIdInInt = Integer.parseInt(tenantId);
        EntityTransaction et = entityManager.getTransaction();
        try {
            et.begin();
            LOGGER.debug("getUserAccountBalance");
            List<GetAccountBalanceResponse> list = getAccountBalanceResponseList(profileId, tenantIdInInt);
            LOGGER.debug("getUserAccountBalance list {}", list);
            et.commit();
            return new GetUserAccountBalanceResponse(list);
        } catch (Exception e) {
            LOGGER.error("The error of the method getUserAccountBalance - {} ", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MNoCurrencyException("User balance not found.");
        }
    }


    @Override
    public synchronized GetAccountBalanceResponse getAccountBalance(GetAccountBalanceRequest request) {
        EntityTransaction et = entityManager.getTransaction();
        try {
            et.begin();
            AccountBalance accountBalance = accountBalanceRepository.get(
                    request.getProfileId(),
                    Integer.parseInt(request.getTenantId()),
                    request.getCurrency(),
                    entityManager
            );
            et.commit();
            return new GetAccountBalanceResponse()
                    .currency(accountBalance.getCurrency())
                    .amount(accountBalance.getBalance());

        } catch (Exception e) {
            LOGGER.error("The error of the method getUserAccountBalance - {} ", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MNoCurrencyException("User balance not found.");
        }
    }

    @Override
    public synchronized void transferBetweenAccounts(TransferBetweenAccountsRequest req) {
        EntityTransaction et = entityManager.getTransaction();
        BigDecimal amount = req.getAmount();
        String toProfileId = req.getToProfileId();
        String tenantId = req.getTenantId();
        int tenantInInt = Integer.parseInt(tenantId);
        try {
            if (Objects.nonNull(req.getFromProfileId()) || Objects.nonNull(toProfileId)) {
                Transaction transaction = createTransaction(req, req.getCurrency());
                et.begin();
                if (Objects.nonNull(req.getFromProfileId())) {
                    withdrawMoneyFromAccount(req, transaction);
                } else {
                    TenantBalance tenantBalance = tenantBalanceRepository.get(tenantInInt, entityManager);
                    if (Objects.nonNull(tenantBalance)) {
                        if (balanceCheck(tenantBalance, amount)) {
                            tenantBalanceRepository.update(tenantInInt, amount.negate(), entityManager);
                            transaction.setFromProfileId(tenantId);
                        } else
                            throw new F4MInsufficient_Funds("Insufficient funds in the tenant account: " + tenantId);
                    } else
                        throw new F4MNotFoundException("Tenant account: " + tenantInInt + " not found.");
                }

                if (Objects.nonNull(toProfileId)) {
                    AccountBalance toAccount = getAccountBalance(toProfileId, tenantInInt, req.getCurrency());
                    if (Objects.nonNull(toAccount)) {
                        toAccount.setBalance(toAccount.getBalance().add(amount));
                        accountBalanceRepository.update(toAccount, entityManager);
                        transaction.setToProfileId(toProfileId);
                    } else {
                        throw new F4MNotFoundException("Tenant account: " + tenantInInt + " not found.");
                    }
                } else if (Objects.nonNull(tenantBalanceRepository.get(tenantInInt, entityManager))) {
                    tenantBalanceRepository.update(tenantInInt, amount, entityManager);
                    transaction.setFromProfileId(tenantId);
                } else {
                    throw new F4MTransferBetweenException("Transfer between error by tenant: " + tenantId);
                }
                transaction.setEndDate(LocalDateTime.now());
                transactionRepository.create(transaction, entityManager);
                et.commit();
            } else {
                throw new F4MTransferBetweenException("Profiles null.");
            }
        } catch (F4MException e) {
            LOGGER.error("TransferBetweenAccounts error e {}", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MTransferBetweenException("Transfer between account error: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("The error of the method transferBetweenAccounts - {} ", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MTransferBetweenException("Transfer between account error: " + e.getMessage());
        }
    }

    @Override
    public synchronized void loadOrWithdrawWithoutCoverage(LoadOrWithdrawWithoutCoverageRequest req) {
//        String fromProfileId = req.getFromProfileId();
//        String toProfileId = req.getToProfileId();
//        if (Objects.nonNull(req.getFromProfileId())) {
//            fromProfileId = req.getFromProfileId();
//        }
//        if (Objects.nonNull(req.getToProfileId())) {
//            toProfileId = req.getToProfileId();
//        }
        int tenantInInt = toIntOrDefault(req.getTenantId());
        Transaction transaction = createTransaction(req, req.getCurrency());
        EntityTransaction et = entityManager.getTransaction();
        try {
            et.begin();
            if (Objects.nonNull(req.getFromProfileId())) {
                AccountBalance fromAccount = getAccountBalance(req.getFromProfileId(), tenantInInt, req.getCurrency());
                if (balanceCheck(fromAccount, req.getAmount())) {
                    Asserts.notNull(fromAccount, "FromAccount ");
                    fromAccount.setBalance(fromAccount.getBalance().add(req.getAmount().negate()));
                    accountBalanceRepository.update(fromAccount, entityManager);
                    transaction.setFromProfileId(req.getFromProfileId());
                } else {
                    throw new F4MInsufficient_Funds("Insufficient funds in the tenant account: " + req.getFromProfileId());
                }
            } else
            {
                Asserts.notNull(req.getTenantId(), "Tenant ");
                // in fact, the tenant account only money, but we have indicated for clarity.
                transaction.setFromProfileId(req.getTenantId());
            }

            if (Objects.nonNull(req.getToProfileId())) {
                AccountBalance toAccount = getAccountBalance(req.getToProfileId(), tenantInInt, req.getCurrency());
                Asserts.notNull(toAccount, "ToAccount ");
                toAccount.setBalance(toAccount.getBalance().add(req.getAmount()));
                accountBalanceRepository.update(toAccount, entityManager);
                transaction.setToProfileId(req.getToProfileId());
            }

            transaction.setEndDate(LocalDateTime.now());
            transactionRepository.create(transaction, entityManager);
            et.commit();
        } catch (Exception e) {
            LOGGER.error("The error of the method loadOrWithdrawWithoutCoverage - {} ", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MLoadOfCoverageException("Exception load of coverage.");
        }
    }

    @Override
    public synchronized GetAccountHistoryResponse getAccountHistory(GetUserAccountHistoryRequest request, String tenantId, String profileId) {
        if (request instanceof GetAccountHistoryRequest) {
            profileId = ((GetAccountHistoryRequest) request).getProfileId();
        }
        EntityTransaction et = entityManager.getTransaction();
        try {
            et.begin();
            List<Transaction> transactions = getTransactions(request, profileId);
            List<GetTransactionResponse> transactionResponseList = transactions
                    .stream()
                    .map(this::createGetTransactionResponse)
                    .collect(Collectors.toList());
            et.commit();
            return new GetAccountHistoryResponse()
                    .items(transactionResponseList)
                    .limit(request.getLimit())
                    .offset(request.getOffset());
        } catch (Exception e) {
            LOGGER.error("The error of the method getAccountHistory - {} ", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MPaymentException("");
        }
    }

    @Override
    public synchronized List<String> convertBetweenCurrencies(PaymentClientInfo info, ConvertBetweenCurrenciesUserRequest req, ExchangeRate rate) {
        Asserts.notNull(rate.getFromAmount(), "FromAmount ");
        Asserts.notNull(rate.getToAmount(), "ToAmount ");
        Asserts.notNull(rate.getFromCurrency(), "FromCurrency ");
        Asserts.notNull(rate.getToCurrency(), "ToCurrency ");

        TransactionRestInsert insert = new TransactionRestInsert();
        String tenantId = info.getTenantId();
        String profileId = info.getProfileId();
        Asserts.notNull(tenantId, "TenantId ");
        Asserts.notNull(profileId, "ProfileId ");


        List<String> transactions = new ArrayList<>();
        EntityTransaction et = entityManager.getTransaction();

        try {
            et.begin();
            List<AccountBalance> accountBalance = accountBalanceRepository.getUserAccountBalance(profileId, Integer.parseInt(tenantId), entityManager);
            LOGGER.debug("ConvertBetweenCurrencies accountBalance 1 {} ", accountBalance);

            if (Objects.nonNull(accountBalance)) {
                accountBalance.forEach(balance -> {
                    LOGGER.debug("convertBetweenCurrencies bCurrency {}, rCurrency {} ", balance.getCurrency(), rate.getFromCurrency());
                    LOGGER.debug("convertBetweenCurrencies money > 0 {} ", rate.getFromAmount().compareTo(BigDecimal.ZERO) > 0);
                    if (Currency.equals(balance.getCurrency(), rate.getFromCurrency())) {

                        if (rate.getFromAmount().compareTo(BigDecimal.ZERO) > 0) {
                            if (balanceCheck(balance, rate.getFromAmount())) {
                                balance.setBalance(balance.getBalance().add(rate.getFromAmount().negate()));
                                accountBalanceRepository.update(balance, entityManager);
                                LOGGER.debug("convertBetweenCurrencies accountBalance 2  {} ", balance);

                                Transaction transactionFromTo = createTransaction(info, balance.getCurrency(), rate.getFromAmount(), req.getDescription());
                                transactionFromTo.setType(TransactionFilterType.TRANSFER);
                                transactionFromTo.setFromProfileId(profileId);
                                transactionRepository.create(transactionFromTo, entityManager);

                                if (balance.getCurrency() == Currency.MONEY) {
                                    LOGGER.debug("convertBetweenCurrencies accountBalance 2.1  {} ", balance);
                                    Tenant tenant = tenantRepository.get(Integer.parseInt(tenantId), entityManager);
                                    LOGGER.debug("convertBetweenCurrencies tenant 2.2  {} ", tenant);
                                    Asserts.notNull(tenant, "Tenant ");
                                    if (tenant.getCurrency().equals(rate.getFromCurrency())) {
                                        TenantBalance tenantBalance = tenantBalanceRepository.get(Integer.parseInt(tenantId), entityManager);
                                        tenantBalance.setBalance(tenantBalance.getBalance().add(rate.getFromAmount()));
                                        tenantBalanceRepository.update(tenantBalance, entityManager);
                                        LOGGER.debug("convertBetweenCurrencies tenantBalance 2.3  {} ", tenantBalance);
                                        transactionFromTo.setToProfileId(tenantId);
                                    } else
                                        throw new F4MPaymentException("Tenant: " + tenantId + " tenantCurrency: " + tenant.getCurrency() + "rateCurrency: " + rate.getFromCurrency() + " currency not equals!");
                                }
                                transactions.add(transactionFromTo.getTransactionId());
                            } else
                                throw new F4MInsufficient_Funds("Insufficient funds in the account: " + profileId);

                        } else
                            throw new F4MPaymentException("ConvertBetweenCurrencies fromAmount must be greater than zero.");


                    } else if (Currency.equalsWithoutRealMoney(balance.getCurrency(), rate.getToCurrency())) {
                        LOGGER.debug("convertBetweenCurrencies bCurrency1 {}, rCurrency2 {} ", balance.getCurrency(), rate.getFromCurrency());
                        if (rate.getToAmount().compareTo(BigDecimal.ZERO) > 0) {
                            balance.setBalance(balance.getBalance().add(rate.getToAmount()));
                            accountBalanceRepository.update(balance, entityManager);
                            LOGGER.debug("convertBetweenCurrencies accountBalance 3  {} ", balance);

                            Transaction transactionTo = createTransaction(info, req.getToCurrency(), rate.getToAmount(), req.getDescription());
                            transactionTo.setFromProfileId(tenantId);
                            transactionTo.setType(TransactionFilterType.TRANSFER);
                            transactionTo.setToProfileId(profileId);
                            transactionRepository.create(transactionTo, entityManager);
                            LOGGER.debug("convertBetweenCurrencies transactionTo 3.1.0  {} ", transactionTo);
                            transactions.add(transactionTo.getTransactionId());


                        }else
                            throw new F4MPaymentException("ConvertBetweenCurrencies fromAmount must be greater than zero.");
                    }
                });
                et.commit();
                LOGGER.debug("convertBetweenCurrencies transactions {} ", transactions );
                if (transactions.size() > 1) {
                    return transactions;
                }
                // there must be at least two transactions.
                throw new F4MPaymentException("ConvertBetweenCurrencies error");
            } else {
                throw new F4MNotFoundException("AccountBalance: " + profileId + " not found.");
            }
        } catch (Exception e) {
            LOGGER.error("The error of the method convertBetweenCurrencies - {} ", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MPaymentException("");
        }
    }

    @Override
    public synchronized TenantBalance getTenantBalance(String tenantId) {
        return tenantBalanceRepository.get(Integer.parseInt(tenantId), entityManager);
    }
    private Transaction createTransaction(PaymentClientInfo info, Currency currency, BigDecimal amount, String description) {
        String tenantId = info.getTenantId();
        return new Transaction()
                .transactionId(transactionRepository.createTransactionId(tenantId))
                .amount(amount)
                .currency(currency)
                .status(PaymentTransactionState.PROCESSED)
                .appId(Integer.parseInt(info.getAppId()))
                .tenantId(tenantId)
                .description(description);
    }

    @Override
    public void createUser(InsertOrUpdateUserRequest request) {
        String profileId = request.getProfileId();
        String tenantId = request.getTenantId();
        Asserts.notNull(profileId, "Profile ");
        Asserts.notNull(tenantId, "Tenant ");
        getAccountBalanceResponseList(profileId, Integer.parseInt(tenantId));
    }

    private void withdrawMoneyFromAccount(TransferBetweenAccountsRequest req, Transaction transaction) {
        AccountBalance fromAccount = accountBalanceRepository.get(req.getFromProfileId(), toIntOrDefault(req.getTenantId()),  req.getCurrency(), entityManager);
        if (Objects.nonNull(fromAccount)) {
            if (balanceCheck(fromAccount, req.getAmount())) {
                fromAccount.setBalance(fromAccount.getBalance().add(req.getAmount().negate()));
                accountBalanceRepository.update(fromAccount, entityManager);
                transaction.setFromProfileId(req.getFromProfileId());
            } else
                throw new F4MInsufficient_Funds("Insufficient funds in the account: " + fromAccount.getProfileId());
        } else
            throw new F4MNotFoundException("Account: " + req.getFromProfileId() + " not found.");
    }


    private Transaction createTransaction(TransferFundsRequest req, Currency currency) {
        Integer appId = Objects.nonNull(req.getPaymentDetails())
                && Objects.nonNull(req.getPaymentDetails().getAppId())
                ? Integer.parseInt(req.getPaymentDetails().getAppId())
                : 0;
        return new Transaction()
                .transactionId(transactionRepository.createTransactionId(req.getTenantId()))
                .amount(req.getAmount())
                .currency(currency)
                .status(PaymentTransactionState.PROCESSED)
                .type(TransactionFilterType.TRANSFER)
                .appId(appId)
                .tenantId(req.getTenantId())
                .description(req.getPaymentDetails().getAdditionalInfo())
                .details(gson.toJson(req.getPaymentDetails()));
    }



    private GetTransactionResponse createGetTransactionResponse(Transaction transaction) {
        GetTransactionResponse response = new GetTransactionResponse();
        response.setCreationDate(transaction.getStartDate().atZone(TIMEZONE));
        if (transaction.getType() == TransactionFilterType.CREDIT) {
            response.setFromCurrency(transaction.getCurrency());
        } else if (transaction.getType() == TransactionFilterType.DEBIT) {
            response.setToCurrency(transaction.getCurrency());
        }
        response.setAmount(transaction.getAmount());
        response.setTenantId(String.valueOf(transaction.getTenantId()));
        response.setType(transaction.getType() != null ? transaction.getType().name() : null);
        response.setId(transaction.getTransactionId());
        PaymentDetails details = gson.fromJson(transaction.getDetails(), PaymentDetails.class);
        response.setPaymentDetails(details != null ? details : new PaymentDetails());
        return response;
    }


    private List<Transaction> getTransactions(GetUserAccountHistoryRequest req, String profileId) {
        Integer limit = notNullOrDefault(req.getLimit());
        Integer Offset = notNullOrDefault(req.getOffset());
        LocalDateTime start = req.getStartDate() != null ? req.getStartDate().toLocalDateTime() : LocalDateTime.now().minusYears(1);
        LocalDateTime end = req.getEndDate() != null ? req.getEndDate().toLocalDateTime() : LocalDateTime.now().plusYears(1);
        TransactionFilterType type = req.getTypeFilter() != null ? req.getTypeFilter() : TransactionFilterType.ALL;

        return transactionRepository.getHistory(
                profileId,
                req.getCurrency(),
                limit,
                Offset,
                start,
                end,
                type,
                entityManager
        );
    }




    @Override
    public GetExternalPaymentResponse getMoneyTransaction(GetExternalPaymentRequest request, String tenantId) {
        Transaction transaction = transactionRepository.getMoneyTransaction(request.getTransactionId(), entityManager);
        return new GetExternalPaymentResponse()
                .amount(transaction.getAmount())
                .created(transaction.getStartDate())
                .description(transaction.getDescription());
    }


    @Override
    public GetTransactionResponse getExternalPayment(GetTransactionRequest request) {
        Transaction transaction = transactionRepository.getTransaction(request.getTransactionId(), entityManager);
        return new GetTransactionResponse()
                .transactionId(transaction.getTransactionId())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .creationDate(transaction.getStartDate());
    }

    private AccountBalance getAccountBalance(String profileId, int tenantId, Currency currency) {
        AccountBalance accountBalance = accountBalanceRepository.get(profileId, tenantId, currency, entityManager);
        Currencies currencies = currenciesRepository.get(currency, entityManager);
        Asserts.notNull(currencies, "Currencies ");
        if (Objects.nonNull(accountBalance)) {
            return accountBalanceRepository.get(profileId, tenantId, currency, entityManager);
        } else {
           return createdAccount(profileId, tenantId, currency, currencies);
        }

    }


    private List<GetAccountBalanceResponse> getAccountBalanceResponseList(String profileId, int tenantIdInInt) {
        List<AccountBalance> accountBalanceList = accountBalanceRepository.getUserAccountBalance(profileId, tenantIdInInt, entityManager);
        List<Currencies> currenciesList = currenciesRepository.getAll(entityManager);
        if (accountBalanceList.isEmpty() || accountBalanceList.size() < currenciesList.size()) {
            createdAccountList(profileId, tenantIdInInt, currenciesList, accountBalanceList);
            accountBalanceList = accountBalanceRepository.getUserAccountBalance(profileId, tenantIdInInt, entityManager);
        }
        return accountBalanceList
                .stream()
                .map(accountBalance -> new GetAccountBalanceResponse(accountBalance.getBalance(), accountBalance.getCurrency()))
                .collect(Collectors.toList());
    }


    private AccountBalance createdAccount(String profileId, int tenantId, Currency currency, Currencies currencies) {
        if (currencies.getCurrency() == currency) {
            accountBalanceRepository.create(profileId, tenantId, currency, entityManager);
        }
        return accountBalanceRepository.get(profileId, tenantId, currency, entityManager);
    }

    private void createdAccountList(String profileId, int tenantId, List<Currencies> currenciesList, List<AccountBalance> list) {
        currenciesList.forEach(currency -> {
            if (list.stream().noneMatch(accountBalance -> accountBalance.getCurrency() == currency.getCurrency())) {
                accountBalanceRepository.create(profileId, tenantId, currency.getCurrency(), entityManager);
            }
        });
    }


    private <T extends AbstractBalanceEntity> boolean balanceCheck(T balance, BigDecimal amount) {
        return balance.getBalance().compareTo(amount) >= 0;
    }

    private Integer toIntOrDefault(String s) {
        return Objects.nonNull(s) ? Integer.parseInt(s) : 0;
    }


    private Integer notNullOrDefault(Integer n) {
        return Objects.nonNull(n) ? n : 0;
    }

}
