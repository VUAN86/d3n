package de.ascendro.f4m.spark.analytics;


import static de.ascendro.f4m.spark.analytics.data.EventTestData.createPlayerGameEndEvent;
import static de.ascendro.f4m.spark.analytics.model.BaseEventMessage.VALUE_COLUMN;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.EventRecord;
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
import scala.Tuple2;

public class AnalyticsMQTTStreamTest {
    private JavaSparkContext jsc;
    private SparkSession spark;
    private SQLContext sqlContext;
    private Gson gson;
    private static final double DELTA = 1e-15;

    private static final int START_EVENT_FIELDS = 7;

    @Before
    public void init() throws IllegalArgumentException, IOException, URISyntaxException {
        gson = new GsonBuilder().serializeNulls().create();
        SparkConf conf = new SparkConf();
        conf.setMaster("local");
        conf.setAppName("junit");

        spark = SparkSession.builder().master("local[*]").appName("junit").getOrCreate();
        jsc = new JavaSparkContext(spark.sparkContext());
        sqlContext = spark.sqlContext();

        URL resource = AnalyticsMQTTStreamTest.class.getResource("/hadoop/");
        if (resource != null) {
            System.setProperty("hadoop.home.dir", new File(resource.toURI()).getAbsolutePath());
        }
    }

    @After
    public void stop() {
        if (spark!=null) {
            spark.stop();
        }
        if (jsc!=null) {
            jsc.stop();
        }
    }

    @Test
    public void testContext() {
        final List<Integer> nums = new ArrayList<Integer>();
        nums.add(3);
        nums.add(4);
        nums.add(2);
        JavaRDD<Integer> rdd = jsc.parallelize(nums, 1);
        Assert.assertEquals(3, rdd.count());
    }

