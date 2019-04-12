package de.ascendro.f4m.service.payment.payment.system.repository.impl;


import de.ascendro.f4m.service.payment.payment.system.model.TenantBalance;
import de.ascendro.f4m.service.payment.payment.system.repository.TenantBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

public class TenantBalanceRepositoryImpl implements TenantBalanceRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantBalanceRepositoryImpl.class);

    @Override
    public void create(int tenantId, EntityManager em) {
        EntityTransaction et = em.getTransaction();
        et.begin();
        String sqlString =
                "INSERT INTO tenant_balance (tenantId, balance) VALUES (" + tenantId + "," + BigDecimal.ZERO + ");";
        Query query = em.createNativeQuery(sqlString, TenantBalance.class);
        query.executeUpdate();
        et.commit();
    }

    @Override
    public TenantBalance get(int tenantId, EntityManager em) {
        String sqlString = "SELECT * FROM tenant_balance a WHERE a.tenantId=" + tenantId + " ;";
        Query query = em.createNativeQuery(sqlString, TenantBalance.class);
        TenantBalance tenantBalance;
        try {
            tenantBalance = (TenantBalance) query.getSingleResult();
            if (tenantBalance == null) {
                create(tenantId, em);
                tenantBalance = (TenantBalance) query.getSingleResult();
            }
            em.refresh(tenantBalance);
            return tenantBalance;
        } catch (Exception e) {
            throw new NoResultException("Tenant's balance was not found.");
        }
    }

    @Override
    public void update(int tenantId, BigDecimal amount, EntityManager em) {
        TenantBalance tenantBalance = get(tenantId, em);
            LOGGER.debug("update 1 tenantBalance {} ", tenantBalance);
            LOGGER.debug("update 2 tenantBalance {} ", tenantBalance);
            if (Objects.nonNull(tenantBalance)) {
                LOGGER.debug("update 3 tenantBalance {} ", tenantBalance);
                BigDecimal jackpot = tenantBalance.getBalance().add(amount);
                tenantBalance.setBalance(jackpot);
                em.merge(tenantBalance);
                LOGGER.debug("update 4 tenantBalance {} ", tenantBalance);
            }
    }

    @Override
    public void update(TenantBalance tenantBalance, EntityManager em) {
        em.merge(tenantBalance);
    }

}
