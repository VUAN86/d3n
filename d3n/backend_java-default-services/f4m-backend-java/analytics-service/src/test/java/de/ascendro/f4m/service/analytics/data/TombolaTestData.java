package de.ascendro.f4m.service.analytics.data;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.tombola.config.TombolaConfig;
import de.ascendro.f4m.service.tombola.dao.TombolaAerospikeDaoImpl;
import de.ascendro.f4m.service.tombola.model.Tombola;
import de.ascendro.f4m.service.tombola.model.UserTombolaInfo;
import de.ascendro.f4m.service.util.JsonLoader;


public class TombolaTestData {
    private JsonLoader jsonLoader;
    private JsonUtil jsonUtil;
    private AnalyticsDaoImpl analyticsDaoImpl;
    private Config config;

    private static final String USER_TOMBOLA_BIN_NAME = "usrTombola";
    private static final String TOMBOLA_BIN_NAME = "tombola";
    private static final String TOMBOLA_PURCHASED_COUNTER_BIN_NAME = "purchased";
    private static final String TOMBOLA_LOCKED_COUNTER_BIN_NAME = "locked";

    public static final String DEFAULT_TEST_TOMBOLA_ID = "1";

    public TombolaTestData(JsonLoader jsonLoader, JsonUtil jsonUtil,
                           AnalyticsDaoImpl analyticsDaoImpl, Config config) {
        this.jsonLoader = jsonLoader;
        this.jsonUtil = jsonUtil;
        this.analyticsDaoImpl = analyticsDaoImpl;
        this.config = config;
    }

    public List<Tombola> createTombolaTestData(String fileName) throws IOException {
        String set = config.getProperty(TombolaConfig.AEROSPIKE_TOMBOLA_SET);
        return insertTombolaTestData(fileName, set, TOMBOLA_BIN_NAME);
    }

    public List<UserTombolaInfo> createUserTombolaTestData(String fileName) throws IOException {
        String set = config.getProperty(TombolaConfig.AEROSPIKE_USER_TOMBOLA_SET);
        return insertUserTombolaTestData(fileName, set, USER_TOMBOLA_BIN_NAME);
    }

    private List<Tombola> insertTombolaTestData(String fileName, String set, String binName) throws IOException {
        List<Tombola> expectedTombolas = new ArrayList<>();
        String jsonVouchers = getPlainTextJsonFromResources(fileName);

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(jsonVouchers);

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String binValue = entry.getValue().toString();
            analyticsDaoImpl.createJson(set, key, binName, binValue);

            expectedTombolas.add(jsonUtil.fromJson(binValue, Tombola.class));
        }
        return expectedTombolas;
    }

    private List<UserTombolaInfo> insertUserTombolaTestData(String fileName, String set, String binName)
            throws IOException {
        List<UserTombolaInfo> expectedTombolas = new ArrayList<>();
        String userTombolaInfos = getPlainTextJsonFromResources(fileName);

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(userTombolaInfos);

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String binValue = entry.getValue().toString();
            analyticsDaoImpl.createJson(set, key, binName, binValue);
            expectedTombolas.add(jsonUtil.fromJson(binValue, UserTombolaInfo.class));
        }
        return expectedTombolas;
    }

    public String getPlainTextJsonFromResources(String path) throws IOException {
        return jsonLoader.getPlainTextJsonFromResources(path);
    }

    public void createPurchasedTickets(Injector injector, Tombola tombola) {
        TombolaAerospikeDaoImpl  tombolaAerospikeDaoImpl = injector.getInstance(TombolaAerospikeDaoImpl.class);
        IAerospikeClient aerospikeClient = injector.getInstance(AerospikeClientProvider.class).get();

        String set = config.getProperty(TombolaConfig.AEROSPIKE_TOMBOLA_TICKET_SET);
        Key counterKey = new Key("test", set, "tombola:" + DEFAULT_TEST_TOMBOLA_ID + ":counters");
        Bin counterBin1 = new Bin(TOMBOLA_PURCHASED_COUNTER_BIN_NAME, 1);
        Bin counterBin2 = new Bin(TOMBOLA_LOCKED_COUNTER_BIN_NAME, 1);
        aerospikeClient.put(null, counterKey, counterBin1, counterBin2);

        tombolaAerospikeDaoImpl.buyTickets(EventTestData.USER_ID, tombola.getName(), tombola.getImageId(), tombola.getId(), tombola.getTargetDate(),
                tombola.getTotalTicketsAmount(), 1, BigDecimal.valueOf(100), Currency.MONEY,
                "bundleImg", APP_ID, TENANT_ID);
    }

}
