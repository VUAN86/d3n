package de.ascendro.f4m.spark.analytics;

import static de.ascendro.f4m.spark.analytics.StreamCursor.cursorValue;
import static de.ascendro.f4m.spark.analytics.StreamCursor.updateCursor;
import static de.ascendro.f4m.spark.analytics.model.BaseEventMessage.TIMESTAMP_COLUMN;
import static de.ascendro.f4m.spark.analytics.model.BaseEventMessage.VALUE_COLUMN;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.max;
import static scala.collection.JavaConversions.mapAsJavaMap;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Level;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.DataStreamReader;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.streaming.StreamingQueryListener;
import org.apache.spark.sql.types.DataTypes;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.spark.analytics.config.AppConfig;
import de.ascendro.f4m.spark.analytics.log.ApplicationFileLogger;
import de.ascendro.f4m.spark.analytics.model.AdMessage;
import de.ascendro.f4m.spark.analytics.model.BaseEventMessage;
import de.ascendro.f4m.spark.analytics.model.GameEndMessage;
import de.ascendro.f4m.spark.analytics.model.InviteMessage;
import de.ascendro.f4m.spark.analytics.model.InvoiceMessage;
import de.ascendro.f4m.spark.analytics.model.MultiplayerGameEndMessage;
import de.ascendro.f4m.spark.analytics.model.PaymentMessage;
import de.ascendro.f4m.spark.analytics.model.PlayerGameEndMessage;
import de.ascendro.f4m.spark.analytics.model.PromoCodeMessage;
import de.ascendro.f4m.spark.analytics.model.RewardMessage;
import de.ascendro.f4m.spark.analytics.model.ShopInvoiceMessage;
import de.ascendro.f4m.spark.analytics.model.TombolaEndMessage;
import de.ascendro.f4m.spark.analytics.model.VoucherCountMessage;
import de.ascendro.f4m.spark.analytics.model.VoucherUsedMessage;

