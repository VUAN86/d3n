package de.ascendro.f4m.service.voucher.dao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.Bin;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.voucher.CommonVoucherAerospikeDaoImpl;
import de.ascendro.f4m.server.voucher.util.VoucherPrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.voucher.config.VoucherConfig;
import de.ascendro.f4m.service.voucher.exception.F4MNoVoucherAvailableException;
import de.ascendro.f4m.service.voucher.exception.F4MVoucherAlreadyUsedException;
import de.ascendro.f4m.service.voucher.model.UserVoucher;
import de.ascendro.f4m.service.voucher.model.UserVoucherType;
import de.ascendro.f4m.service.voucher.model.Voucher;

public class VoucherAerospikeDaoImpl extends CommonVoucherAerospikeDaoImpl implements VoucherAerospikeDao {

    private static final String VOUCHER_BIN_NAME = "voucher";
    private static final String VOUCHER_LIST_BIN_NAME = "vouchers";
    private static final String USER_VOUCHER_BIN_NAME = "usrVoucher";
    private static final String VOUCHER_COUNTER_RECORD_NAME = "counter";
    private static final String VOUCHER_CREATION_COUNTER_BIN_NAME = "creationCnt";
    private static final String VOUCHER_LOCK_COUNTER_BIN_NAME = "lockCnt";
    private static final String VOUCHER_CONSUMING_COUNTER_BIN_NAME = "consumingCnt";
    private static final String SPECIFIED_VOUCHER_DOES_NOT_EXIST = "Specified Voucher does not exist";
    private static final String COUNTERS_NOT_INITIALIZED = "Counters for specified Voucher are not initialized";
    private static final String USER_VOUCHER_DOES_NOT_EXIST = "User voucher does not exist";

    @Inject
    public VoucherAerospikeDaoImpl(Config config, VoucherPrimaryKeyUtil voucherPrimaryKeyUtil,
                                   AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
        super(config, voucherPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
    }



    private String getListSet() {
        return config.getProperty(VoucherConfig.AEROSPIKE_VOUCHER_LIST_SET);
    }

    private String getUserVoucherSet() {
        return config.getProperty(VoucherConfig.AEROSPIKE_USER_VOUCHER_SET);
    }

    @Override
    public UserVoucher getUserVoucher(String voucherId, String userVoucherId) {
        String key = primaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, userVoucherId);
        final String userVoucherJson = readJson(getUserVoucherSet(), key, USER_VOUCHER_BIN_NAME);
        return StringUtils.isNotBlank(userVoucherJson) ? jsonUtil.fromJson(userVoucherJson, UserVoucher.class) : null;
    }