    @Test
    public void testGameEndMessage() throws URISyntaxException {
        GameEndMessage message = new GameEndMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/game_end.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "14878556985250bd5b2e5-f8a7-4e9b-a510-68dd5e228ffa");
        Assert.assertEquals(events.first().getLong(START_EVENT_FIELDS), 11);
    }

    @Test
    public void testPaymentMessage() throws URISyntaxException {
        PaymentMessage message = new PaymentMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/payment.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "234325547333564b15f79ba-73dd-4a4c-b1ed-8b4a87d99fcc");
        Assert.assertEquals(events.first().getDouble(START_EVENT_FIELDS + 10), 200.5d, DELTA);
    }

    @Test
    public void testRewardMessage() throws URISyntaxException {
        RewardMessage message = new RewardMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/reward.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "1489132260280106bbb98-e88c-428a-aa35-aa17bef35cc4");
        Assert.assertEquals(events.first().getLong(START_EVENT_FIELDS + 4), 500);
    }

    @Test
    public void testVoucherUsedMessage() throws URISyntaxException {
        VoucherUsedMessage message = new VoucherUsedMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/voucher_used.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "2546325556891da980b00-b9d0-41a1-b3f5-95c4da33a561");
        Assert.assertEquals(events.first().getLong(START_EVENT_FIELDS), 13);
        Assert.assertEquals(events.first().getString(START_EVENT_FIELDS + 1), "b00-b9d0");
    }

    @Test
    public void testVoucherCountMessage() throws URISyntaxException {
        VoucherCountMessage message = new VoucherCountMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/voucher_count.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "2546325556891da980b00-b9d0-41a1-b3f5-95c4da33a561");
        Assert.assertEquals(events.first().getLong(START_EVENT_FIELDS), 13);
        Assert.assertEquals(events.first().getLong(START_EVENT_FIELDS + 1), 50);
    }

    @Test
    public void testTombolaEndMessage() throws URISyntaxException {
        TombolaEndMessage message = new TombolaEndMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/tombola_end.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "1489146325556da980b00-b9d0-41a1-b3f5-95c4da33a561");
        Assert.assertEquals(events.first().getLong(START_EVENT_FIELDS), 7);
    }

    @Test
    public void testAdMessage() throws URISyntaxException {
        AdMessage message = new AdMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/advertisement.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "1488257582925e1b585e6-eaa3-438b-b209-bc580cfcf969");
        Assert.assertEquals(events.first().getString(START_EVENT_FIELDS + 1), "provider_12_advertisement_1.json");
        Assert.assertTrue(events.first().getBoolean(START_EVENT_FIELDS + 2));
    }

    @Test
    public void testMultiplayerGameEndMessage() throws URISyntaxException {
        MultiplayerGameEndMessage message = new MultiplayerGameEndMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/multiplayer_game_end.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "14883009600931a988683-c318-477e-84e3-560d7e91edc7");
        Assert.assertEquals(events.first().getString(START_EVENT_FIELDS + 2), "1489079199969a3897882-f7e0-41e6-a564-6422e99bb809");
    }

    @Test
    public void testPlayerGameEndMessage() throws URISyntaxException {
        PlayerGameEndMessage message = new PlayerGameEndMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/player_game_end.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "2334432433564b15f79ba-73dd-4a4c-b1ed-8b4a87d99bcc");
        Assert.assertEquals(events.first().getLong(START_EVENT_FIELDS+ 1), 7);
    }

    @Test
    public void testInvoiceMessage() throws URISyntaxException {
        InvoiceMessage message = new InvoiceMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/invoice.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "2488556682925e1b585e6-eaa3-438b-b209-bc580c23gaa9");
        Assert.assertEquals(events.first().getString(START_EVENT_FIELDS), "ENTRY_FEE");
        Assert.assertEquals(events.first().getDouble(START_EVENT_FIELDS + 1), 10.56D, DELTA);
        Assert.assertEquals(events.first().getDouble(START_EVENT_FIELDS + 6), 2.2d, DELTA);
    }

    @Test
    public void testShopInvoiceMessage() throws URISyntaxException {
        ShopInvoiceMessage message = new ShopInvoiceMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/shopInvoice.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "2488556682925e1b585e6-eaa3-438b-b209-bc580c23gaa9");
        Assert.assertEquals(events.first().getString(START_EVENT_FIELDS), "article_1234");
        Assert.assertEquals(events.first().getString(START_EVENT_FIELDS + 1), "art_no_566");
        Assert.assertEquals(events.first().getString(START_EVENT_FIELDS + 2), "GooglePixel");
        Assert.assertEquals(events.first().getDouble(START_EVENT_FIELDS + 3), 500.00, DELTA);
        Assert.assertEquals(events.first().getDouble(START_EVENT_FIELDS + 4), 450.00, DELTA);
        Assert.assertEquals(events.first().getDouble(START_EVENT_FIELDS + 5), 10000.00, DELTA);
        Assert.assertEquals(events.first().getDouble(START_EVENT_FIELDS + 6), 300.00, DELTA);
    }

    @Test
    public void testInviteMessage() throws URISyntaxException {
        InviteMessage message = new InviteMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/invite.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(events.first().getString(1), "5346332442925e1b585e6-eaa3-438b-b209-bc580c23gaa9");
        Assert.assertEquals(events.first().getLong(START_EVENT_FIELDS), 1);
        Assert.assertEquals(events.first().getBoolean(START_EVENT_FIELDS + 1), true);

        Assert.assertEquals(1, events.count());
    }

    @Test
    public void testPromoCodeMessage() throws URISyntaxException {
        PromoCodeMessage message = new PromoCodeMessage("json", null);
        URL fileURL = AnalyticsMQTTStreamTest.class.getResource("/json/promocode.json");

        Dataset<Row> events = getEvents(fileURL, message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "5346332260280106bbb98-e88c-428a-aa35-aa17bef3fs4");
        Assert.assertEquals(events.first().getString(START_EVENT_FIELDS), "XCLC9B");
        Assert.assertEquals(events.first().getBoolean(START_EVENT_FIELDS + 2), true);
    }

    @Test
    public void testPlayerGameEndMessageFromEventRecord()
            throws URISyntaxException, ClassNotFoundException, IllegalAccessException {
        PlayerGameEndMessage message = new PlayerGameEndMessage("json", null);
        Dataset<Row> events = getEvents(createPlayerGameEndEvent(), message);

        Assert.assertEquals(1, events.count());
        Assert.assertEquals(events.first().getString(1), "2334432433564b15f79ba-73dd-4a4c-b1ed-8b4a87d99bcc");
        Assert.assertEquals(events.first().getDouble(START_EVENT_FIELDS + 14), 77D, DELTA);
    }

    private Dataset<Row> getEvents(URL fileURL, BaseEventMessage message) throws URISyntaxException {
        JavaRDD<Tuple2<String, String>> bulkRDD = jsc.sc()
                .wholeTextFiles(new File(fileURL.toURI()).getAbsolutePath(), 2).toJavaRDD();

        JavaRDD<String> jsonRDD = bulkRDD.map(stringStringTuple2 -> {
            return stringStringTuple2._2();
        }).map(s -> {
            return s.replaceAll("\\s+", "");
        });

        //check resource data read
        Assert.assertEquals(1, jsonRDD.count());
        //check structure integrity
        Assert.assertTrue(checkIntegrity(jsonRDD, message));

        return sqlContext.read().json(jsonRDD)
                .select(message.getInputSelectColumns().toArray(new Column[message.getInputSelectColumns().size()]));
    }

    private StructType getStreamSchema(BaseEventMessage message) {
        List<StructField> fields = new ArrayList<>();

        fields.add(DataTypes.createStructField(VALUE_COLUMN, DataType.fromJson(message.getInputSchema().json()), true));

        return DataTypes.createStructType(fields);
    }

    private Dataset<Row> getEvents(EventRecord record, BaseEventMessage message)
            throws ClassNotFoundException, IllegalAccessException {
        JavaRDD<String> jsonRDD = createEventRecordRDD(record);

        return sqlContext.read().json(jsonRDD)
                .select(message.getInputSelectColumns().toArray(new Column[message.getInputSelectColumns().size()]));
    }

    private JavaRDD<String> createEventRecordRDD(EventRecord record)
            throws ClassNotFoundException, IllegalAccessException {
        EventContent eventContent = record.getAnalyticsContent();

        Field[] fields = Class.forName(eventContent.getEventType()).getDeclaredFields();
        for (Field field : fields) {
            JsonObject jsonObject = eventContent.getEventData().getJsonObject();
            field.setAccessible(true);
            if (field.get(null) instanceof String &&
                    !Modifier.isTransient(field.getModifiers())) {
                String jsonProperty = (String) field.get(null);
                if (jsonObject.get(jsonProperty) == null) {
                    jsonObject.add(jsonProperty, null);
                }
            }
        }
        return jsc.parallelize(Arrays.asList(
                new StringBuilder()
                        .append("{\"value\":")
                        .append(gson.toJson(eventContent))
                        .append("}").toString()));
    }

    private boolean checkIntegrity(JavaRDD<String> jsonRDD, BaseEventMessage message) {
        char[] jsonRDDCharArray = sqlContext.read().json(jsonRDD)
                .first().schema().simpleString().toCharArray();
        Arrays.sort(jsonRDDCharArray);
        char[] messageObjectCharArray = getStreamSchema(message).simpleString().toCharArray();
        Arrays.sort(messageObjectCharArray);

        return Arrays.equals(jsonRDDCharArray, messageObjectCharArray);
    }
}
