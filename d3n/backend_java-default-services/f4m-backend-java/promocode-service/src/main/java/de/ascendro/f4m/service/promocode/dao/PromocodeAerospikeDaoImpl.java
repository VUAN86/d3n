package de.ascendro.f4m.service.promocode.dao;

import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.aerospike.client.Bin;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.analytics.model.PromoCodeEvent;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.promocode.config.PromocodeConfig;
import de.ascendro.f4m.service.promocode.exception.F4MNoLongerAvailableException;
import de.ascendro.f4m.service.promocode.model.Promocode;
import de.ascendro.f4m.service.promocode.model.PromocodeCampaign;
import de.ascendro.f4m.service.promocode.model.UserPromocode;
import de.ascendro.f4m.service.promocode.util.PromocodePrimaryKeyUtil;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class PromocodeAerospikeDaoImpl extends AerospikeOperateDaoImpl<PromocodePrimaryKeyUtil> implements PromocodeAerospikeDao {

    private static final String PROMOCODE_BIN_NAME = "promocode";
    private static final String PROMOCODE_CAMPAIGN_BIN_NAME = "promoCampaign";
    private static final String PROMOCODE_COUNTER_RECORD_NAME = "counter";
    private static final String PROMOCODE_CREATION_COUNTER_BIN_NAME = "creationCnt";
    private static final String PROMOCODE_CONSUMING_COUNTER_BIN_NAME = "consumingCnt";
    private static final String PROMOCODE_NUM_OF_USES_BIN_NAME = "numOfUses";


    @Inject
    public PromocodeAerospikeDaoImpl(Config config, PromocodePrimaryKeyUtil promocodePrimaryKeyUtil,
                                   AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
        super(config, promocodePrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
    }

    private String getSet() {
        return config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_CAMPAIGN_SET);
    }

    private String getPromocodeSet() {
        return config.getProperty(PromocodeConfig.AEROSPIKE_PROMOCODE_SET);
    }


    private List<Operation> getCounterWriteOperations(Record readResult, int consumingCounterOffset, int creationCounterOffset) {
        List<Operation> writeOperations = new ArrayList<>();

        long currentConsumingCounter = readResult.getLong(PROMOCODE_CONSUMING_COUNTER_BIN_NAME);
        long currentCreationCounter = readResult.getLong(PROMOCODE_CREATION_COUNTER_BIN_NAME);

        if (consumingCounterOffset != 0) {
            validateConsumingCounterIncrement(consumingCounterOffset, currentConsumingCounter, currentCreationCounter);
            Bin bin = getLongBin(PROMOCODE_CONSUMING_COUNTER_BIN_NAME, consumingCounterOffset);
            writeOperations.add(Operation.add(bin));
        }

        if (creationCounterOffset != 0) {
            if (creationCounterOffset < 0) {
                throw new F4MNoLongerAvailableException("Creation counter cannot be decreased");
            }
            Bin bin = getLongBin(PROMOCODE_CREATION_COUNTER_BIN_NAME, creationCounterOffset);
            writeOperations.add(Operation.add(bin));
        }

        return writeOperations;
    }

    private void validateConsumingCounterIncrement(int consumingCounterOffset, long currentConsumingCounter, long currentCreationCounter) {
        if (consumingCounterOffset > 0 && currentConsumingCounter >= currentCreationCounter) {
            throw new F4MNoLongerAvailableException("Promocode no longer available to be assigned");
        }
    }


    @Override
    public Map.Entry<UserPromocode, PromoCodeEvent> usePromocodeForUser(String promocodeCode, String userId, String appId) {
        // Retrieve userPromocode instance to be assigned, modify it and save it, atomically using operate call
        final String promocodeKey = primaryKeyUtil.createPromocodeKey(promocodeCode);

        if (!exists(getPromocodeSet(), promocodeKey)) {
            throw new F4MEntryNotFoundException("Specified Promocode does not exist");
        }

        // check the application ID
        // TODO: Why this data?
        String jsonPromocode = readJson(getPromocodeSet(), promocodeKey, PROMOCODE_BIN_NAME);

        Promocode promocode = jsonUtil.fromJson(jsonPromocode, Promocode.class);

        // find the associated campaign :
        String jsonPromoCampaign = readJson(getSet(), primaryKeyUtil.createCampaignPrimaryKey(promocode.getPromocodeCampaignId()), PROMOCODE_CAMPAIGN_BIN_NAME);
        PromocodeCampaign promocodeCampaign = jsonUtil.fromJson(jsonPromoCampaign, PromocodeCampaign.class);

        if(!promocodeCampaign.getAppIds().contains(appId)) {
            throw new F4MEntryNotFoundException("Specified promocode does not exist for this app.");
        }

        ZonedDateTime date = DateTimeUtil.parseISODateTimeString(promocode.getExpirationDate());

        if(date.isBefore(DateTimeUtil.getCurrentDateTime())) {
            // the promocode is expired
            throw new F4MNoLongerAvailableException("Specified promocode is expired.");
        }

        // check against the numberOfUses :
        String numberOfUsesCounterKey = primaryKeyUtil.createNumberOfUsesCounterKey(promocodeCode, userId);

        // check if counterRecord exists, if not create it, otherwise we get a NPE below :
        boolean exists = exists(getPromocodeSet(), numberOfUsesCounterKey);
        if(!exists) {
            createRecord(getPromocodeSet(), numberOfUsesCounterKey, new Bin(PROMOCODE_NUM_OF_USES_BIN_NAME, 1L));
        } else {
            operate(getPromocodeSet(), numberOfUsesCounterKey, new Operation[]{Operation.get()}, (readResult, ops) ->
                    incrementNumberOfUsesCounter(promocode, readResult));
        }


        String counterKey = primaryKeyUtil.createSubRecordKeyByPromocodeId(promocodeCode, PROMOCODE_COUNTER_RECORD_NAME);
        // Modify counters to reflect that a new promocode is assigned
        Record countersRecord;
        countersRecord = operate(getPromocodeSet(), counterKey, new Operation[]{Operation.get()},
                (readResult, ops) -> getCounterWriteOperations(readResult, 1, 0));

        long consumingCounter = countersRecord.getLong(PROMOCODE_CONSUMING_COUNTER_BIN_NAME);

        // create the record for the user as this is not created by nodejs:
        String userPromocodeKey =  primaryKeyUtil.createSubRecordPrimaryKey(promocodeKey, Long.toString(consumingCounter - 1));
        UserPromocode userPromocode = new UserPromocode();
        userPromocode.setId(userPromocodeKey);
        userPromocode.setCode(promocodeCode);
        userPromocode.setExpirationDate(promocode.getExpirationDate());
        userPromocode.setUsedOnDate(DateTimeUtil.getCurrentTimestampInISOFormat());
        userPromocode.setUserId(userId);
        userPromocode.setMoneyValue(promocode.getMoneyValue());
        userPromocode.setBonuspointsValue(promocode.getBonuspointsValue());
        userPromocode.setCreditValue(promocode.getCreditValue());
        createJson(getPromocodeSet(), userPromocodeKey, PROMOCODE_BIN_NAME, jsonUtil.toJson(userPromocode));

        PromoCodeEvent promoCodeEvent = new PromoCodeEvent();
        promoCodeEvent.setPromoCode(promocodeCode);
        promoCodeEvent.setPromoCodeCampaignId(promocodeCampaign.getId());
        promoCodeEvent.setBonusPointsPaid(promocode.getBonuspointsValue().longValue());
        promoCodeEvent.setCreditsPaid(promocode.getCreditValue().longValue());
        promoCodeEvent.setMoneyPaid(promocode.getMoneyValue());
        promoCodeEvent.setPromoCodeUsed(true);

        return new AbstractMap.SimpleEntry<>(userPromocode, promoCodeEvent);

    }

    private List<Operation> incrementNumberOfUsesCounter(Promocode promocode, Record readResult) {
        List<Operation> writeOperations = new ArrayList<>();
        long currentNumberOfUses = readResult.getLong(PROMOCODE_NUM_OF_USES_BIN_NAME);
        if (currentNumberOfUses >= promocode.getNumberOfUses()) {
            throw new F4MNoLongerAvailableException(
                    "Specified promocode was already used the specified number of times by the user.");
        }

        Bin bin = getLongBin(PROMOCODE_NUM_OF_USES_BIN_NAME, 1);
        writeOperations.add(Operation.add(bin));
        return writeOperations;
    }

    @Override
    public String getPromocodeById(String id) {
        return readJson(getPromocodeSet(), id, PROMOCODE_BIN_NAME);
    }

    @Override
    public void updateUserPromocode(UserPromocode userPromocode) {
        updateJson(getPromocodeSet(), userPromocode.getId(), PROMOCODE_BIN_NAME,
                (readResult, writePolicy) -> jsonUtil.toJson(userPromocode));
    }
}
