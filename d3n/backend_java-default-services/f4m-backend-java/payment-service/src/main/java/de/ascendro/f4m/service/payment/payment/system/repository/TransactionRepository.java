package de.ascendro.f4m.service.payment.payment.system.repository;


import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionFilterType;
import de.ascendro.f4m.service.payment.payment.system.model.Transaction;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository   {

    Transaction create(Transaction transaction, EntityManager entityManager);

    List<Transaction> getTransactions(String mgiId, EntityManager entityManager);

    Transaction getMoneyTransaction(String transactionId, EntityManager entityManager);

    Transaction getTransaction(String transactionId, EntityManager entityManager);

    void update(Transaction transaction, EntityManager entityManager);

    String createTransactionId(String tenantId);

    List<Transaction> getHistory(String accountId, Currency currency, Integer limit, Integer Offset,
                                 LocalDateTime startDate, LocalDateTime endDateTime, TransactionFilterType type, EntityManager em);
}
