package de.ascendro.f4m.service.analytics.module.statistic.jdbc;

import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.config.IModuleConfig;

public class JdbcConfig implements IModuleConfig {

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String value) {
        url = value;
    }

    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String value) {
        userName = value;
    }

    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String value) {
        password = value;
    }

    private int maxConnections;

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int value) {
        maxConnections = value;
    }

    @Override
    public void setConfiguration(AnalyticsConfig config) {

        this.setUrl(config.getProperty(AnalyticsConfig.MYSQL_DATABASE_URL));
        this.setUserName(config.getProperty(AnalyticsConfig.MYSQL_DATABASE_USER));
        this.setPassword(config.getProperty(AnalyticsConfig.MYSQL_DATABASE_PASSWORD));

        this.setMaxConnections(config.getPropertyAsInteger(AnalyticsConfig.MYSQL_DATABASE_MAX_CONNECTION));
    }
}
