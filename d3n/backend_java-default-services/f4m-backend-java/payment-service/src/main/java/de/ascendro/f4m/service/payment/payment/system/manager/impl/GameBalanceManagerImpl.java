package de.ascendro.f4m.service.payment.payment.system.manager.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.payment.model.TransferFundsRequest;
import de.ascendro.f4m.service.payment.payment.system.manager.GameBalanceManager;
import de.ascendro.f4m.service.payment.exception.*;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionFilterType;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.external.PaymentTransactionState;
import de.ascendro.f4m.service.payment.model.internal.*;
import de.ascendro.f4m.service.payment.payment.system.model.AccountBalance;
import de.ascendro.f4m.service.payment.payment.system.model.GameBalance;
import de.ascendro.f4m.service.payment.payment.system.model.Transaction;
import de.ascendro.f4m.service.payment.payment.system.repository.*;
import net.bytebuddy.dynamic.DynamicType;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GameBalanceManagerImpl implements GameBalanceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameBalanceManagerImpl.class);
    private AccountBalanceRepository accountBalanceRepository;
    private TenantBalanceRepository tenantBalanceRepository;
    private GameBalanceRepository gameBalanceRepository;
    private TransactionRepository transactionRepository;
    private CurrenciesRepository currenciesRepository;
    private Gson gson;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    public GameBalanceManagerImpl(AccountBalanceRepository accountBalanceRepository, CurrenciesRepository currenciesRepository,
                                  GameBalanceRepository gameBalanceRepository, TenantBalanceRepository tenantBalanceRepository,
                                  TransactionRepository transactionRepository) {

        this.accountBalanceRepository = accountBalanceRepository;
        this.tenantBalanceRepository = tenantBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.gameBalanceRepository = gameBalanceRepository;
        this.currenciesRepository = currenciesRepository;
        gson = new GsonBuilder().create();
    }

    @Override
    public synchronized TransactionId transferJackpot(TransferJackpotRequest request) {
        Asserts.notNull(request.getMultiplayerGameInstanceId(), "MgiId should not be NULL.");
        Asserts.notNull(request.getTenantId(), "TenantId should not be NULL.");
        EntityTransaction et = entityManager.getTransaction();
        int tenantId = Integer.parseInt(request.getTenantId());
        try {
            et.begin();
            GameBalance gameBalance = gameBalanceRepository.get(request.getMultiplayerGameInstanceId(), entityManager);
            if (Objects.nonNull(gameBalance)) {
                if (gameBalance.getGameState() == GameState.OPEN) {
                    boolean isPaid = transferFunds(
                            request.getFromProfileId(),
                            tenantId,
                            gameBalance.getCurrency(),
                            request.getAmount().negate()
                    );
                    if (isPaid) {
                        Transaction transaction = createTransactionForTransfer(request, gameBalance, request.getMultiplayerGameInstanceId());
                        et.commit();
                        return new TransactionId(transaction.getTransactionId());
                    } else {
                        throw new F4MGameClosedException("Game mgiId: " + request.getMultiplayerGameInstanceId() + " not paid.");
                    }
                } else {
                    throw new F4MGameClosedException("Game mgiId: " + request.getMultiplayerGameInstanceId() + " closed.");
                }
            } else {
                throw new F4MNotFoundException("Game mgiId: " + request.getMultiplayerGameInstanceId() + " not found.");
            }
        } catch (F4MException e) {
            if (et.isActive()) {
                et.rollback();
            }
            LOGGER.error("TransferJackpot error e {}", e.getMessage());
            throw new F4MTransferJackpotException("Transfer jackpot error: " + e.getMessage() + " code: " + e.getCode());
        } catch (Exception e) {
            LOGGER.error("TransferJackpot error e {}", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MTransferJackpotException("Transfer jackpot error.");
        }
    }


    /**
     *
     * @param request
     * @param mgiId
     * @return
     * @See de.ascendro.f4m.server.request.jackpot public void sendTransferMinimumJackpotGuarantee()
     * it is necessary to alter the minimum payment change request transferJackpot
     */
    @Override
    public synchronized TransactionId transferJackpot(LoadOrWithdrawWithoutCoverageRequest request, String mgiId) {
        Asserts.notNull(mgiId, "MgiId should not be NULL.");
        Asserts.notNull(request.getTenantId(), "TenantId should not be NULL.");
        if (request.getToProfileId() != null) {
            EntityTransaction et = entityManager.getTransaction();
            try {
                et.begin();
                GameBalance gameBalance = gameBalanceRepository.get(mgiId, entityManager);
                if (Objects.nonNull(gameBalance)) {
                    if (gameBalance.getGameState() == GameState.OPEN) {
                        Transaction transaction = createTransactionForTransfer(request, gameBalance, null);
                        et.commit();
                        return new TransactionId(transaction.getTransactionId());
                    } else {
                        throw new F4MGameClosedException("Game mgiId = " + mgiId + " closed!");
                    }
                } else {
                    throw new F4MNotFoundException("Game mgiId = " + mgiId + " not found!");
                }
            } catch (F4MException e) {
                if (et.isActive()) {
                    et.rollback();
                }
                LOGGER.error("TransferJackpot error e {}", e.getMessage());
                throw new F4MTransferJackpotException("Transfer jackpot error: " + e.getMessage() + " code: " + e.getCode());
            }  catch (Exception e) {
                LOGGER.error("Method transferJackpot error e {}", e.getMessage());
                    if (et.isActive()) {
                        et.rollback();
                    }
            }
        }
        throw new F4MTransferJackpotException("The profile is not initialized.");
    }

    @Override
    public synchronized boolean createJackpot(CreateJackpotRequest request, CustomGameConfig customGameConfig) {
        int appId = customGameConfig.getAppId() != null ? Integer.parseInt(customGameConfig.getAppId()) : 0;
        int gameId = customGameConfig.getGameId() != null ? Integer.parseInt(customGameConfig.getGameId()) : 0;
        Asserts.notNull(request.getMultiplayerGameInstanceId(), "MgiId should not be NULL.");
        Asserts.notNull(request.getTenantId(), "TenantId should not be NULL.");
        Asserts.notNull(request.getCurrency(), "Currency should not be NULL.");

        GameBalance gameBalance = new GameBalance()
                .gameId(gameId)
                .mgiId(request.getMultiplayerGameInstanceId())
                .tenantId(Integer.parseInt(request.getTenantId()))
                .appId(appId)
                .currency(request.getCurrency());

        EntityTransaction et = entityManager.getTransaction();
        try {
            et.begin();
            gameBalanceRepository.create(gameBalance, entityManager);
            et.commit();
            return true;
        } catch (Exception e) {
            LOGGER.error("Method createJackpot error e {}", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            };
            throw new F4MCreateJackpotException("Error create jackpot game.");
        }

    }

    @Override
    public synchronized GetJackpotResponse getJackpot(GetJackpotRequest request) {
        EntityTransaction et = entityManager.getTransaction();
        try {
            et.begin();
            GameBalance gameBalance = gameBalanceRepository.get(request.getMultiplayerGameInstanceId(), entityManager);
            if (Objects.nonNull(gameBalance)) {
                if(gameBalance.getGameState()==GameState.OPEN) {
                    List<Transaction> transactionList = transactionRepository.getTransactions(request.getMultiplayerGameInstanceId(), entityManager);
                    BigDecimal jackpot = transactionList
                            .stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal::add)
                            .orElse(BigDecimal.ZERO);
                    et.commit();

                    return new GetJackpotResponse()
                            .balalance(jackpot)
                            .currency(gameBalance.getCurrency())
                            .state(gameBalance.getGameState());
                } else {
                    et.commit();
                    return new GetJackpotResponse()
                            .balalance(gameBalance.getBalance())
                            .currency(gameBalance.getCurrency())
                            .state(gameBalance.getGameState());
                }
            } else {
                throw new F4MNotFoundException("Game mgiId: " + request.getMultiplayerGameInstanceId() + " not found.");
            }
        } catch (F4MException e) {
            LOGGER.error("TransferJackpot error e {}", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MTransferJackpotException("Get jackpot error: " + e.getMessage() + " code: " + e.getCode());
        } catch (Exception e) {
            LOGGER.error("Method getJackpot error e {}", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MGetJackpotException("Get jackpot error");
        }
    }

    @Override
    // you need to add to the active timer task cleaning closed games only after a positive response from payment.
    public synchronized CloseJackpotResponse closeJackpot(CloseJackpotRequest request) {
        Asserts.notNull(request.getMultiplayerGameInstanceId(), "MgiId should not be NULL.");
        Asserts.notNull(request.getTenantId(), "TenantId should not be NULL.");
        int tenantId = Integer.parseInt(request.getTenantId());
        List<CloseJackpotRequest.PayoutItem> payoutItems = request.getPayouts();
        EntityTransaction et = entityManager.getTransaction();
        try {
            et.begin();
            GameBalance gameBalance = gameBalanceRepository.get(request.getMultiplayerGameInstanceId(), entityManager);
            BigDecimal jackpot = closeTransactionAndGetJackpot(request.getMultiplayerGameInstanceId());
            if (Objects.nonNull(gameBalance)) {
                if (gameBalance.getGameState() == GameState.OPEN) {
                        Asserts.notNull(payoutItems, "PayoutItem is ");
                        if (!jackpot.equals(BigDecimal.ZERO)) {
                            BigDecimal amount = payoutItems
                                    .stream()
                                    .map(CloseJackpotRequest.PayoutItem::getAmount)
                                    .reduce(BigDecimal::add)
                                    .orElse(BigDecimal.ZERO);
                            if (jackpot.compareTo(amount) >= 0) {
                                payoutItems.forEach(account -> {
                                    boolean isPaid = transferFunds(
                                            account.getProfileId(),
                                            tenantId,
                                            gameBalance.getCurrency(),
                                            account.getAmount()
                                    );
                                    if (isPaid) {
                                        createTransactionForCloseJackpot(request, gameBalance, account.getAmount(), account.getProfileId());
                                    }
                                });

                                BigDecimal tenantAmount = BigDecimal.ZERO;
                                if (jackpot.compareTo(amount) > 0) {
                                    tenantAmount = jackpot.add(amount.negate());
                                    if (Objects.nonNull(tenantBalanceRepository.get(tenantId, entityManager))) {
                                        createTransactionForCloseJackpot(request, gameBalance, tenantAmount, request.getTenantId());
                                        tenantBalanceRepository.update(tenantId, tenantAmount, entityManager);
                                    }
                                }
                                closeGameBalance(payoutItems, gameBalance, tenantAmount, request.getPaymentDetails().getAdditionalInfo());
                                et.commit();
                            } else {
                                throw new F4MInsufficient_Funds("Insufficient funds in the jackpot mgiId: " + request.getMultiplayerGameInstanceId() + ".");
                            }
                        } else {
                            closeGameBalance(payoutItems, gameBalance, null, request.getPaymentDetails().getAdditionalInfo());
                            et.commit();
                        }
                } else {
                    throw new F4MGameClosedException("Game mgiId: " + request.getMultiplayerGameInstanceId() + " closed.");
                }
            } else {
                throw new F4MNotFoundException("Game mgiId: " + request.getMultiplayerGameInstanceId() + " not found.");
            }
        } catch (F4MException e) {
            LOGGER.error("CloseJackpot error e {}", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MCloseJackpotException("Close jackpot error: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Method closeJackpot error e {}", e.getMessage());
            if (et.isActive()) {
                et.rollback();
            }
            throw new F4MCloseJackpotException("Close jackpot error! ");
        }
        throw new F4MCloseJackpotException("Close jackpot error! ");
    }

    private void createTransactionForCloseJackpot(CloseJackpotRequest request, GameBalance gameBalance, BigDecimal amount, String profileId) {
        Transaction transaction = new Transaction()
                .toProfileId(profileId)
                .mgiId(request.getMultiplayerGameInstanceId())
                .amount(amount)
                .currency(gameBalance.getCurrency())
                .status(PaymentTransactionState.PROCESSED)
                .type(TransactionFilterType.TRANSFER)
                .endDate(LocalDateTime.now())
                .appId(gameBalance.getAppId())
                .tenantId(request.getTenantId())
                .description(request.getPaymentDetails().getAdditionalInfo())
                .details(gson.toJson(request.getPaymentDetails()));
        createTransaction(transaction);
    }

    private boolean transferFunds(String profileId, int tenantId, Currency currency, BigDecimal amount) {
        AccountBalance accountBalance = accountBalanceRepository.get(profileId, tenantId, currency, entityManager);
        if (Objects.nonNull(accountBalance)) {
            if (accountBalance.getBalance().compareTo(BigDecimal.ZERO) > 0 && accountBalance.getBalance().compareTo(amount) >= 0) {
                accountBalance.setBalance(accountBalance.getBalance().add(amount));
                return accountBalanceRepository.update(accountBalance, entityManager).getBalance().equals(accountBalance.getBalance());
            } else {
                throw new F4MInsufficient_Funds("Insufficient funds in the account: " + profileId + ".");
            }
        } else throw new F4MUserNotFoundException("User account: " + profileId + " not found.");
    }

    private Transaction createTransactionForTransfer(TransferFundsRequest request, GameBalance gameBalance, String mgiId) {
        Transaction transaction = new Transaction()
                .fromProfileId(request.getFromProfileId())
                .mgiId(mgiId)
                .amount(request.getAmount())
                .currency(gameBalance.getCurrency())
                .status(PaymentTransactionState.PENDING)
                .type(TransactionFilterType.TRANSFER)
                .appId(gameBalance.getAppId())
                .tenantId(request.getTenantId())
                .description("entryFee")
                .details(gson.toJson(request.getPaymentDetails()));
        return createTransaction(transaction);
    }

    private Transaction createTransaction(Transaction transaction) {
        String transactionId = transactionRepository.createTransactionId(transaction.getTransactionId());
        transaction.setTransactionId(transactionId);
        transactionRepository.create(transaction, entityManager);
        return transaction;
    }



    private BigDecimal closeTransactionAndGetJackpot(String mgiId) {
        List<Transaction> transactionList = transactionRepository.getTransactions(mgiId, entityManager);
        return transactionList
                .stream()
                .map(transaction -> {
                    closeTransaction(transaction);
                    return transaction.getAmount();
                })
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }

    private void closeTransaction(Transaction transaction) {
        transaction.setStatus(PaymentTransactionState.PROCESSED);
        transaction.setEndDate(LocalDateTime.now());
        transactionRepository.update(transaction, entityManager);
    }

    private void closeGameBalance(List<CloseJackpotRequest.PayoutItem> payoutItems, GameBalance gameBalance, BigDecimal tenantAmount, String description) {
        if (tenantAmount != null) {
            gameBalance.setPrizes(gson.toJson(payoutItems) + "[{\"tenantId\":\"" + gameBalance.getTenantId() + "\",\"amount\":" + gson.toJson(tenantAmount) + "]}");
        } else gameBalance.setPrizes(gson.toJson(payoutItems));
        gameBalance.setGameState(GameState.CLOSED);
        gameBalance.setDescription(description);
        gameBalance.setEndDate(LocalDateTime.now());
        gameBalanceRepository.update(gameBalance, entityManager);
    }


}
