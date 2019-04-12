package de.ascendro.f4m.spark.analytics.config;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.spark.sql.SparkSession;
import org.slf4j.LoggerFactory;

public class AppConfig {

    protected final Properties properties = createProperties();
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    public static final String BASE_PROPERTY = "spark.analytics.";

    public static final String PROP_BROKER_URL = BASE_PROPERTY + "broker.url";
    public static final String PROP_BROKER_TRANSPORT = BASE_PROPERTY + "broker.transport";
    public static final String PROP_BROKER_PORT = BASE_PROPERTY + "broker.port";
    public static final String PROP_TOPIC = BASE_PROPERTY + "topic";
    public static final String PROP_HDFS_PATH = BASE_PROPERTY + "hdfs.path";
    public static final String PROP_PERSIST_INTERVAL = BASE_PROPERTY + "hdfs.persist.interval";
    public static final String PROP_STORAGE_FORMAT = BASE_PROPERTY + "file.format";
    public static final String PROP_EXPOSED_TABLES = BASE_PROPERTY + "thrift.exposed.tables";


    private static final String DEFAULT_BROKER_URL = "172.31.29.229";
    private static final String DEFAULT_BROKER_TRANSPORT = "tcp";
    private static final int DEFAULT_BROKER_PORT = 9456;
    private static final String DEFAULT_STORAGE_FORMAT = "json";

    private static final String BASE_TOPIC = "f4m/analytics/ext";
    private static final String DEFAULT_TOPIC =  BASE_TOPIC + "/spark";

    private static final String DEFAULT_HDFS_PATH = "/user/spark/warehouse/";

    private static final int DEFAULT_PERSIST_INTERVAL = 30;

    private static final String DEFAULT_EXPOSED_TABLES = "advertisement,invoice,reward";


    public AppConfig(SparkSession spark) {
        setProperty(PROP_BROKER_URL, DEFAULT_BROKER_URL);
        setProperty(PROP_BROKER_TRANSPORT, DEFAULT_BROKER_TRANSPORT);
        setProperty(PROP_BROKER_PORT, DEFAULT_BROKER_PORT);
        setProperty(PROP_TOPIC, DEFAULT_TOPIC);
        setProperty(PROP_HDFS_PATH, DEFAULT_HDFS_PATH);
        setProperty(PROP_PERSIST_INTERVAL, DEFAULT_PERSIST_INTERVAL);
        setProperty(PROP_STORAGE_FORMAT, DEFAULT_STORAGE_FORMAT);
        setProperty(PROP_EXPOSED_TABLES, DEFAULT_EXPOSED_TABLES);

        loadProperties(spark);
    }

    protected Properties createProperties() {
        final Properties prop = new Properties();
        prop.putAll(System.getProperties());
        return prop;
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public void setProperty(String key, Number value) {
        properties.setProperty(key, String.valueOf(value));
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public Integer getPropertyAsInteger(String key) {
        final String propertyAsString = properties.getProperty(key);

        if (!StringUtils.isBlank(propertyAsString)) {
            return Integer.valueOf(propertyAsString);
        } else {
            return null;
        }
    }

    public Long getPropertyAsLong(String key) {
        final String propertyAsString = properties.getProperty(key);

        if (!StringUtils.isBlank(propertyAsString)) {
            return Long.valueOf(propertyAsString);
        } else {
            return null;
        }
    }

    protected void loadProperties(SparkSession spark) {
        properties.stringPropertyNames().forEach(p -> {
            if (spark.conf().contains(p)) {
                String value = spark.conf().get(p);
                if (StringUtils.isNotBlank(value)) {
                    LOGGER.info("Loading property: {} with value: {}", p, value);
                    setProperty(p, value);
                }
            }
        });
    }
}
