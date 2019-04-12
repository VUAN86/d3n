package de.ascendro.f4m.service.analytics.config;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.BaseUpdater;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.tombola.config.TombolaConfig;

public class AnalyticsConfig extends F4MConfigImpl {

    private static final String EVENT_PROPERTY_BASE = "analytics.";
    private static final String ACTIVE_MQ_PROPERTY_NAME_PREFIX = EVENT_PROPERTY_BASE + "activemq.";
    public static final String ACTIVE_MQ_PORT = ACTIVE_MQ_PROPERTY_NAME_PREFIX + "port";
    public static final String ACTIVE_MQ_NETWORK_DISCOVERY_PROTOCOL = ACTIVE_MQ_PROPERTY_NAME_PREFIX
            + "network.discovery.protocol";
    public static final String ACTIVE_MQ_INTERNAL_COMMUNICATION_PROTOCOL = ACTIVE_MQ_PROPERTY_NAME_PREFIX
            + "transport.internal.protocol";
    public static final String MESSAGE_DURABLE_SUBSCRIBEE_OFLINE_TIMEOUT = ACTIVE_MQ_PROPERTY_NAME_PREFIX
            + "subscriber.durable.offlineDurableSubscriberTimeout";
    public static final String MESSAGE_PRODUCER_MESSAGE_TIME_TO_LIVE = ACTIVE_MQ_PROPERTY_NAME_PREFIX
            + "producer.message.timeToLive";

    public static final String ACTIVE_MQ_EXTERNAL_COMMUNICATION_PROTOCOL = ACTIVE_MQ_PROPERTY_NAME_PREFIX
            + "transport.external.protocol";
    public static final String ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST = ACTIVE_MQ_PROPERTY_NAME_PREFIX
            + "transport.external.host";
    public static final String ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST_DEFAULT_VALUE = "0.0.0.0";
    public static final String ACTIVE_MQ_PERSISTENT = ACTIVE_MQ_PROPERTY_NAME_PREFIX
            + "persistent";

    public static final String WILDCARD_SEPARATOR = ACTIVE_MQ_PROPERTY_NAME_PREFIX + "wildcard.separator.actual";

    private static final String MYSQL_PROPERTY_NAME_PREFIX = EVENT_PROPERTY_BASE + "mysql.";
    public static final String MYSQL_DATABASE_URL =MYSQL_PROPERTY_NAME_PREFIX + "database.url";
    public static final String MYSQL_DATABASE_USER =MYSQL_PROPERTY_NAME_PREFIX + "database.user";
    public static final String MYSQL_DATABASE_PASSWORD =MYSQL_PROPERTY_NAME_PREFIX + "database.password";
    public static final String MYSQL_DATABASE_MAX_CONNECTION =MYSQL_PROPERTY_NAME_PREFIX + "database.maxConnections";
    public static final String MYSQL_BATCH_SIZE =MYSQL_PROPERTY_NAME_PREFIX + "batchSize";
    public static final String MYSQL_UPDATE_TRIGGER_IN_SECONDS =MYSQL_PROPERTY_NAME_PREFIX + "updateTrigger";

    public static final String EXPIRED_MESSAGES_TRIGGER_TO_SUSPEND_SCAN =MYSQL_PROPERTY_NAME_PREFIX + "expired.trigger.value";

    public static final String MONTHLY_BONUS_NUMBER_OF_FRIENDS = EVENT_PROPERTY_BASE + "monthly.bonus.invites";
    public static final String MONTHLY_BONUS_VALUE = EVENT_PROPERTY_BASE + "monthly.bonus.value";
    public static final String MONTHLY_BONUS_CURRENCY = EVENT_PROPERTY_BASE + "monthly.bonus.currency";

    public static final String AEROSPIKE_SCAN_DELAY = EVENT_PROPERTY_BASE + "aerospike.scan.delay";

    public AnalyticsConfig() {
        super(new AerospikeConfigImpl(), new TombolaConfig(), new ElasticConfigImpl());

        final String jettyPort = getProperty(F4MConfigImpl.JETTY_SSL_PORT);
        initDefaultActiveMqPort(jettyPort);

        setProperty(ACTIVE_MQ_NETWORK_DISCOVERY_PROTOCOL, "static");
        setProperty(ACTIVE_MQ_INTERNAL_COMMUNICATION_PROTOCOL, "vm");
        setProperty(EXPIRED_MESSAGES_TRIGGER_TO_SUSPEND_SCAN, 10);
        setProperty(MESSAGE_DURABLE_SUBSCRIBEE_OFLINE_TIMEOUT, 5 * 60 * 1000);
        setProperty(MESSAGE_PRODUCER_MESSAGE_TIME_TO_LIVE, 5 * 60 * 1000);
        setProperty(MYSQL_DATABASE_MAX_CONNECTION, 10);
        setProperty(MYSQL_BATCH_SIZE, BaseUpdater.BATCH_SIZE);
        setProperty(MYSQL_UPDATE_TRIGGER_IN_SECONDS, BaseUpdater.UPDATE_TRIGGER);
        setProperty(ACTIVE_MQ_PERSISTENT, true);

        setProperty(AEROSPIKE_SCAN_DELAY, 60);

        setProperty(ACTIVE_MQ_EXTERNAL_COMMUNICATION_PROTOCOL, "mqtt");
        setProperty(ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST, ACTIVE_MQ_EXTERNAL_COMMUNICATION_HOST_DEFAULT_VALUE);

        setProperty(WILDCARD_SEPARATOR, "/");

        setProperty(ACTIVE_MQ_PERSISTENT, true);
        setProperty(ACTIVE_MQ_PERSISTENT, true);

        setProperty(MONTHLY_BONUS_NUMBER_OF_FRIENDS, 10);
        setProperty(MONTHLY_BONUS_VALUE, 1);
        setProperty(MONTHLY_BONUS_CURRENCY, Currency.BONUS.name());

        loadProperties();
    }

    private void initDefaultActiveMqPort(String jettyPort) {
        final int firstJettyPortDigit = Integer.parseInt(Character.toString(jettyPort.charAt(0)));
        final String activeMqPortFirstDigit = String.valueOf(firstJettyPortDigit + 1);
        final String activeMqPort = activeMqPortFirstDigit + jettyPort.substring(1);

        setProperty(ACTIVE_MQ_PORT, activeMqPort);
    }
}
