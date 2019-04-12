package de.ascendro.f4m.service.analytics.dao;

import de.ascendro.f4m.server.AerospikeDao;

public interface AnalyticsAerospikeDao extends AerospikeDao {
    void loadAnalyticsData();
    void suspendScan();
    void resumeScan();
    boolean isRunning();
}
