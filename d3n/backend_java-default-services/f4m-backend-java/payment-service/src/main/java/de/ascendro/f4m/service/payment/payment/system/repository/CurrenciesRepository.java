package de.ascendro.f4m.service.payment.payment.system.repository;


import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.payment.system.model.Currencies;

import javax.persistence.EntityManager;
import java.util.List;

public interface CurrenciesRepository {

    List<Currencies> getAll(EntityManager em);

    Currencies get(Currency currency, EntityManager em);

}
