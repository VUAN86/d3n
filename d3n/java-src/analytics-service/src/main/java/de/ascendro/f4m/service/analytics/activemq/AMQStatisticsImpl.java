package de.ascendro.f4m.service.analytics.activemq;

import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.MapMessage;

import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.service.analytics.AnalyticsScan;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.usermessage.translation.Messages;

public class AMQStatisticsImpl implements AMQStatistics {
    @InjectLogger
    protected static Logger LOGGER;

    protected final Config config;
    protected final IQueueBrokerManager embeddedActiveMQ;
    protected final NotificationCommon notificationUtil;
    protected final AnalyticsScan analyticsScan;
    protected final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("AMQStatisticsImpl").build());

    protected static final String STORE_PERCENT_USAGE_PROPERTY = "storePercentUsage";
    protected static final String MEMORY_PERCENT_USAGE_PROPERTY = "memoryPercentUsage";
    protected static final String EXPIRED_COUNT_PROPERTY = "expiredCount";
    protected static final AtomicLong expiredCount = new AtomicLong(0);

    private static final int STORE_PERCENT_USAGE = 90;
    private static final int MEMORY_PERCENT_USAGE = 95;

    @Inject
    public AMQStatisticsImpl(IQueueBrokerManager embeddedActiveMQ, AnalyticsScan analyticsScan,
                             NotificationCommon notificationUtil, Config config) {
        this.embeddedActiveMQ = embeddedActiveMQ;
        this.analyticsScan = analyticsScan;
        this.notificationUtil = notificationUtil;
        this.config = config;
    }


    private String logMapMessage(MapMessage mapMessage) throws JMSException {
        StringBuilder messageBuilder = new StringBuilder();
        for (Enumeration<?> e = mapMessage.getMapNames(); e.hasMoreElements(); ) {
            String name = e.nextElement().toString();
            messageBuilder.append(name).append(" = ").append(mapMessage.getObject(name)).append("\\n");
            LOGGER.debug("{} = {}", name, mapMessage.getObject(name));
        }

        return messageBuilder.toString();
    }

    private void init() {
        try {
            MapMessage mapMessage = embeddedActiveMQ.gatherAMQStatistics();
            expiredCount.set(mapMessage.getLong(EXPIRED_COUNT_PROPERTY));
        } catch (JMSException jmse) {
            LOGGER.error("Error gathering initial AMQ statistics", jmse);
        }
    }

    @Override
    public void startAMQStatistics() {
        init();
        String[] subjectParams = new String[]{ config.getProperty(AnalyticsConfig.EXPIRED_MESSAGES_TRIGGER_TO_SUSPEND_SCAN)};
        service.scheduleAtFixedRate(() -> {
            MapMessage mapMessage;
            try {
                if (embeddedActiveMQ.isStarted()) {
                    mapMessage = embeddedActiveMQ.gatherAMQStatistics();
                    if (isMemoryFull(mapMessage)) {
                        if (analyticsScan.isRunning()) {
                            analyticsScan.suspend();
                            notificationUtil.sendEmailToAdmin(Messages.AMQ_MEMORY_ERROR_TO_ADMIN_SUBJECT, null,
                                    logMapMessage(mapMessage), null);
                        }
                    } else if (isMessagesExpiring(mapMessage)) {
                        analyticsScan.suspend();
                        notificationUtil.sendEmailToAdmin(Messages.AMQ_EXPIRED_MESSAGES_ERROR_TO_ADMIN_SUBJECT, subjectParams,
                                logMapMessage(mapMessage), null);
                    } else {
                        analyticsScan.resume();
                    }
                }
            } catch (JMSException jmse) {
                LOGGER.error("Error gathering AMQ statistics", jmse);
            } finally {
                mapMessage = null;
            }
        }, 0, 30, TimeUnit.MINUTES);
    }

    protected boolean isMemoryFull(MapMessage mapMessage) throws JMSException {
        if (mapMessage.getInt(STORE_PERCENT_USAGE_PROPERTY) > STORE_PERCENT_USAGE
                || mapMessage.getInt(MEMORY_PERCENT_USAGE_PROPERTY) > MEMORY_PERCENT_USAGE) {
            return true;
        }
        return false;
    }

    protected boolean isMessagesExpiring(MapMessage mapMessage) throws JMSException {
        LOGGER.debug("Diff for expire messages {}", mapMessage.getLong(EXPIRED_COUNT_PROPERTY) - expiredCount.get());
        if (mapMessage.getLong(EXPIRED_COUNT_PROPERTY) - expiredCount.get() >
                config.getPropertyAsInteger(AnalyticsConfig.EXPIRED_MESSAGES_TRIGGER_TO_SUSPEND_SCAN)) {
            expiredCount.set(mapMessage.getLong(EXPIRED_COUNT_PROPERTY));
            return true;
        }
        return false;
    }

    @Override
    public void stopAMQStatistics() {
        if (!service.isShutdown()) {
            service.shutdownNow();
        }
    }



}
