package de.ascendro.f4m.spark.analytics;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.Level;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.hive.thriftserver.HiveThriftServer2;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.spark.analytics.config.AppConfig;
import de.ascendro.f4m.spark.analytics.log.ApplicationFileLogger;
import de.ascendro.f4m.spark.analytics.model.BaseEventMessage;

public class ThriftServerApplication {
    private static AppConfig config;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ThriftServerApplication.class);
    private static final List<ThriftStream> streamingQueries = new CopyOnWriteArrayList<>();

    protected ThriftServerApplication(AppConfig config) {
        ThriftServerApplication.config = config;
    }

    public static void main(String[] args) throws Exception {
        ApplicationFileLogger.initLogger(Level.toLevel(args.length>0?args[0]:null, Level.INFO));
        LOGGER.info("Starting ThriftServerApplication application.");

        SparkConf sparkConf = new SparkConf().setAppName("F4M.ThriftServerApplication");
        sparkConf.set("spark.sql.hive.thriftServer.singleSession", "true");
        // check Spark configuration for master URL, set it to local if not configured
        if (!sparkConf.contains("spark.master")) {
            sparkConf.setMaster("local[4]");
        }

        final SparkSession spark = SparkSession.builder()
                .enableHiveSupport().config(sparkConf).getOrCreate();

        final SQLContext sqlContext = spark.sqlContext();

        new ThriftServerApplication(new AppConfig(spark)).start(spark, sqlContext);
    }

    public int getStreamingQueriesCount() {
        return streamingQueries.size();
    }

    private void start(SparkSession spark, SQLContext sqlContext)
            throws AnalysisException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        //get events data from HDFS
        loadStreamQueries(sqlContext);
        startFileCheckTask(sqlContext);

        spark.catalog().listDatabases().show(false);
        spark.catalog().listTables("default").show();

        HiveThriftServer2.startWithContext(sqlContext);
        streamingQueries.forEach(this::awaitTermination);
    }

    protected void loadStreamQueries(SQLContext sqlContext) throws AnalysisException,
            NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        String format = config.getProperty(AppConfig.PROP_STORAGE_FORMAT);
        String path = config.getProperty(AppConfig.PROP_HDFS_PATH);
        String tables = config.getProperty(AppConfig.PROP_EXPOSED_TABLES);

        if (StringUtils.isNotBlank(tables)) {
            Set<String> tableSet = new HashSet<>(Arrays.asList(tables.split(",")));
            for (Class<? extends BaseEventMessage> clazz : MessageMapper.getEventMessageSet()) {
                BaseEventMessage message = clazz.getDeclaredConstructor(String.class, String.class).newInstance(format, path);
                if (tableSet.contains(message.getEventTableName())) {
                    streamingQueries.add(initStream(sqlContext, message));
                }
            }
        }
    }

    private void startFileCheckTask(SQLContext sqlContext) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    for (ThriftStream thriftStream : streamingQueries) {
                        if (thriftStream.getStreamStatus().equals(StreamStatus.FILE_MISSING)) {
                            loadStream(sqlContext, thriftStream);
                        }
                    }
                } catch (AnalysisException e) {
                    LOGGER.error("Error loading stream", e);
                }
            }
        };

        new Timer().schedule(timerTask, 1000L, 1L * 60 * 1000);
    }

    private void awaitTermination(final ThriftStream thriftStream) {
        StreamingQuery streamingQuery = thriftStream.getStreamingQuery();
        if (thriftStream.getStreamStatus().equals(StreamStatus.RUNNING)) {
            try {
                String queryName = streamingQuery.name();
                LOGGER.info("Stream {} started and loaded {} rows",
                        queryName, streamingQuery.lastProgress() != null ? streamingQuery.lastProgress().numInputRows() : 0);
                streamingQuery.awaitTermination();
                LOGGER.info("Stream {} finalized", queryName);
            } catch (StreamingQueryException ex) {
                LOGGER.error("Error on stream {}", streamingQuery.name());
                throw new RuntimeException(ex);
            }
        }
    }

    private synchronized ThriftStream initStream(SQLContext sqlContext, BaseEventMessage event) throws AnalysisException {
        ThriftStream thriftStream = new ThriftStream(event);
        loadStream(sqlContext, thriftStream);
        return thriftStream;
    }


    private synchronized void loadStream(SQLContext sqlContext, ThriftStream thriftStream) throws AnalysisException {
        BaseEventMessage event = thriftStream.getEvent();
        boolean fileExists = false;
        try {
            FileSystem fileSystem = org.apache.hadoop.fs.FileSystem.get(sqlContext.sparkContext().hadoopConfiguration());
            fileExists = fileSystem.exists(new org.apache.hadoop.fs.Path(event.getFilePath()));
        } catch (IOException e) {
            LOGGER.error("Error accessing hadoop file system", e);
        }

        if (fileExists) {
            Dataset<Row> dataSet = sqlContext.readStream().format(event.getFormat())
                    .schema(event.getOutputSchema()).load(event.getFilePath());
            LOGGER.info("Start streaming table {}", event.getEventTableName());
            dataSet.schema().printTreeString();
            thriftStream.setStreamingQuery(dataSet.writeStream()
                    .outputMode("append")
                    .format("memory")
                    .queryName(event.getEventTableName())
                    .start());
            thriftStream.setStreamStatus(StreamStatus.RUNNING);
        } else {
            thriftStream.setStreamStatus(StreamStatus.FILE_MISSING);
            LOGGER.warn("File {} dose not exists. Stream for table {} is not started.", event.getFilePath(), event.getEventTableName());
        }
    }

    @SuppressWarnings("unused")
    private void loadTempTable(SQLContext sqlContext, BaseEventMessage event) throws AnalysisException {
        Dataset<Row> dataSet = sqlContext.read().format(event.getFormat())
                .schema(event.getOutputSchema()).load(event.getFilePath());
        LOGGER.info("Loaded table {}. Total rows: {}", event.getEventTableName(), dataSet.count());
        dataSet.schema().printTreeString();

        sqlContext.registerDataFrameAsTable(dataSet.select(event.getOutputSelectColumns().toArray(new Column[event.getOutputSelectColumns().size()])),
                event.getEventTableName());
        sqlContext.cacheTable(event.getEventTableName());
        sqlContext.sql("select count(*) from " + event.getEventTableName()).collect();
    }

}
