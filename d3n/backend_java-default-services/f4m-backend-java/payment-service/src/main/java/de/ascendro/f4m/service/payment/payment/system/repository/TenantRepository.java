package de.ascendro.f4m.service.payment.payment.system.repository;

import de.ascendro.f4m.service.payment.payment.system.model.Tenant;

import javax.persistence.EntityManager;

public interface TenantRepository {

    Tenant get(int tenantId, EntityManager em);
}
