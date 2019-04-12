package de.ascendro.f4m.service.payment.payment.system.repository.impl;


import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.payment.system.model.Currencies;
import de.ascendro.f4m.service.payment.payment.system.repository.CurrenciesRepository;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class CurrenciesRepositoryImpl implements CurrenciesRepository {

    @Override
    public List<Currencies> getAll(EntityManager em) {
        return em.createNamedQuery(Currencies.GET_ALL_CURRENCIES, Currencies.class).getResultList();
    }

    @Override
    public Currencies get(Currency currency, EntityManager em) {
        String sqlString = "SELECT * FROM currencies c WHERE c.currency='" + currency + "';";
        Query query = em.createNativeQuery(sqlString, Currencies.class);
        return (Currencies) query.getSingleResult();
    }
}
