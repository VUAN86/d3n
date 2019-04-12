package de.ascendro.f4m.service.analytics.module.statistic.query.base;

import java.sql.Connection;
import java.sql.SQLException;

import de.ascendro.f4m.service.analytics.IMessageProcessor;
import de.ascendro.f4m.service.analytics.module.statistic.model.base.BaseStatisticTable;

public interface ITableUpdater<T extends BaseStatisticTable> extends IMessageProcessor {
    Connection getConnection();
    void triggerBatchExecute() throws SQLException;
    void addRecord(T table) throws SQLException;
}
