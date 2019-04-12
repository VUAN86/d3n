package de.ascendro.f4m.service.analytics;

import de.ascendro.f4m.server.analytics.EventContent;

public interface IMessageProcessor {
    void handleEvent(EventContent content);
    void processEvent(EventContent content) throws Exception;
}
