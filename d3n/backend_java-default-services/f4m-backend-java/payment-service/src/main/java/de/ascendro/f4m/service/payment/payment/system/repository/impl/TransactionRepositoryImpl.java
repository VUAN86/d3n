package de.ascendro.f4m.service.payment.payment.system.repository.impl;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionFilterType;
import de.ascendro.f4m.service.payment.payment.system.model.Transaction;
import de.ascendro.f4m.service.payment.payment.system.repository.TransactionRepository;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TransactionRepositoryImpl implements TransactionRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionRepositoryImpl.class);

    @Override
    public Transaction create(Transaction transaction, EntityManager entityManager) {
        entityManager.persist(transaction);
        return transaction;
    }

    @Override
    public Transaction getMoneyTransaction(String transactionId, EntityManager entityManager) {
        String sqlString = "SELECT * " +
                "FROM transaction t " +
                "WHERE t.transactionId='" + transactionId +"' AND t.currency='" + Currency.MONEY+ "';";
        Query query = entityManager.createNativeQuery(sqlString, Transaction.class);
        return (Transaction) query.getSingleResult();
    }

    @Override
    public Transaction getTransaction(String transactionId, EntityManager entityManager) {
        return entityManager.find(Transaction.class, transactionId);
    }

    @Override
    public void update(Transaction transaction, EntityManager entityManager) {
        entityManager.merge(transaction);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Transaction> getTransactions(String mgiId, EntityManager entityManager) {
        List<Transaction> transactionList;
        String sqlString = "SELECT * " +
                "FROM transaction t " +
                "WHERE t.mgiId='" + mgiId + "';";
        Query query = entityManager.createNativeQuery(sqlString, Transaction.class);
        transactionList = query.getResultList();
        transactionList.forEach(entityManager::refresh);
        return transactionList;
    }

    @Override
    public String createTransactionId(String tenantId) {
        tenantId = Objects.nonNull(tenantId) ? tenantId : "";
        return tenantId + System.currentTimeMillis() + UUID.randomUUID().toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Transaction> getHistory(String accountId, Currency currency, Integer limit, Integer Offset, LocalDateTime startDate,
                                        LocalDateTime endDateTime, TransactionFilterType type, EntityManager em) {
        String typeString = type == TransactionFilterType.ALL ? "" : "AND t.type='" + type + "' ";
        String sqlString = "SELECT * " +
                "FROM transaction t " +
                "WHERE (t.fromProfileId='" + accountId + "'" +
                "OR t.toProfileId='"+accountId+"')"+
                "AND t.currency='" + currency + "' " +
                "AND t.startDate>='" + startDate + "' " +
                "AND t.endDate<='" + endDateTime + "' " +
                 typeString +
                "LIMIT " + limit + " " +
                "OFFSET " + Offset + ";";
        LOGGER.debug("getHistory sqlString {} ", sqlString);
        Query query = em.createNativeQuery(sqlString, Transaction.class);
        return query.getResultList();
    }
}