public class AnalyticsMQTTStreamApplication {
    private static AppConfig config;
    private static final Set<BaseEventMessage> eventMessages = new HashSet<>();

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AnalyticsMQTTStreamApplication.class);

    private static final String MEMORY_TABLE = "aggregates";
    private static final String SQL_QUERY_ON_MEMORY_TABLE = "select " + TIMESTAMP_COLUMN
            + ", " + VALUE_COLUMN + " from " + MEMORY_TABLE;

    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("SparkWriteJob").build());

    static final AtomicLong lastTimestamp = new AtomicLong(0);

    private AnalyticsMQTTStreamApplication() {
    }

    public static void main(String[] args) throws Exception {
        ApplicationFileLogger.initLogger(Level.toLevel(args.length>0?args[0]:null, Level.INFO));
        LOGGER.info("Starting AnalyticsMQTTStreamApplication application.");
        LOGGER.info("Args: {}", Arrays.toString(args));

        SparkConf sparkConf = new SparkConf().setAppName("F4M.AnalyticsMQTTStreamApplication");

        // check Spark configuration for master URL, set it to local if not configured
        if (!sparkConf.contains("spark.master")) {
            sparkConf.setMaster("local[4]");
        }

        final SparkSession spark = SparkSession.builder().config(sparkConf).getOrCreate();
        final SparkContext sc = spark.sparkContext();
        //sc.setLogLevel();
        final SQLContext sqlContext = spark.sqlContext();
        final String globalTempDB = spark.conf().get("spark.sql.globalTempDatabase");

        //Log current settings
        LOGGER.info("Global database location: {}", globalTempDB);
        mapAsJavaMap(sqlContext.getAllConfs()).entrySet()
                .forEach(entry -> LOGGER.info("{}: {}", entry.getKey(), entry.getValue()));

        config = new AppConfig(spark);

        try {
            spark.streams().addListener(new StreamingQueryListener() {
                @Override
                public void onQueryStarted(QueryStartedEvent queryStarted) {
                    LOGGER.debug("Query started: {}", queryStarted.id());
                }

                @Override
                public void onQueryTerminated(QueryTerminatedEvent queryTerminated) {
                    LOGGER.debug("Query terminated: {}", queryTerminated.id());
                }

                @Override
                public void onQueryProgress(QueryProgressEvent queryProgress) {
                    LOGGER.debug("Query made progress: {}", queryProgress.progress());
                }
            });

            new AnalyticsMQTTStreamApplication().startActiveMQListener(spark);
            LOGGER.info("Stopping AnalyticsMQTTStreamApplication.");
        } finally {
            spark.stop();
            sc.stop();
        }
    }

    private void startActiveMQListener(SparkSession spark) throws StreamingQueryException, SparkApplicationException {
        String format = config.getProperty(AppConfig.PROP_STORAGE_FORMAT);
        String path = config.getProperty(AppConfig.PROP_HDFS_PATH);

        eventMessages.add(new AdMessage(format, path));
        eventMessages.add(new GameEndMessage(format, path));
        eventMessages.add(new MultiplayerGameEndMessage(format, path));
        eventMessages.add(new PaymentMessage(format, path));
        eventMessages.add(new PlayerGameEndMessage(format, path));
        eventMessages.add(new RewardMessage(format, path));
        eventMessages.add(new TombolaEndMessage(format, path));
        eventMessages.add(new InvoiceMessage(format, path));
        eventMessages.add(new ShopInvoiceMessage(format, path));
        eventMessages.add(new VoucherUsedMessage(format, path));
        eventMessages.add(new VoucherCountMessage(format, path));
        eventMessages.add(new PromoCodeMessage(format, path));
        eventMessages.add(new InviteMessage(format, path));

        DataStreamReader dsr = spark.readStream();

        LOGGER.info("Starting broker on URL {} and topic {}", getBrokerUrl(), getTopic());

        Dataset<Row> lines = dsr
                .format("org.apache.bahir.sql.streaming.mqtt.MQTTStreamSourceProvider")
                .option("topic", getTopic())
                .load(getBrokerUrl());

        StreamingQuery query = lines.writeStream()
                .outputMode("append")
                .format("memory")
                .queryName(MEMORY_TABLE)
                .start();

        startFilePersistenceTask(spark);
        query.awaitTermination();
    }


    private void startFilePersistenceTask(SparkSession spark) throws SparkApplicationException {
        LOGGER.info("Cursor time: {}", cursorValue());
        lastTimestamp.set(cursorValue());

        service.scheduleAtFixedRate(() -> {
            Dataset<Row> agg = spark.sql(SQL_QUERY_ON_MEMORY_TABLE);
            long aggregateCount = agg.count();
            LOGGER.debug("ITEMS IN MEMORY: " + aggregateCount);

            Dataset<Row> events = agg.filter(col(TIMESTAMP_COLUMN).cast(DataTypes.TimestampType).$greater(new Timestamp(lastTimestamp.get())));
            LOGGER.debug("EVENTS TO BE PROCESSED: " + events.count());

            if (events.count() > 0) {
                lastTimestamp.set(events.select(max(TIMESTAMP_COLUMN)).first().getTimestamp(0).getTime());
                eventMessages.forEach(f -> f.persist(events));
            } else if (aggregateCount > 0) {
                lastTimestamp.set(agg.select(max(TIMESTAMP_COLUMN)).first().getTimestamp(0).getTime());
            }
            persistCursor(lastTimestamp);
        }, 0, config.getPropertyAsLong(AppConfig.PROP_PERSIST_INTERVAL), TimeUnit.MINUTES);
    }

    private synchronized void persistCursor(AtomicLong lastTimestamp) {
        try {
            updateCursor(lastTimestamp.get());
        } catch (SparkApplicationException sae) {
            LOGGER.error("Error on cursor update. Application shutting down.", sae);
            service.shutdown();
            System.exit(0);
        }
    }

    public static String getBrokerUrl() {
        return config.getProperty(AppConfig.PROP_BROKER_TRANSPORT)
                + "://" + config.getProperty(AppConfig.PROP_BROKER_URL)
                + ":" + config.getPropertyAsInteger(AppConfig.PROP_BROKER_PORT);
    }

    public static String getTopic() {
        return config.getProperty(AppConfig.PROP_TOPIC);
    }

}
