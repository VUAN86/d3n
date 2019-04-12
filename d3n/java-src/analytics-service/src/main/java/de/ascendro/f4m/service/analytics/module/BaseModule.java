package de.ascendro.f4m.service.analytics.module;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.slf4j.Logger;

import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;

public abstract class BaseModule implements AnalyticMessageListener {

    @InjectLogger
    protected static Logger LOGGER;

    private static final String ERROR_MESSAGE_SUBJECT = "Error processing message in analytics module";
    private static final String ERROR_MESSAGE_BODY = "Error in module: {} /n Message content: {} /n Error message: {}";

    protected final NotificationCommon notificationUtil;

    @Inject
    protected BaseModule(NotificationCommon notificationUtil) {
        this.notificationUtil = notificationUtil;
    }

    @Override
    public void onMessage(Message message) {
        final TextMessage textMessage = (TextMessage) message;
        try {
            onModuleMessage(textMessage);
        } catch (Exception ex) {

            String[] bodyParams =  new String[3];
            bodyParams[0] = this.getClass().getSimpleName();
            bodyParams[1] = message.toString();
            bodyParams[2] = ex.getMessage();
            notificationUtil.sendEmailToAdmin(ERROR_MESSAGE_SUBJECT, null,
                    ERROR_MESSAGE_BODY, null);

            LOGGER.error("Error on message type", ex);
        }
    }

    protected abstract void onModuleMessage(Message message);
}
