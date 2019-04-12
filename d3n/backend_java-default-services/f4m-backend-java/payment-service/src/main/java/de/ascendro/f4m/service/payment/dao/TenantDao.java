package de.ascendro.f4m.service.payment.dao;

public interface TenantDao {

	public TenantInfo getTenantInfo(String tenantId);

	default void cloneTenantInfo(String tenantId){

    };
}
