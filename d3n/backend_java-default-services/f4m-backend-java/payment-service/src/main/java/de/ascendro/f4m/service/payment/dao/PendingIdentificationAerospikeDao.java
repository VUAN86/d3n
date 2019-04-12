package de.ascendro.f4m.service.payment.dao;

public interface PendingIdentificationAerospikeDao {
    PendingIdentification getPendingIdentification(String tenantId, String profileId);
    void deletePendingIdentification(String tenantId, String profileId);
    void createPendingIdentification(PendingIdentification pendingIdentification);
}
