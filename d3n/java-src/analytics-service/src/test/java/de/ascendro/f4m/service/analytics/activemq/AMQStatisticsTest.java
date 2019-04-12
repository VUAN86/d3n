package de.ascendro.f4m.service.analytics.activemq;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import javax.jms.JMSException;
import javax.jms.MapMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.analytics.AnalyticsScan;
import de.ascendro.f4m.service.analytics.AnalyticsTestConfig;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.config.Config;

public class AMQStatisticsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AMQStatisticsTest.class);

    private Config config;

    @Mock
    private MapMessage mapMessage;

    @Mock
    private IQueueBrokerManager embeddedActiveMQ;

    @Mock
    private NotificationCommon notificationUtil;

    @Mock
    private AnalyticsScan analyticsScan;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        config = new AnalyticsTestConfig();
    }

    @Test
    public void testAMQTriggers() throws JMSException {
        AMQStatisticsImpl amqStatistics = new AMQStatisticsImpl(embeddedActiveMQ, analyticsScan,
                notificationUtil, config);

        AMQStatisticsImpl.LOGGER = LOGGER;

        when(mapMessage.getInt(AMQStatisticsImpl.STORE_PERCENT_USAGE_PROPERTY)).thenReturn(91);
        when(mapMessage.getInt(AMQStatisticsImpl.MEMORY_PERCENT_USAGE_PROPERTY)).thenReturn(98);
        when(mapMessage.getLong(AMQStatisticsImpl.EXPIRED_COUNT_PROPERTY)).thenReturn(11L);

        assertTrue(amqStatistics.isMemoryFull(mapMessage));
        assertTrue(amqStatistics.isMessagesExpiring(mapMessage));
    }
}