	@Override
	public void updateVoucherUser(String voucherId, String userVoucherId, String targetUserId) {
        String key = primaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, userVoucherId);
		updateJson(getUserVoucherSet(), key, USER_VOUCHER_BIN_NAME, (readValue, writePolicy) -> {
			UserVoucher userVoucher = jsonUtil.fromJson(readValue, UserVoucher.class);
			userVoucher.setUserId(targetUserId);
			return jsonUtil.toJson(userVoucher);
		});
	}

    @Override
    public String assignVoucherToUser(String voucherId, String userId, String gameInstanceId, String tombolaId, String reason) {
        String assignedVoucherId;
        Record countersRecord;
        int lockCounterOffset;
        String voucherKey = primaryKeyUtil.createPrimaryKey(voucherId);
        if (!exists(getSet(), voucherKey)) {
            throw new F4MEntryNotFoundException(SPECIFIED_VOUCHER_DOES_NOT_EXIST);
        }

        String counterKey = primaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, VOUCHER_COUNTER_RECORD_NAME);
        if (!exists(getUserVoucherSet(), counterKey)) {
            throw new F4MEntryNotFoundException(COUNTERS_NOT_INITIALIZED);
        }
        if ("winningComponent".equals(reason)){
            lockCounterOffset = 0;
        } else {
            lockCounterOffset = -1;
        }
        // Modify counters to reflect that a new voucher is assigned
        countersRecord = operate(getUserVoucherSet(), counterKey, new Operation[]{Operation.get()},
                (readResult, ops) -> getCounterWriteOperations(readResult, 1, lockCounterOffset, 0));

        long consumingCounter = countersRecord.getLong(VOUCHER_CONSUMING_COUNTER_BIN_NAME);

        assignedVoucherId = String.valueOf(consumingCounter - 1); // userVoucher ids are 0 based

        // Retrieve userVoucher instance to be assigned, modify it and save it, atomically using operate call
        final String userVoucherKey = primaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, assignedVoucherId);
        final Operation[] readOperations = new Operation[]{Operation.get(USER_VOUCHER_BIN_NAME)};

        operate(getUserVoucherSet(), userVoucherKey, readOperations,
                (readResult, ops) -> getUserVoucherAssignWriteOperations(userId, gameInstanceId, tombolaId, readResult));
        return assignedVoucherId;
    }

    private List<Operation> getUserVoucherAssignWriteOperations(String userId, String gameInstanceId, String tombolaId,
            Record readResult) {
        List<Operation> writeOperations = new ArrayList<>();

        if (readResult == null) {
            throw new F4MEntryNotFoundException(USER_VOUCHER_DOES_NOT_EXIST);
        }

        String existingUserVoucherJsonString = readJson(USER_VOUCHER_BIN_NAME, readResult);

        if (existingUserVoucherJsonString == null) {
            throw new F4MEntryNotFoundException(USER_VOUCHER_DOES_NOT_EXIST);
        }
        // Get current user Voucher
        UserVoucher userVoucher = jsonUtil.fromJson(existingUserVoucherJsonString, UserVoucher.class);
        userVoucher.setObtainDate(DateTimeUtil.getCurrentTimestampInISOFormat());
        userVoucher.setUserId(userId);
        userVoucher.setGameInstanceId(gameInstanceId);
        userVoucher.setTombolaId(tombolaId);
        boolean isPurchased = gameInstanceId == null && tombolaId == null;
        userVoucher.setPurchased(isPurchased);

        // add the creation operation
        writeOperations.add(Operation.put(getJsonBin(USER_VOUCHER_BIN_NAME, jsonUtil.toJson(userVoucher))));

        return writeOperations;
    }

    private List<Operation> getCounterWriteOperations(Record readResult, int consumingCounterOffset,
            int lockCounterOffset, int creationCounterOffset) {
        List<Operation> writeOperations = new ArrayList<>();

        if (readResult == null) {
            throw new F4MNoVoucherAvailableException(USER_VOUCHER_DOES_NOT_EXIST);
        }

        long currentConsumingCounter = readResult.getLong(VOUCHER_CONSUMING_COUNTER_BIN_NAME);
        long currentLockCounter = readResult.getLong(VOUCHER_LOCK_COUNTER_BIN_NAME);
        long currentCreationCounter = readResult.getLong(VOUCHER_CREATION_COUNTER_BIN_NAME);

        if (consumingCounterOffset != 0) {
            validateConsumingCounterIncrement(consumingCounterOffset, currentConsumingCounter, currentCreationCounter);
            Bin bin = getLongBin(VOUCHER_CONSUMING_COUNTER_BIN_NAME, consumingCounterOffset);
            writeOperations.add(Operation.add(bin));
        }
        if (lockCounterOffset != 0) {
            validateLockCounterChange(lockCounterOffset, currentConsumingCounter, currentLockCounter, currentCreationCounter);
            Bin bin = getLongBin(VOUCHER_LOCK_COUNTER_BIN_NAME, lockCounterOffset);
            writeOperations.add(Operation.add(bin));
        }
        if (creationCounterOffset != 0) {
            if (creationCounterOffset < 0) {
                throw new F4MNoVoucherAvailableException("Creation counter cannot be decreased");
            }
            Bin bin = getLongBin(VOUCHER_CREATION_COUNTER_BIN_NAME, creationCounterOffset);
            writeOperations.add(Operation.add(bin));
        }

        return writeOperations;
    }

    private void validateLockCounterChange(int lockCounterOffset, long currentConsumingCounter, long currentLockCounter, long currentCreationCounter) {
        if (lockCounterOffset > 0 && (currentConsumingCounter + currentLockCounter) >= currentCreationCounter) {
            throw new F4MNoVoucherAvailableException("Voucher no longer available to be locked");
        }
        if (lockCounterOffset < 0 && currentLockCounter + lockCounterOffset < 0) {
            throw new F4MNoVoucherAvailableException("Voucher lock no longer available to be released");
        }
    }

    private void validateConsumingCounterIncrement(int consumingCounterOffset, long currentConsumingCounter, long currentCreationCounter) {
        if (consumingCounterOffset > 0 && currentConsumingCounter >= currentCreationCounter) {
            throw new F4MNoVoucherAvailableException("Voucher no longer available to be assigned");
        }
    }


    @Override
    public long getTotalUserVouchersCountByType(String voucherId, UserVoucherType type) {
        long result;

        String counterKey = primaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, VOUCHER_COUNTER_RECORD_NAME);
        if (!exists(getUserVoucherSet(), counterKey)) {
            throw new F4MEntryNotFoundException(COUNTERS_NOT_INITIALIZED);
        }

        Record countersRecord = readRecord(getUserVoucherSet(), counterKey);

        Long consumingCounter = countersRecord.getLong(VOUCHER_CONSUMING_COUNTER_BIN_NAME);
        Long lockedCounter = countersRecord.getLong(VOUCHER_LOCK_COUNTER_BIN_NAME);
        Long creationCounter = countersRecord.getLong(VOUCHER_CREATION_COUNTER_BIN_NAME);

        if (type == UserVoucherType.CONSUMED) {
            result = consumingCounter;
        } else if (type == UserVoucherType.LOCKED) {
            result = lockedCounter;
        } else { // AVAILABLE
            result = creationCounter - consumingCounter - lockedCounter;
        }

        return result;
    }

    @Override
    public List<UserVoucher> getUserVoucherListByVoucherId(int limit, long offset, String voucherId, UserVoucherType type) {
        List<UserVoucher> userVoucherList = new ArrayList<>();
        String counterKey = primaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, VOUCHER_COUNTER_RECORD_NAME);
        if (!exists(getUserVoucherSet(), counterKey)) {
            throw new F4MEntryNotFoundException(COUNTERS_NOT_INITIALIZED);
        }

        Record countersRecord = readRecord(getUserVoucherSet(), counterKey);

        Long consumingCounter = countersRecord.getLong(VOUCHER_CONSUMING_COUNTER_BIN_NAME);
        Long lockedCounter = countersRecord.getLong(VOUCHER_LOCK_COUNTER_BIN_NAME);
        Long creationCounter = countersRecord.getLong(VOUCHER_CREATION_COUNTER_BIN_NAME);

        long actualOffset;
        int actualLimit = limit;
        long totalCount;

        if (Objects.isNull(type)) {
            actualOffset = offset;
        	totalCount = creationCounter;
            actualLimit = (int) (totalCount);
        } else if (type == UserVoucherType.CONSUMED) {
            actualOffset = offset;
            totalCount = consumingCounter;
        } else if (type == UserVoucherType.LOCKED) {
            actualOffset = consumingCounter + offset;
            totalCount = lockedCounter;
        } else { // AVAILABLE
            actualOffset = consumingCounter + lockedCounter + offset;
            totalCount = creationCounter - consumingCounter - lockedCounter;
        }
        if (actualLimit > totalCount) {
            actualLimit = (int) (totalCount);
        }

        for (Long i = actualOffset; i < actualOffset + actualLimit; i++) {
            String key = primaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, String.valueOf(i));
            String userVoucherJson = readJson(getUserVoucherSet(), key, USER_VOUCHER_BIN_NAME);
            UserVoucher userVoucher = jsonUtil.fromJson(userVoucherJson, UserVoucher.class);
            userVoucher.setId(i.toString());
            userVoucherList.add(userVoucher);
        }

        return userVoucherList;
    }

    @Override
    public List<Voucher> getVouchersByTenantId(int limit, long offset, String tenantId) {
        List<Voucher> results = getAllMap(getListSet(),
                primaryKeyUtil.createVoucherListPrimaryKey(tenantId), VOUCHER_LIST_BIN_NAME)
                .entrySet().stream()
                .map(e -> jsonUtil.fromJson((String) e.getValue(), Voucher.class))
                .collect(Collectors.toCollection(ArrayList::new));
        sortVoucherListById(results);
        return results;
    }

    private void sortVoucherListById(List<Voucher> voucherList) {
        voucherList.sort(Comparator.comparing(Voucher::getId));
    }

    @Override
    public UserVoucher getUserVoucherById(String userVoucherId) {
        final String userVoucherJson = readJson(getUserVoucherSet(), primaryKeyUtil.createUserVoucherKey(userVoucherId), USER_VOUCHER_BIN_NAME);
        return StringUtils.isNotBlank(userVoucherJson) ? jsonUtil.fromJson(userVoucherJson, UserVoucher.class) : null;
    }

    @Override
    public Voucher getVoucherByUserVoucherId(String userVoucherId) {
        final String voucherJson = readJson(getSet(), primaryKeyUtil.createVoucherKeyFromUserVoucherKey(userVoucherId), VOUCHER_BIN_NAME);
        return StringUtils.isNotBlank(voucherJson) ? jsonUtil.fromJson(voucherJson, Voucher.class) : null;
    }

    @Override
    public void useVoucherForUser(String userVoucherId, String userId) {
        // Retrieve userVoucher instance to be assigned, modify it and save it, atomically using operate call
        final String userVoucherKey = primaryKeyUtil.createUserVoucherKey(userVoucherId);

        if (!exists(getUserVoucherSet(), userVoucherKey)) {
            throw new F4MEntryNotFoundException(USER_VOUCHER_DOES_NOT_EXIST);
        }

        operate(getUserVoucherSet(), userVoucherKey, new Operation[]{Operation.get(USER_VOUCHER_BIN_NAME)},
                (readResult, ops) -> getUserVoucherUseWriteOperations(readResult, userId));
    }

    private List<Operation> getUserVoucherUseWriteOperations(Record readResult, String userId) {
        List<Operation> writeOperations = new ArrayList<>();

        String existingUserVoucherJsonString = readJson(USER_VOUCHER_BIN_NAME, readResult);
        if (StringUtils.isBlank(existingUserVoucherJsonString)) {
            throw new F4MEntryNotFoundException(USER_VOUCHER_DOES_NOT_EXIST);
        }

        // Get current user Voucher
        UserVoucher userVoucher = jsonUtil.fromJson(existingUserVoucherJsonString, UserVoucher.class);

        if (!userId.equals(userVoucher.getUserId())) {
            throw new F4MInsufficientRightsException("This voucher does not belong to you");
        }
        if (userVoucher.isUsed()) {
            throw new F4MVoucherAlreadyUsedException("This Voucher code was already used");
        }

        userVoucher.setUsed(true);
        userVoucher.setUsedDate(DateTimeUtil.getCurrentTimestampInISOFormat());

        // add the creation operation
        writeOperations.add(Operation.put(getJsonBin(USER_VOUCHER_BIN_NAME, jsonUtil.toJson(userVoucher))));

        return writeOperations;
    }

    @Override
    public void reserveVoucher(String voucherId) {
        String counterKey = primaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, VOUCHER_COUNTER_RECORD_NAME);

        String voucherKey = primaryKeyUtil.createPrimaryKey(voucherId);
        if (!exists(getSet(), voucherKey)) {
            throw new F4MNoVoucherAvailableException(SPECIFIED_VOUCHER_DOES_NOT_EXIST);
        }

        if (!exists(getUserVoucherSet(), counterKey)) {
            throw new F4MNoVoucherAvailableException(COUNTERS_NOT_INITIALIZED);
        }
        // Modify counters to reflect that a new voucher is assigned
        operate(getUserVoucherSet(), counterKey, new Operation[]{Operation.get()},
                (readResult, ops) -> getCounterWriteOperations(readResult, 0, 1, 0));
    }

    @Override
    public void releaseVoucher(String voucherId) {
        String counterKey = primaryKeyUtil.createSubRecordKeyByVoucherId(voucherId, VOUCHER_COUNTER_RECORD_NAME);

        String voucherKey = primaryKeyUtil.createPrimaryKey(voucherId);
        if (!exists(getSet(), voucherKey)) {
            throw new F4MEntryNotFoundException(SPECIFIED_VOUCHER_DOES_NOT_EXIST);
        }

        if (!exists(getUserVoucherSet(), counterKey)) {
            throw new F4MEntryNotFoundException(COUNTERS_NOT_INITIALIZED);
        }

        operate(getUserVoucherSet(), counterKey, new Operation[]{Operation.get()},
                (readResult, ops) -> getCounterWriteOperations(readResult, 0, -1, 0));

    }

    @Override
    public void removeExpiredVouchersFromVoucherList(String tenantId) {
        String key = primaryKeyUtil.createVoucherListPrimaryKey(tenantId);

        if(exists(getListSet(), key)) {
            updateMap(getListSet(), key, VOUCHER_LIST_BIN_NAME, (readResult, wp) -> {
                readResult.entrySet().removeIf(v -> {
                    Voucher voucher = jsonUtil.fromJson((String) v.getValue(), Voucher.class);
                    return DateTimeUtil.parseISODateTimeString(voucher.getExpirationDate())
                            .isBefore(DateTimeUtil.getCurrentDateTime());
                });

                return readResult;
            });
        }
	}
}
