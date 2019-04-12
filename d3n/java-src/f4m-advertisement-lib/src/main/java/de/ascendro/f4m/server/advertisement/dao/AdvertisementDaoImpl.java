package de.ascendro.f4m.server.advertisement.dao;

import com.google.inject.Inject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.advertisement.config.AdvertisementConfig;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

public class AdvertisementDaoImpl extends AerospikeOperateDaoImpl<AdvertisementPrimaryKeyUtil>
        implements AdvertisementDao {

    public static final String COUNTER_BIN_NAME = "counter";
    public static final String ADS_BIN_NAME = "advertisements";
    public static final String INSTANCE_KEY_PREFIX = "gameInstance";
    public static final char KEY_ITEM_SEPARATOR = ':';

    @Inject
    public AdvertisementDaoImpl(Config config, AdvertisementPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
                                AerospikeClientProvider aerospikeClientProvider) {
        super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
    }

    @Override
    public void addAdvertisementShown(String gameInstanceId, String advertisementBlobKey){

        final String gameInstanceIdKey = INSTANCE_KEY_PREFIX + KEY_ITEM_SEPARATOR + gameInstanceId;
        final String resultsJson = readJson(getSet(), gameInstanceIdKey, ADS_BIN_NAME);
        List<String> advertisementBlobKeys;
        if (StringUtils.isNotBlank(resultsJson)) {
            advertisementBlobKeys = jsonUtil.fromJson(resultsJson, List.class);
        } else {
            advertisementBlobKeys = new LinkedList();
        }
        advertisementBlobKeys.add(advertisementBlobKey);
        createOrUpdateJson(getSet(), gameInstanceIdKey, ADS_BIN_NAME,
                (existing, wp) -> jsonUtil.toJson(advertisementBlobKeys));
    }

    @Override
    public Integer getAdvertisementCount(long providerId) {
        final String advKey = primaryKeyUtil.createPrimaryKey(providerId);
        return readInteger(getSet(), advKey, COUNTER_BIN_NAME);
    }

    private String getSet() {
        return config.getProperty(AdvertisementConfig.AEROSPIKE_ADVERTISEMENT_SET);
    }
}
