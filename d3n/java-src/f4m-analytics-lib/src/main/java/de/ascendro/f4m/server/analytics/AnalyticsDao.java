package de.ascendro.f4m.server.analytics;


public interface AnalyticsDao {
    String createAnalyticsEvent(EventRecord record);

    void createFirstAppUsageRecord(EventContent eventContent);
    void createFirstGameUsageRecord(EventContent eventContent);
    void createNewAppPlayerRecord(EventContent eventContent);
    void createNewGamePlayerRecord(EventContent eventContent);
}
