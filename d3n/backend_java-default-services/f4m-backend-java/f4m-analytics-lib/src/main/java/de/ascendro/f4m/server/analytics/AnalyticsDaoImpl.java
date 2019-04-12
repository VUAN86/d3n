package de.ascendro.f4m.server.analytics;

import javax.inject.Inject;

import com.aerospike.client.AerospikeException;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class AnalyticsDaoImpl extends AerospikeDaoImpl<AnalyticsPrimaryKeyUtil> implements AnalyticsDao {

    public static final String TIMESTAMP_BIN_NAME = "timestamp";
    public static final String CONTENT_BIN_NAME = "content";
    public static final String ANALYTICS_SET_NAME = "analytics";


    @Inject
    public AnalyticsDaoImpl(Config config, AnalyticsPrimaryKeyUtil primaryKeyUtil, AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
        super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
    }

    @Override
    public String createAnalyticsEvent(EventRecord record) {
        final String recordKey = primaryKeyUtil.createPrimaryKey(record);
        createRecord(ANALYTICS_SET_NAME, recordKey,
                getLongBin(TIMESTAMP_BIN_NAME, record.getTimestamp()),
                getJsonBin(CONTENT_BIN_NAME, jsonUtil.toJson(record.getAnalyticsContent())));

        return recordKey;
    }

    @Override
    public void createFirstAppUsageRecord(EventContent eventContent) {
        //test if record is already present, and create new one if is missing
        AdEvent adEvent = (AdEvent)eventContent.getEventData();
        boolean isFirstAppUsage = true;
        try {
            createRecord(AdEvent.ANALYTICS_ADD2APP_SET_NAME,
                    adEvent.getAdId() + PrimaryKeyUtil.KEY_ITEM_SEPARATOR + eventContent.getAppId(),
                    getLongBin(TIMESTAMP_BIN_NAME, DateTimeUtil.getUTCTimestamp()));
        } catch (AerospikeException e) {
            isFirstAppUsage = false;
        }
        adEvent.setFirstAppUsage(isFirstAppUsage);
    }

    @Override
    public void createFirstGameUsageRecord(EventContent eventContent) {
        //test if record is already present, and create new one if is missing
        AdEvent adEvent = (AdEvent)eventContent.getEventData();
        boolean isFirstGameUsage = true;
        try {
            createRecord(AdEvent.ANALYTICS_ADD2GAME_SET_NAME,
                    adEvent.getAdId() + PrimaryKeyUtil.KEY_ITEM_SEPARATOR + adEvent.getGameId().toString(),
                    getLongBin(TIMESTAMP_BIN_NAME, DateTimeUtil.getUTCTimestamp()));
        } catch (AerospikeException e) {
            isFirstGameUsage = false;
        }
        adEvent.setFirstGameUsage(isFirstGameUsage);
    }

    @Override
    public void createNewAppPlayerRecord(EventContent eventContent) {
        //test if record is already present, and create new one if is missing
        PlayerGameEndEvent gameEndEvent = eventContent.getEventData();
        boolean isNewAppPlayer = true;
        try {
            createRecord(PlayerGameEndEvent.ANALYTICS_USER2APP_SET_NAME,
                    eventContent.getUserId() + PrimaryKeyUtil.KEY_ITEM_SEPARATOR + eventContent.getAppId(),
                    getLongBin(TIMESTAMP_BIN_NAME, DateTimeUtil.getUTCTimestamp()));
        } catch (AerospikeException e) {
            isNewAppPlayer = false;
        }
        gameEndEvent.setNewAppPlayer(isNewAppPlayer);
    }

    @Override
    public void createNewGamePlayerRecord(EventContent eventContent) {
        //test if record is already present, and create new one if is missing
        PlayerGameEndEvent gameEndEvent = eventContent.getEventData();
        boolean isNewGamePlayer = true;
        try {
            createRecord(PlayerGameEndEvent.ANALYTICS_USER2GAME_SET_NAME,
                    eventContent.getUserId() + PrimaryKeyUtil.KEY_ITEM_SEPARATOR + gameEndEvent.getGameId(),
                    getLongBin(TIMESTAMP_BIN_NAME, DateTimeUtil.getUTCTimestamp()));
        } catch (AerospikeException e) {
            isNewGamePlayer = false;
        }
        gameEndEvent.setNewGamePlayer(isNewGamePlayer);
    }
}
