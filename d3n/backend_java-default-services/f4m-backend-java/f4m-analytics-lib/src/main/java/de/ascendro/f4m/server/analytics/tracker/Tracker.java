package de.ascendro.f4m.server.analytics.tracker;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.EventRecord;
import de.ascendro.f4m.server.analytics.model.base.BaseEvent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;


public interface Tracker {
    TrackerBuilders.EventBuilder getEventBuilder(ClientInfo clientInfo);

    TrackerBuilders.AnonymousEventBuilder getAnonymousEventBuilder();

    void add(EventRecord record);

    void addEvent(ClientInfo clientInfo, BaseEvent eventData);
    
    void addAnonymousEvent(BaseEvent eventData);

    void fillMissingData(EventContent eventContent);
}
