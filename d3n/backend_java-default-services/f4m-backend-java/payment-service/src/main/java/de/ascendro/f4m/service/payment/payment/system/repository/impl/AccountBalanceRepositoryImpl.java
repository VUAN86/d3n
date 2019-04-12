package de.ascendro.f4m.service.payment.payment.system.repository.impl;


import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.payment.system.model.AccountBalance;
import de.ascendro.f4m.service.payment.payment.system.repository.AccountBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AccountBalanceRepositoryImpl implements AccountBalanceRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountBalanceRepositoryImpl.class);

    @Override
    public AccountBalance get(String profileId, int tenantId, Currency currency, EntityManager entityManager) {
        String sqlString = "SELECT * " +
                "FROM account_balance a " +
                "WHERE a.profileId ='" + profileId + "'" +
                "AND a.tenantId=" + tenantId + " " +
                "AND a.accountCurrency='" + currency + "';";
        Query query = entityManager.createNativeQuery(sqlString, AccountBalance.class);
        AccountBalance accountBalance=null;
             accountBalance = (AccountBalance) query.getSingleResult();
        entityManager.refresh(accountBalance);
        return accountBalance != null
                && accountBalance.getTenant().getId() == tenantId
                && accountBalance.getCurrency() == currency
                ? accountBalance : null;
    }

    @Override
    public void create(String profileId, int tenantId, Currency currency, EntityManager entityManager) {
        String sqlString = "INSERT INTO account_balance (profileId, tenantId, accountCurrency, balance) " +
                "VALUES ('" + profileId + "'," + tenantId + ",'" + currency + "'," + BigDecimal.ZERO + ");";
        Query query = entityManager.createNativeQuery(sqlString, AccountBalance.class);
        query.executeUpdate();
    }



    @Override
    public  AccountBalance update(AccountBalance accountBalance, EntityManager entityManager) {
        roundGameBalance(accountBalance);
        return entityManager.merge(accountBalance);
    }

    @Override
    public AccountBalance delete(String profileId, EntityManager entityManager) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AccountBalance> getUserAccountBalance(String profileId, int tenantId, EntityManager entityManager) {
        List<AccountBalance> accountBalances = new ArrayList<>();
        LOGGER.debug("getUserAccountBalance profileId: {}, tenantId: {} ", profileId, tenantId);
        try {
            String sqlString = "SELECT * " +
                    "FROM account_balance a " +
                    "WHERE a.profileId ='" + profileId + "' " +
                    "AND a.tenantId=" + tenantId + ";";
            LOGGER.debug("getUserAccountBalance sqlString: {} ", sqlString);
            Query query = entityManager.createNativeQuery(sqlString, AccountBalance.class);
            accountBalances = (query.getResultList());
            LOGGER.debug("getUserAccountBalance accountBalances: {} ", accountBalances);
            accountBalances.forEach(accountBalance -> {
                entityManager.refresh(accountBalance);
                roundGameBalance(accountBalance);
            });
        } catch (Exception e) {
            LOGGER.debug("Exception e {} ", e.getMessage());
        }

        return accountBalances;
    }

    private void roundGameBalance(AccountBalance accountBalance) {
        if (Objects.nonNull(accountBalance)) {
            if (accountBalance.getCurrency() == Currency.BONUS || accountBalance.getCurrency() == Currency.CREDIT) {
                accountBalance.setBalance(accountBalance.getBalance().setScale(0, RoundingMode.HALF_UP));
            }
        }
    }
}
