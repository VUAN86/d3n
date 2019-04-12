package de.ascendro.f4m.server.analytics.tracker;

import javax.inject.Inject;

import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.EventRecord;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.base.BaseEvent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;


public class TrackerImpl implements Tracker {
    private final AnalyticsDao analyticsDao;

    @Inject
    public TrackerImpl(AnalyticsDao analyticsDao) {
        this.analyticsDao = analyticsDao;
    }

    @Override
    public TrackerBuilders.EventBuilder getEventBuilder(ClientInfo clientInfo) {
    	TrackerBuilders.ContentBuilder contentBuilder = new TrackerBuilders.ContentBuilder();
    	if (clientInfo != null) {
    		contentBuilder.setUserId(clientInfo.getUserId())
	            .setApplicationId(clientInfo.getAppId())
                .setCountryCode(clientInfo.getOriginCountryAsString())
	            .setTenantId(clientInfo.getTenantId())
	            .setSessionIP(clientInfo.getIp());
    	}
        return new TrackerBuilders.EventBuilder().setAnalyticsContent(contentBuilder);
    }

    @Override
    public TrackerBuilders.AnonymousEventBuilder getAnonymousEventBuilder() {
        return new TrackerBuilders.AnonymousEventBuilder().setAnalyticsContent(
                new TrackerBuilders.ContentBuilder());
    }

    @Override
    public void add(EventRecord record) {
        this.analyticsDao.createAnalyticsEvent(record);
    }

    /**
     *
     *  Save new event in database for a specified client
     *
     * @param clientInfo
     * @param eventData
     */
    @Override
    public void addEvent(ClientInfo clientInfo, BaseEvent eventData) {
        TrackerBuilders.EventBuilder builder = getEventBuilder(clientInfo);
        builder.getAnalyticsContent().setEventData(eventData);
        EventRecord record = builder.build();
        fillMissingData(record.getAnalyticsContent());
        this.add(record);
    }

    @Override
    public void addAnonymousEvent(BaseEvent eventData) {
        TrackerBuilders.AnonymousEventBuilder builder = getAnonymousEventBuilder();
        builder.getAnalyticsContent().setEventData(eventData);
        EventRecord record = builder.build();
        fillMissingData(record.getAnalyticsContent());
        this.add(record);
    }

    @Override
    public void fillMissingData(EventContent eventContent) {
        BaseEvent eventData = eventContent.getEventData();
        if (eventData instanceof AdEvent) {
            analyticsDao.createFirstAppUsageRecord(eventContent);
            analyticsDao.createFirstGameUsageRecord(eventContent);
        } else if (eventData instanceof PlayerGameEndEvent) {
            analyticsDao.createNewAppPlayerRecord(eventContent);
            analyticsDao.createNewGamePlayerRecord(eventContent);
        }
    }


}
