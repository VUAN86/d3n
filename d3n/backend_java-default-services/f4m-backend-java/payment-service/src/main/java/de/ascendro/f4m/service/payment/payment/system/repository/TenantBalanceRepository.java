package de.ascendro.f4m.service.payment.payment.system.repository;



import de.ascendro.f4m.service.payment.payment.system.model.TenantBalance;

import javax.persistence.EntityManager;
import java.math.BigDecimal;

public interface TenantBalanceRepository   {

    void create(int tenantId, EntityManager em);

    void update(int tenantId, BigDecimal amount, EntityManager em);

    void update(TenantBalance tenantBalance, EntityManager em);

    TenantBalance get(int tenantId, EntityManager em);

}
