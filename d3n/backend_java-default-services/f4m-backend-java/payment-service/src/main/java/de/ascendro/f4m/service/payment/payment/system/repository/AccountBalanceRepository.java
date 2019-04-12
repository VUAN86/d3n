package de.ascendro.f4m.service.payment.payment.system.repository;



import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.payment.system.model.AccountBalance;

import javax.persistence.EntityManager;
import java.util.List;

public interface AccountBalanceRepository {

    AccountBalance get(String profileId, int tenantId, Currency currency, EntityManager entityManager);

    void create(String profileId, int tenantId, Currency currency, EntityManager entityManager);

    AccountBalance update(AccountBalance accountBalance, EntityManager entityManager);

    AccountBalance delete(String profileId, EntityManager entityManager);

    List<AccountBalance> getUserAccountBalance(String profileId, int tenantId, EntityManager entityManager);

}

