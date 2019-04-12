package de.ascendro.f4m.server.analytics;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

public class AnalyticsPrimaryKeyUtil extends PrimaryKeyUtil<EventRecord> {

    @Inject
    public AnalyticsPrimaryKeyUtil(Config config) {
        super(config);
    }

    private String generateKey(EventRecord record) {
        return record.getAnalyticsContent().getUserId() + KEY_ITEM_SEPARATOR + generateId();
    }

    @Override
    public String createPrimaryKey(EventRecord record) {
        return generateKey(record);
    }
}
