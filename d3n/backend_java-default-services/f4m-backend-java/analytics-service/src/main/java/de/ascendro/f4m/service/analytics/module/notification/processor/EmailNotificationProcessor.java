package de.ascendro.f4m.service.analytics.module.notification.processor;


import javax.inject.Inject;

import org.slf4j.Logger;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.notification.processor.base.INotificationProcessor;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;

public class EmailNotificationProcessor implements INotificationProcessor {
    @InjectLogger
    private static Logger LOGGER;

    protected final NotificationCommon notificationCommon;

    @Inject
    public EmailNotificationProcessor(NotificationCommon notificationCommon) {
        this.notificationCommon = notificationCommon;
    }

    @Override
    public void handleEvent(EventContent content) {
        try {
            processEvent(content);
        } catch (Exception e) {
            LOGGER.error("Error on event handle", e);
        }
    }

    @Override
    public void processEvent(EventContent content) throws Exception {
        //:TODO Implement the handling of calls that need to send email by calling sendEmailToUser()
    }

}
