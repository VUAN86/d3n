package de.ascendro.f4m.service.payment.payment.system.repository.impl;

import de.ascendro.f4m.service.payment.payment.system.model.Tenant;
import de.ascendro.f4m.service.payment.payment.system.repository.TenantRepository;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class TenantRepositoryImpl implements TenantRepository {

    @Override
    public Tenant get(int tenantId, EntityManager em) {
        String sqlString = "SELECT * FROM tenant t WHERE t.id=" + tenantId + " ;";
        Query query = em.createNativeQuery(sqlString, Tenant.class);
        return (Tenant) query.getSingleResult();
    }
}
