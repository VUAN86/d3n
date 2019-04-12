package de.ascendro.f4m.service.analytics.module.statistic.jdbc;


import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class DatabaseFactory implements Provider<Connection> {

    @InjectLogger
    private static Logger LOGGER;

    private MysqlConnectionPoolDataSource connectionPoolDataSource;
    private PoolManager poolManager;

    private final JdbcConfig config;

    @Inject
    public DatabaseFactory(Config config, ServiceMonitoringRegister serviceMonitoringRegister){
        JdbcConfig jdbcConfig = new JdbcConfig();
        jdbcConfig.setConfiguration((AnalyticsConfig) config);
        this.config = jdbcConfig;
        serviceMonitoringRegister.register(this::updateMonitoringStatistics);
    }

    public PoolManager getPoolManager() {
        if (connectionPoolDataSource == null || poolManager == null) {
            connectionPoolDataSource = new MysqlConnectionPoolDataSource();
            connectionPoolDataSource.setURL(config.getUrl());
            connectionPoolDataSource.setUser(config.getUserName());
            connectionPoolDataSource.setPassword(config.getPassword());
            poolManager = new PoolManager(connectionPoolDataSource, config.getMaxConnections());
        }
        return poolManager;
    }

    @Override
    public Connection get() {
        try {
            return this.getPoolManager().getConnection();
        } catch (SQLException e) {
            throw new F4MAnalyticsFatalErrorException("Could not create connection", e);
        }
    }

	protected void updateMonitoringStatistics(ServiceStatistics statistics) {
		MonitoringConnectionStatus status;
		if (poolManager != null && poolManager.isAnyConnectionAttemptMade()) {
            status = (poolManager.getActiveConnections() > 0 || poolManager.getRecycledConnections() > 0)
                    ? MonitoringConnectionStatus.OK : MonitoringConnectionStatus.NOK;
        } else {
			status = MonitoringConnectionStatus.NC;
		}
		statistics.getConnectionsToDb().setMysql(status);
	}
}
