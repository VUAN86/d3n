package de.ascendro.f4m.service.tombola.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import de.ascendro.f4m.service.tombola.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.util.TimeBasedPaginationUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.tombola.config.TombolaConfig;
import de.ascendro.f4m.service.tombola.exception.TombolaNoLongerAvailableException;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingListResponse;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaListResponse;
import de.ascendro.f4m.service.tombola.util.TombolaPrimaryKeyUtil;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class TombolaAerospikeDaoImpl extends AerospikeOperateDaoImpl<TombolaPrimaryKeyUtil>
        implements TombolaAerospikeDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(TombolaAerospikeDaoImpl.class);

    private static final String AVAILABLE_TOMBOLA_LIST_BIN_NAME = "tombolas";
    private static final String TOMBOLA_BIN_NAME = "tombola";
    private static final String TOMBOLA_DRAWING_LIST_BIN_NAME = "drawings";
    private static final String DRAWING_BIN_NAME = "drawing";
    private static final String USER_TOMBOLA_BIN_NAME = "usrTombola";
    private static final String HISTORY_BIN_NAME = "history";
    private static final String STATISTICS_BIN_NAME = "statistics";
    private static final String TOMBOLA_TICKET_BIN_NAME = "ticket";

    private static final String TOMBOLA_COUNTER_RECORD_NAME = "counters";
    private static final String TOMBOLA_LOCKED_COUNTER_BIN_NAME = "locked";
    private static final String TOMBOLA_PURCHASED_COUNTER_BIN_NAME = "purchased";

    @Inject
    public TombolaAerospikeDaoImpl(Config config, TombolaPrimaryKeyUtil tombolaPrimaryKeyUtil,
            AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
        super(config, tombolaPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
    }

    private String getTombolaSet() {
        return config.getProperty(TombolaConfig.AEROSPIKE_TOMBOLA_SET);
    }

    private String getAvailableTombolaListSet() {
        return config.getProperty(TombolaConfig.AEROSPIKE_AVAILABLE_TOMBOLA_LIST_SET);
    }

    private String getTombolaDrawingListSet() {
        return config.getProperty(TombolaConfig.AEROSPIKE_TOMBOLA_DRAWING_LIST_SET);
    }

    private String getTombolaTicketSet() {
        return config.getProperty(TombolaConfig.AEROSPIKE_TOMBOLA_TICKET_SET);
    }

    private String getUserTombolaSet() {
        return config.getProperty(TombolaConfig.AEROSPIKE_USER_TOMBOLA_SET);
    }

    @Override
    public List<Tombola> getAvailableTombolaList(String appId) {
        String key = primaryKeyUtil.createAvailableTombolaListPrimaryKey(appId);

        Map<Object, Object> tombolaMap = getAllMap(getAvailableTombolaListSet(), key, AVAILABLE_TOMBOLA_LIST_BIN_NAME);
        List<Tombola> results = tombolaMap.entrySet().stream()
                .map(e -> jsonUtil.fromJson((String) e.getValue(), Tombola.class))
                .collect(Collectors.toCollection(ArrayList::new));

        sortTombolaListById(results);

        return results;
    }

    @Override
    public void removeTombolaFromAvailableTombolaList(String tombolaId, List<String> appIdList) {
        appIdList.forEach(appId -> {
            String key = primaryKeyUtil.createAvailableTombolaListPrimaryKey(appId);
            deleteByKeyFromMapSilently(getAvailableTombolaListSet(), key, AVAILABLE_TOMBOLA_LIST_BIN_NAME, tombolaId);
        });
    }

    @Override
    public void addTombolaToAvailableTombolaList(Tombola tombola, List<String> appIdList) {
        appIdList.forEach(appId -> {
            String key = primaryKeyUtil.createAvailableTombolaListPrimaryKey(appId);
            createOrUpdateMapValueByKey(getAvailableTombolaListSet(), key, AVAILABLE_TOMBOLA_LIST_BIN_NAME,
                    tombola.getId(), (existing, writePolicy) -> jsonUtil.toJson(tombola));
        });
    }

    @Override
    public Tombola getTombola(String tombolaId) {
        String tombolaKey = primaryKeyUtil.createPrimaryKey(tombolaId);
        if (!exists(getTombolaSet(), tombolaKey)) {
            throw new F4MEntryNotFoundException("Specified Tombola does not exist");
        }

        return jsonUtil.fromJson(readJson(getTombolaSet(), tombolaKey, TOMBOLA_BIN_NAME), Tombola.class);
    }

    @Override
    public void setTombolaStatus(String tombolaId, TombolaStatus status) {
        String key = primaryKeyUtil.createPrimaryKey(tombolaId);
        updateJson(getTombolaSet(), key, TOMBOLA_BIN_NAME, (readValue, writePolicy) -> {
            Tombola tombola = jsonUtil.fromJson(readValue, Tombola.class);
            tombola.setStatus(status);
            return jsonUtil.toJson(tombola);
        });
    }

    @Override
    public void setTombolaStatusInAvailableTombolaList(String tombolaId, TombolaStatus status) {
        Tombola tombola = getTombola(tombolaId);

        tombola.getApplicationsIds().forEach(appId -> {
            List<Tombola> tombolas = getAvailableTombolaList(appId);
            String key = primaryKeyUtil.createAvailableTombolaListPrimaryKey(appId);
            if (tombolas.stream().anyMatch(t -> t.getId().equals(tombolaId))) {
                createOrUpdateMapValueByKey(getAvailableTombolaListSet(), key, AVAILABLE_TOMBOLA_LIST_BIN_NAME,
                        tombolaId, (existing, writePolicy) -> jsonUtil.toJson(tombola));
            }
        });
    }

    private List<TombolaDrawing> getDrawingsForDate(String appId, LocalDate drawDate) {
        String set = getTombolaDrawingListSet();
        String key = primaryKeyUtil.createTombolaDrawingPrimaryKey(appId, drawDate.toString());
        return getAllMap(set, key, TOMBOLA_DRAWING_LIST_BIN_NAME).entrySet().stream()
                .map(e -> jsonUtil.fromJson((String) e.getValue(), TombolaDrawing.class))
                .collect(Collectors.toList());
    }

    private int getNumberOfDrawingsForDate(String appId, LocalDate drawDate) {
        Integer size = null;
        String key = primaryKeyUtil.createTombolaDrawingPrimaryKey(appId, drawDate.toString());
        if (exists(getTombolaDrawingListSet(), key)) {
            size = getMapSize(getTombolaDrawingListSet(), key, TOMBOLA_DRAWING_LIST_BIN_NAME);
        }
        return size != null ? size : 0;
    }

    @Override
    public TombolaDrawingListResponse getTombolaDrawings(String appId, long offset, int limit,
            ZonedDateTime dateTimeFrom, ZonedDateTime dateTimeTo) {
        ListResult<TombolaDrawing> tombolaDrawingListResult = TimeBasedPaginationUtil.getPaginatedItems(offset, limit,
                dateTimeFrom, dateTimeTo,
                zonedDateTime -> getDrawingsForDate(appId, zonedDateTime.toLocalDate()),
                tombolaDrawing -> DateTimeUtil.parseISODateTimeString(tombolaDrawing.getDrawDate()),
                zonedDateTime -> getNumberOfDrawingsForDate(appId, zonedDateTime.toLocalDate()),
                (zonedDateTime1, zonedDateTime2) -> zonedDateTime1.toLocalDate().equals(zonedDateTime2.toLocalDate()),
                zonedDateTime -> !zonedDateTime.toLocalDate().isBefore(dateTimeFrom.toLocalDate()),
                zonedDateTime -> zonedDateTime.minusDays(1));

        return new TombolaDrawingListResponse(limit, offset, tombolaDrawingListResult.getTotal(),
                tombolaDrawingListResult.getItems());
    }

    @Override
    public TombolaDrawing getTombolaDrawing(String tombolaId) {
        TombolaDrawing result = null;
        String tombolaKey = primaryKeyUtil.createPrimaryKey(tombolaId);
        if (exists(getTombolaSet(), tombolaKey)) {
            result = jsonUtil.fromJson(readJson(getTombolaSet(), tombolaKey, DRAWING_BIN_NAME), TombolaDrawing.class);
        }
        return result;
    }

    @Override
    public UserTombolaListResponse getUserTombolaList(String userId, long offset, int limit, ZonedDateTime dateFrom,
            ZonedDateTime dateTo, boolean isPendingDrawing) {
        // collect data available
        UserTombolaListResponse response;
        if (isPendingDrawing) {
            response = getPendingDrawingUserTombolaList(userId, offset, limit, dateFrom, dateTo);
        } else {
            response = getFinishedDrawingUserTombolaList(userId, offset, limit, dateFrom, dateTo);
        }

        return response;
    }

    private int getNumberOfUserTombolaInfoForMonth(String userId, String month) {
        Integer size = null;
        String key = primaryKeyUtil.createUserTombolaHistoryByMonthPrimaryKey(userId, month);
        if (exists(getUserTombolaSet(), key)) {
            size = getMapSize(getUserTombolaSet(), key, HISTORY_BIN_NAME);
        }
        return size != null ? size : 0;
    }

    private UserTombolaListResponse getFinishedDrawingUserTombolaList(String userId, long offset, int limit,
            ZonedDateTime dateTimeFrom, ZonedDateTime dateTimeTo) {
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        ListResult<UserTombolaInfo> userTombolaInfoListResult = TimeBasedPaginationUtil.getPaginatedItems(offset, limit,
                dateTimeFrom, dateTimeTo,
                zonedDateTime -> getUserTombolaHistoryForMonth(userId, zonedDateTime.format(monthFormatter)),
                userTombolaInfo -> DateTimeUtil.parseISODateTimeString(userTombolaInfo.getDrawDate()),
                zonedDateTime -> getNumberOfUserTombolaInfoForMonth(userId, zonedDateTime.format(monthFormatter)),
                (zonedDateTime1, zonedDateTime2) ->
                        zonedDateTime1.format(monthFormatter).equals(zonedDateTime2.format(monthFormatter)),
                zonedDateTime -> !zonedDateTime.toLocalDate().withDayOfMonth(1)
                        .isBefore(dateTimeFrom.toLocalDate().withDayOfMonth(1)),
                zonedDateTime -> zonedDateTime.minusMonths(1));

        return new UserTombolaListResponse(limit, offset, userTombolaInfoListResult.getTotal(),
                userTombolaInfoListResult.getItems());
    }

    private UserTombolaListResponse getPendingDrawingUserTombolaList(String userId, long offset, int limit,
            ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        // collect data available
        List<UserTombolaInfo> userTombolaInfoList = getUserTombolaHistoryForPending(userId);
        TimeBasedPaginationUtil.sortListByDateDescending(userTombolaInfoList,
                (UserTombolaInfo t) -> DateTimeUtil.parseISODateTimeString(t.getDrawDate()));

        // remove items outside of the time interval
        int numberOfItemsBeforeDateFrom = (int) userTombolaInfoList.stream()
                .map(userTombolaInfo -> DateTimeUtil.parseISODateTimeString(userTombolaInfo.getDrawDate()))
                .filter(drawDateTime -> drawDateTime.isBefore(dateFrom)).count();
        int numberOfItemsAfterDateTo = (int) userTombolaInfoList.stream()
                .map(userTombolaInfo -> DateTimeUtil.parseISODateTimeString(userTombolaInfo.getDrawDate()))
                .filter(drawDateTime -> drawDateTime.isAfter(dateTo)).count();
        userTombolaInfoList = TimeBasedPaginationUtil.removeExtraItemsFromList(userTombolaInfoList,
                numberOfItemsAfterDateTo, numberOfItemsBeforeDateFrom);

        int total = userTombolaInfoList.size();
        // remove items based on offset and limit
        int tail = total - offset - limit > 0 ? (int) (total - offset - limit) : 0;
        userTombolaInfoList = TimeBasedPaginationUtil.removeExtraItemsFromList(userTombolaInfoList, (int) offset, tail);

        return new UserTombolaListResponse(limit, offset, total, userTombolaInfoList);
    }

    @Override
    public UserTombolaInfo getUserTombola(String userId, String tombolaId) {
        UserTombolaInfo result = null;
        String userTombolaKey = primaryKeyUtil.createUserTombolaPrimaryKey(userId, tombolaId);
        if (exists(getUserTombolaSet(), userTombolaKey)) {
            result = jsonUtil.fromJson(readJson(getUserTombolaSet(), userTombolaKey, USER_TOMBOLA_BIN_NAME),
                    UserTombolaInfo.class);
        }
        return result;
    }

    @Override
    public void updateUserTombolaAfterDrawing(String userId, String tenantId, String tombolaId, String drawDate,
            List<TombolaWinner> wonPrizes) {
        String set = getUserTombolaSet();
        String detailKey = primaryKeyUtil.createUserTombolaPrimaryKey(userId, tombolaId);

        String updatedJson = updateJson(set, detailKey, USER_TOMBOLA_BIN_NAME, (readValue, writePolicy) -> {
            UserTombolaInfo userTombolaInfo = jsonUtil.fromJson(readValue, UserTombolaInfo.class);
            userTombolaInfo.setPending(false);
            userTombolaInfo.setDrawDate(drawDate);
            userTombolaInfo.addPrizes(wonPrizes);
            userTombolaInfo.setTotalPrizesWon(userTombolaInfo.getPrizes().size());
            return jsonUtil.toJson(userTombolaInfo);
        });

        UserTombolaInfo updatedUserTombolaInfo = jsonUtil.fromJson(updatedJson, UserTombolaInfo.class);

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        String month = DateTimeUtil.parseISODateTimeString(drawDate).format(monthFormatter);
        String monthListKey = primaryKeyUtil.createUserTombolaHistoryByMonthPrimaryKey(userId, month);
        createOrUpdateMapValueByKey(set, monthListKey, HISTORY_BIN_NAME, tombolaId,
                (existing, writePolicy) -> jsonUtil.toJson(updatedUserTombolaInfo));

        String pendingListKey = primaryKeyUtil.createUserTombolaHistoryPendingDrawingPrimaryKey(userId);
        deleteByKeyFromMapSilently(set, pendingListKey, HISTORY_BIN_NAME, tombolaId);

        // add month to the history list, needed for merge profiles
        String drawnMonthsKey = primaryKeyUtil.createUserTombolaHistoryDrawnMonthsListPrimaryKey(userId);
        createOrUpdateList(getUserTombolaSet(), drawnMonthsKey, HISTORY_BIN_NAME,
                (existing, writePolicy) -> addUniqueItemToList(month, existing));
    }

    @Override
    public List<String> getDrawnMonthsForUser(String userId) {
        String drawnMonthsKey = primaryKeyUtil.createUserTombolaHistoryDrawnMonthsListPrimaryKey(userId);
        return readList(getUserTombolaSet(), drawnMonthsKey, HISTORY_BIN_NAME);
    }

    @Override
    public void setDrawnMonthsForUser(String userId, List<String> drawnMonths) {
        String key = primaryKeyUtil.createUserTombolaHistoryDrawnMonthsListPrimaryKey(userId);
        this.<String> createOrUpdateList(getUserTombolaSet(), key, HISTORY_BIN_NAME,
                (readResult, writePolicy) -> drawnMonths);
    }

    @Override
    public void deleteDrawnMonthsForUser(String userId) {
        String key = primaryKeyUtil.createUserTombolaHistoryDrawnMonthsListPrimaryKey(userId);
        deleteSilently(getUserTombolaSet(), key);
    }

    @Override
    public void updateStatisticsAfterDrawing(String userId, String drawDate, int numberOfWinningTickets,
            String tenantId, List<String> appIdList) {
        if (numberOfWinningTickets > 0) {
            appIdList.forEach(appId -> {
                String key = primaryKeyUtil.createUserTombolaStatisticsByAppIdKey(userId, appId);
                if (exists(getUserTombolaSet(), key)) {
                    createOrUpdateJson(getUserTombolaSet(), key, STATISTICS_BIN_NAME,
                            (existing, wp) -> getUpdatedStatistics(numberOfWinningTickets, drawDate, existing));
                }
            } );

            String key = primaryKeyUtil.createUserTombolaStatisticsByTenantIdKey(userId, tenantId);
            createOrUpdateJson(getUserTombolaSet(), key, STATISTICS_BIN_NAME,
                    (existing, wp) -> getUpdatedStatistics(numberOfWinningTickets, drawDate, existing));

        }
    }

    private String getUpdatedStatistics(int numberOfWinningTickets, String drawDate, String existing) {
        UserStatistics existingUserStatistics = existing == null ? null : jsonUtil.fromJson(existing,
                UserStatistics.class);
        if(existingUserStatistics == null) {
            existingUserStatistics = new UserStatistics();
        }

        existingUserStatistics.setTicketsBoughtSinceLastWin(0);
        existingUserStatistics.setWinningTicketsNumber(
                existingUserStatistics.getWinningTicketsNumber() + numberOfWinningTickets);
        existingUserStatistics.setLastWinDate(drawDate);

        return jsonUtil.toJson(existingUserStatistics);
    }

    @Override
    public TombolaTicket getTombolaTicket(String tombolaId, String ticketId) {
        String ticketKey = primaryKeyUtil.createTombolaTicketKey(tombolaId, ticketId);
        TombolaTicket ticket = null;
        if (exists(getTombolaTicketSet(), ticketKey)) {
            String jsonString = readJson(getTombolaTicketSet(), ticketKey, TOMBOLA_TICKET_BIN_NAME);
            ticket = jsonUtil.fromJson(jsonString, TombolaTicket.class);
        }
        return ticket;
    }

    @Override
    public void createTombolaDrawing(TombolaDrawing tombolaDrawing, List<String> applicationIds) {
        String tombolaKey = primaryKeyUtil.createPrimaryKey(tombolaDrawing.getTombolaId());
        createOrUpdateJson(getTombolaSet(), tombolaKey, DRAWING_BIN_NAME,
                (existing, wp) -> jsonUtil.toJson(tombolaDrawing));
        LocalDate drawDate = DateTimeUtil.parseISODateTimeString(tombolaDrawing.getDrawDate()).toLocalDate();
        updateDrawingLists(tombolaDrawing, drawDate, applicationIds);
    }

    @Override
    public int getPurchasedTicketsNumber(String tombolaId) {
        String counterKey = primaryKeyUtil.createSubRecordKeyByTombolaId(tombolaId, TOMBOLA_COUNTER_RECORD_NAME);
        return Optional.ofNullable(readInteger(getTombolaTicketSet(), counterKey, TOMBOLA_PURCHASED_COUNTER_BIN_NAME))
                .orElse(0);
    }

    @Override
    public int getBoostTicketsNumber(String tombolaId) {
        String boostKey = primaryKeyUtil.createBoostKeyByTombolaId(tombolaId);
        if (!exists(getTombolaSet(), boostKey)) {
           return 0;
        }
        return jsonUtil.fromJson(readJson(getTombolaSet(), boostKey, TOMBOLA_BIN_NAME), TombolaBoost.class).getBoostSize();
    }

    @Override
    public UserStatistics getUserStatisticsByTenantId(String userId, String tenantId) {
        UserStatistics result = null;
        String userStatisticsKey = primaryKeyUtil.createUserTombolaStatisticsByTenantIdKey(userId, tenantId);
        if (exists(getUserTombolaSet(), userStatisticsKey)) {
            result = jsonUtil.fromJson(readJson(getUserTombolaSet(), userStatisticsKey, STATISTICS_BIN_NAME),
                    UserStatistics.class);
        }
        return result;
    }

    @Override
    public UserStatistics getUserStatisticsByAppId(String userId, String appId) {
        UserStatistics result = null;
        String userStatisticsKey = primaryKeyUtil.createUserTombolaStatisticsByAppIdKey(userId, appId);
        if (exists(getUserTombolaSet(), userStatisticsKey)) {
            result = jsonUtil.fromJson(readJson(getUserTombolaSet(), userStatisticsKey, STATISTICS_BIN_NAME),
                    UserStatistics.class);
        }
        return result;
    }


    @Override
    public void updateUserTombolaHistoryForMonth(String userId, String tombolaId, UserTombolaInfo userTombolaInfo,
            String month) {
        String key = primaryKeyUtil.createUserTombolaHistoryByMonthPrimaryKey(userId, month);
        createOrUpdateMapValueByKey(getUserTombolaSet(), key, HISTORY_BIN_NAME, tombolaId,
                (existing, writePolicy) -> jsonUtil.toJson(userTombolaInfo));
    }

    @Override
    public void updateUserTombolaHistoryForPending(String userId, String tombolaId, UserTombolaInfo userTombolaInfo) {
        // update map for targetUserId
        String key = primaryKeyUtil.createUserTombolaHistoryPendingDrawingPrimaryKey(userId);
        createOrUpdateMapValueByKey(getUserTombolaSet(), key, HISTORY_BIN_NAME, tombolaId,
                (existing, writePolicy) -> jsonUtil.toJson(userTombolaInfo));
    }

    @Override
    public void updateUserTombola(String userId, String tombolaId, UserTombolaInfo userTombolaInfo) {
        String userTombolaPrimaryKey = primaryKeyUtil.createUserTombolaPrimaryKey(userId, tombolaId);
        createOrUpdateJson(getUserTombolaSet(), userTombolaPrimaryKey, USER_TOMBOLA_BIN_NAME,
                (existing, wp) -> jsonUtil.toJson(userTombolaInfo));
    }

    @Override
    public void deleteUserTombola(String userId, String tombolaId) {
        String userTombolaPrimaryKey = primaryKeyUtil.createUserTombolaPrimaryKey(userId, tombolaId);
        delete(getUserTombolaSet(), userTombolaPrimaryKey);

    }

    @Override
    public void deleteUserTombolaHistoryItemFromMonth(String userId, String tombolaId, String month) {
        String key = primaryKeyUtil.createUserTombolaHistoryByMonthPrimaryKey(userId, month);
        deleteByKeyFromMapSilently(getUserTombolaSet(), key, HISTORY_BIN_NAME, tombolaId);
    }

    @Override
    public void deleteUserTombolaHistoryItemFromPending(String userId, String tombolaId) {
        String key = primaryKeyUtil.createUserTombolaHistoryPendingDrawingPrimaryKey(userId);
        deleteByKeyFromMapSilently(getUserTombolaSet(), key, HISTORY_BIN_NAME, tombolaId);
    }

    @Override
    public List<UserTombolaInfo> getUserTombolaHistoryForMonth(String userId, String drawMonth) {
        String set = getUserTombolaSet();
        String key = primaryKeyUtil.createUserTombolaHistoryByMonthPrimaryKey(userId, drawMonth);
        List<UserTombolaInfo> userTombolaInfoList = getAllMap(set, key, HISTORY_BIN_NAME).entrySet().stream()
                .map(e -> jsonUtil.fromJson((String) e.getValue(), UserTombolaInfo.class))
                .collect(Collectors.toList());
        return fillUserTombolaListImageId(userTombolaInfoList);
    }

    private List<UserTombolaInfo> fillUserTombolaListImageId(List<UserTombolaInfo> userTombolaInfoList) {
        for (UserTombolaInfo curInfo : userTombolaInfoList){
            if (curInfo.getImageId()==null || "".equals(curInfo.getImageId())){
                try {
                    Tombola fullTombola = getTombola(curInfo.getTombolaId());
                    if (fullTombola != null && fullTombola.getImageId() != "" && fullTombola.getImageId() != null) {
                        curInfo.setImageId(fullTombola.getImageId());
                    }
                } catch (Exception e){
                    continue;
                }
            }
        }
        return userTombolaInfoList;
    }

    @Override
    public Map<String, String> getUserTombolaHistoryMapForMonth(String userId, String drawMonth) {
        String set = getUserTombolaSet();
        String key = primaryKeyUtil.createUserTombolaHistoryByMonthPrimaryKey(userId, drawMonth);

        return getAllMap(set, key, HISTORY_BIN_NAME);
    }

    @Override
    public List<UserTombolaInfo> getUserTombolaHistoryForPending(String userId) {
        String set = getUserTombolaSet();
        String key = primaryKeyUtil.createUserTombolaHistoryPendingDrawingPrimaryKey(userId);

        return getAllMap(set, key, HISTORY_BIN_NAME).entrySet().stream()
                .map(e -> jsonUtil.fromJson((String) e.getValue(), UserTombolaInfo.class))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Map<String, String> getUserTombolaHistoryMapForPending(String userId) {
        String set = getUserTombolaSet();
        String key = primaryKeyUtil.createUserTombolaHistoryPendingDrawingPrimaryKey(userId);

        return getAllMap(set, key, HISTORY_BIN_NAME);
    }

    @Override
    public void updateUserTicketsForBundle(String sourceUserId, String targetUserId, String tombolaId,
            PurchasedBundle bundle) {
        IntStream.range(bundle.getFirstTicketId(), bundle.getFirstTicketId() + bundle.getAmount()).forEach(ticketId -> {
            String ticketIdString = Integer.toString(ticketId);
            TombolaTicket ticket = getTombolaTicket(tombolaId, ticketIdString);
            if (ticket != null && ticket.getUserId().equals(sourceUserId)) {
                ticket.setUserId(targetUserId);
                String ticketKey = primaryKeyUtil.createTombolaTicketKey(tombolaId, ticketIdString);
                createOrUpdateJson(getTombolaTicketSet(), ticketKey, TOMBOLA_TICKET_BIN_NAME,
                        (existing, wp) -> jsonUtil.toJson(ticket));
            }
        });
    }

    public UserStatistics getUserTombolaStatisticsByAppIdKey(String userId, String appId) {
        String json = readJson(getUserTombolaSet(), primaryKeyUtil.createUserTombolaStatisticsByAppIdKey(userId, appId),
                STATISTICS_BIN_NAME);
        return jsonUtil.fromJson(json, UserStatistics.class);
    }

    public UserStatistics getUserTombolaStatisticsByTenantIdKey(String userId, String tenantId) {
        String json = readJson(getUserTombolaSet(), primaryKeyUtil.createUserTombolaStatisticsByTenantIdKey(userId,
                tenantId),
                STATISTICS_BIN_NAME);
        return jsonUtil.fromJson(json, UserStatistics.class);
    }

    private void sortTombolaListById(List<Tombola> tombolas) {
        tombolas.sort(Comparator.comparing(Tombola::getId));
    }

    @Override
    public void lockTickets(String userId, String tombolaId, int totalAvailableTickets, int ticketsToBuy) {
        String counterKey = primaryKeyUtil.createSubRecordKeyByTombolaId(tombolaId, TOMBOLA_COUNTER_RECORD_NAME);
        initializeCounters(tombolaId, counterKey);
        // Modify counters to reflect buy is started :
        operate(getTombolaTicketSet(), counterKey, new Operation[]{Operation.get()},
                (readResult, ops) -> getCounterWriteOperations(readResult, 0, ticketsToBuy, totalAvailableTickets));
    }

    private void initializeCounters(String tombolaId, String counterKey) {
        if (!exists(getTombolaTicketSet(), counterKey)) {
            try {
                createRecord(getTombolaTicketSet(), counterKey, new Bin(TOMBOLA_PURCHASED_COUNTER_BIN_NAME, 0),
                        new Bin(TOMBOLA_LOCKED_COUNTER_BIN_NAME, 0));
            } catch (AerospikeException e) {
                if (e.getResultCode() == ResultCode.KEY_EXISTS_ERROR) {
                    LOGGER.trace("Counters record already exists for tombolaId {}, create skipped.", tombolaId);
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
	public List<String> buyTickets(String userId, String tombolaName, String imageId, String tombolaId, String tombolaDrawTargetDate,
            int totalAvailableTickets, int ticketsToBuy, BigDecimal price, Currency currency, String bundleImageId,
            String appId, String tenantId) {
		Record countersRecord;
		String counterKey = primaryKeyUtil.createSubRecordKeyByTombolaId(tombolaId, TOMBOLA_COUNTER_RECORD_NAME);

		initializeCounters(tombolaId, counterKey);
		// Modify counters to reflect buy is started :
		countersRecord = operate(getTombolaTicketSet(), counterKey, new Operation[] { Operation.get() },
				(readResult, ops) -> getCounterWriteOperations(readResult, ticketsToBuy, -ticketsToBuy, totalAvailableTickets));

		int consumingCounter = countersRecord.getInt(TOMBOLA_PURCHASED_COUNTER_BIN_NAME);

		List<String> assignedTicketIds = new ArrayList<>();
		for (int ticketId = consumingCounter - 1; ticketId >= consumingCounter - ticketsToBuy; ticketId--) {
		    String primaryKey = primaryKeyUtil.createTombolaTicketKey(tombolaId, Integer.toString(ticketId));
			assignedTicketIds.add(primaryKey);
			// create the tickets as they are not created by the configurator :
            createTombolaTicket(userId, appId, tombolaId, ticketId, primaryKey);
		}
		int firstTicketId = consumingCounter - ticketsToBuy;
        updateUsrTombolaBinEntry(userId, tombolaName, imageId, tombolaId, tombolaDrawTargetDate, ticketsToBuy, price, currency,
                bundleImageId, firstTicketId);
        updateHistoryBinMap(userId, tombolaName, imageId, tombolaId, tombolaDrawTargetDate, ticketsToBuy, price, currency,
                bundleImageId, firstTicketId);
        updateStatisticsBin(userId, ticketsToBuy, appId, tenantId);
		return assignedTicketIds;
	}

	@Override
    public List<String> getUserStatisticsAppIds(String userId) {
        String key = primaryKeyUtil.createUserTombolaStatisticsAppIdsListKey(userId);
        return readList(getUserTombolaSet(), key, STATISTICS_BIN_NAME);
    }

    @Override
    public void setUserStatisticsAppIds(String userId, List<String> appIds) {
        String key = primaryKeyUtil.createUserTombolaStatisticsAppIdsListKey(userId);
        this.<String> createOrUpdateList(getUserTombolaSet(), key, STATISTICS_BIN_NAME,
                (readResult, writePolicy) -> appIds);
    }

    @Override
    public void deleteUserStatisticsAppIds(String userId) {
        String key = primaryKeyUtil.createUserTombolaStatisticsAppIdsListKey(userId);
        deleteSilently(getUserTombolaSet(), key);
    }

    @Override
    public List<String> getUserStatisticsTenantIds(String userId) {
        String key = primaryKeyUtil.createUserTombolaStatisticsTenantIdsListKey(userId);
        return readList(getUserTombolaSet(), key, STATISTICS_BIN_NAME);
    }

    @Override
    public void setUserStatisticsTenantIds(String userId, List<String> tenantIds) {
        String key = primaryKeyUtil.createUserTombolaStatisticsTenantIdsListKey(userId);
        this.<String> createOrUpdateList(getUserTombolaSet(), key, STATISTICS_BIN_NAME,
                (readResult, writePolicy) -> tenantIds);
    }

    @Override
    public void deleteUserStatisticsTenantIds(String userId) {
        String key = primaryKeyUtil.createUserTombolaStatisticsTenantIdsListKey(userId);
        deleteSilently(getUserTombolaSet(), key);
    }

    @Override
    public void updateUserStatisticsForTenantId(String userId, String tenantId, UserStatistics userStatistics) {
        String key = primaryKeyUtil.createUserTombolaStatisticsByTenantIdKey(userId, tenantId);
        createOrUpdateJson(getUserTombolaSet(), key, STATISTICS_BIN_NAME,
                (existing, wp) -> jsonUtil.toJson(userStatistics));
    }

    @Override
    public void deleteUserStatisticsForTenantId(String userId, String tenantId) {
        String key = primaryKeyUtil.createUserTombolaStatisticsByTenantIdKey(userId, tenantId);
        deleteSilently(getUserTombolaSet(), key);
    }

    @Override
    public void updateUserStatisticsForAppId(String userId, String appId, UserStatistics userStatistics) {
        String key = primaryKeyUtil.createUserTombolaStatisticsByAppIdKey(userId, appId);
        createOrUpdateJson(getUserTombolaSet(), key, STATISTICS_BIN_NAME,
                (existing, wp) -> jsonUtil.toJson(userStatistics));
    }

    @Override
    public void deleteUserStatisticsForAppId(String userId, String appId) {
        String key = primaryKeyUtil.createUserTombolaStatisticsByAppIdKey(userId, appId);
        deleteSilently(getUserTombolaSet(), key);
    }

    @Override
    public void updateDrawing(String tombolaId, TombolaDrawing drawing) {
        String tombolaKey = primaryKeyUtil.createPrimaryKey(tombolaId);
        createOrUpdateJson(getTombolaSet(), tombolaKey, DRAWING_BIN_NAME,
                (existing, wp) -> jsonUtil.toJson(drawing));

        LocalDate drawDate = DateTimeUtil.parseISODateTimeString(drawing.getDrawDate()).toLocalDate();
        List<String> applicationIds = getTombola(tombolaId).getApplicationsIds();
        updateDrawingLists(drawing, drawDate, applicationIds);
    }

    private void updateDrawingLists(TombolaDrawing drawing, LocalDate drawDate, List<String> applicationIds) {
        String set = getTombolaDrawingListSet();
        applicationIds.forEach(appId -> {
            String key = primaryKeyUtil.createTombolaDrawingPrimaryKey(appId, drawDate.toString());
            createOrUpdateMapValueByKey(set, key, TOMBOLA_DRAWING_LIST_BIN_NAME, drawing.getTombolaId(),
                    (existing, writePolicy) -> jsonUtil.toJson(drawing));
        });
    }

    private void updateStatisticsBin(String userId, int ticketsToBuy, String appId, String tenantId) {
        createOrUpdateJson(getUserTombolaSet(), primaryKeyUtil.createUserTombolaStatisticsByAppIdKey(userId, appId),
                STATISTICS_BIN_NAME, (existing, wp) -> getUpdatedStatistics(ticketsToBuy, existing));

        createOrUpdateJson(getUserTombolaSet(),
                primaryKeyUtil.createUserTombolaStatisticsByTenantIdKey(userId, tenantId), STATISTICS_BIN_NAME,
                (existing, wp) -> getUpdatedStatistics(ticketsToBuy, existing));

        // add appId to the statistics appIds list, needed for merge profiles
        createOrUpdateList(getUserTombolaSet(), primaryKeyUtil.createUserTombolaStatisticsAppIdsListKey(userId),
                STATISTICS_BIN_NAME, (existing, writePolicy) -> addUniqueItemToList(appId, existing));
        // add tenantId to the statistics tenantIds list, needed for merge profiles
        createOrUpdateList(getUserTombolaSet(), primaryKeyUtil.createUserTombolaStatisticsTenantIdsListKey(userId),
                STATISTICS_BIN_NAME, (existing, writePolicy) -> addUniqueItemToList(tenantId, existing));
    }

    private List<Object> addUniqueItemToList(String item, List<Object> existing) {
        List<Object> updated = existing;
        if (updated == null) {
            updated = new ArrayList<>();
        }
        if (!updated.contains(item)) {
            updated.add(item);
        }
        return updated;
    }

    private String getUpdatedStatistics(int ticketsToBuy, String existing) {
        UserStatistics existingUserStatistics = existing == null ? null : jsonUtil.fromJson(existing,
                UserStatistics.class);
        if(existingUserStatistics == null) {
            existingUserStatistics = new UserStatistics();
        }

        existingUserStatistics.setBoughtTicketsNumber(existingUserStatistics.getBoughtTicketsNumber() + ticketsToBuy);
        existingUserStatistics.setTicketsBoughtSinceLastWin(
                existingUserStatistics.getTicketsBoughtSinceLastWin() + ticketsToBuy);

        return jsonUtil.toJson(existingUserStatistics);
    }

    private void updateHistoryBinMap(String userId, String tombolaName, String imageId, String tombolaId, String tombolaDrawTargetDate,
            int ticketsToBuy, BigDecimal price, Currency currency, String bundleImageId, int firstTicketId) {
        PurchasedBundle bundle = createUserTombolaHistoryBundle(ticketsToBuy, price, currency, bundleImageId,
                firstTicketId);

        String historyKey = primaryKeyUtil.createUserTombolaHistoryPendingDrawingPrimaryKey(userId);
        createOrUpdateMapValueByKey(getUserTombolaSet(), historyKey, HISTORY_BIN_NAME, tombolaId,
                (existing, writePolicy) ->
                        getUpdatedUserTombolaInfo(tombolaName, imageId, tombolaId, tombolaDrawTargetDate, bundle, existing));
    }

    private String getUpdatedUserTombolaInfo(String tombolaName, String imageId, String tombolaId, String tombolaDrawTargetDate,
            PurchasedBundle bundle, Object existing) {
        UserTombolaInfo existingUserTombolaInfo =
                existing == null ? null : jsonUtil.fromJson((String) existing, UserTombolaInfo.class);
        if (existingUserTombolaInfo == null) {
            existingUserTombolaInfo = createUserTombolaHistory(tombolaName, imageId, tombolaId, tombolaDrawTargetDate,
                    bundle);
        } else {
            existingUserTombolaInfo.getBundles().add(bundle);
            int totalTicketsBought = existingUserTombolaInfo.getTotalTicketsBought() + bundle.getAmount();
            existingUserTombolaInfo.setTotalTicketsBought(totalTicketsBought);
        }
        return jsonUtil.toJson(existingUserTombolaInfo);
    }

    private void updateUsrTombolaBinEntry(String userId, String tombolaName, String imageId, String tombolaId,
            String tombolaDrawTargetDate, int ticketsToBuy,
            BigDecimal price, Currency currency, String bundleImageId, int firstTicketId) {
        PurchasedBundle bundle = createUserTombolaHistoryBundle(ticketsToBuy, price, currency, bundleImageId,
                firstTicketId);

        String historyKey = primaryKeyUtil.createUserTombolaPrimaryKey(userId, tombolaId);
        createOrUpdateJson(getUserTombolaSet(), historyKey, USER_TOMBOLA_BIN_NAME, (existing, wp) ->
                getUpdatedUserTombolaInfo(tombolaName, imageId, tombolaId, tombolaDrawTargetDate, bundle, existing));
    }

    private UserTombolaInfo createUserTombolaHistory(String tombolaName, String imageId, String tombolaId, String tombolaDrawTargetDate,
            PurchasedBundle bundle) {
        List<PurchasedBundle> bundles = new ArrayList<>();
        bundles.add(bundle);
        return new UserTombolaInfo(tombolaId, tombolaName, imageId, tombolaDrawTargetDate, true, bundle.getAmount(), bundles);
    }

    private PurchasedBundle createUserTombolaHistoryBundle(int ticketsToBuy, BigDecimal price, Currency currency,
            String bundleImageId, int firstTicketId) {
        return new PurchasedBundle(ticketsToBuy, price, currency, bundleImageId, firstTicketId,
                DateTimeUtil.getCurrentTimestampInISOFormat());
    }

    private void createTombolaTicket(String userId, String appId, String tombolaId, int ticketId, String primaryKey) {
        TombolaTicket ticket = new TombolaTicket();
        ticket.setUserId(userId);
        ticket.setAppId(appId);
        ticket.setCode(this.generateTombolaTicketCode(tombolaId, Integer.toString(ticketId)));
        ticket.setObtainDate(DateTimeUtil.getCurrentTimestampInISOFormat());
        createJson(getTombolaTicketSet(), primaryKey, TOMBOLA_TICKET_BIN_NAME, jsonUtil.toJson(ticket));
    }

    @Override
    public void cancelBuyTickets(String userId, String tombolaId, int totalAvailableTickets, int ticketsToBuy) {
        String counterKey = primaryKeyUtil.createSubRecordKeyByTombolaId(tombolaId, TOMBOLA_COUNTER_RECORD_NAME);

        initializeCounters(tombolaId, counterKey);
        // Modify counters to unlock the tickets:
        operate(getTombolaTicketSet(), counterKey, new Operation[] { Operation.get() },
                (readResult, ops) -> getCounterWriteOperations(readResult, 0, -ticketsToBuy, totalAvailableTickets));

    }

    private List<Operation> getCounterWriteOperations(Record readResult, int consumingCounterOffset,
                                                      int lockCounterOffset, int totalAvailable) {
        List<Operation> writeOperations = new ArrayList<>();

        int currentConsumingCounter = readResult.getInt(TOMBOLA_PURCHASED_COUNTER_BIN_NAME);
        int currentLockCounter = readResult.getInt(TOMBOLA_LOCKED_COUNTER_BIN_NAME);

        if (consumingCounterOffset != 0) {
            validateConsumingCounterIncrement(consumingCounterOffset, currentConsumingCounter, totalAvailable);
            Bin bin = getLongBin(TOMBOLA_PURCHASED_COUNTER_BIN_NAME, consumingCounterOffset);
            writeOperations.add(Operation.add(bin));
        }
        if (lockCounterOffset != 0) {
            validateLockCounterChange(lockCounterOffset, currentConsumingCounter, currentLockCounter, totalAvailable);
            Bin bin = getLongBin(TOMBOLA_LOCKED_COUNTER_BIN_NAME, lockCounterOffset);
            writeOperations.add(Operation.add(bin));
        }

        return writeOperations;
    }

    private void validateLockCounterChange(int lockCounterOffset, int currentConsumingCounter, int currentLockCounter,
            int currentCreationCounter) {
        if (lockCounterOffset > 0 && (currentConsumingCounter + currentLockCounter + lockCounterOffset) > currentCreationCounter
                && currentCreationCounter > -1 ) { // currentCreationCounter=-1 - infinity tickets
            throw new TombolaNoLongerAvailableException("Not enough Tombola tickets available to be locked");
        }
        if (lockCounterOffset < 0 && currentLockCounter + lockCounterOffset < 0) {
            throw new TombolaNoLongerAvailableException("Tombola tickets lock no longer available to be released");
        }
    }

	private void validateConsumingCounterIncrement(int consumingCounterOffset, int currentConsumingCounter,
            int currentCreationCounter) {
        if (consumingCounterOffset > 0 && (currentConsumingCounter + consumingCounterOffset) > currentCreationCounter
                && currentCreationCounter > -1 ) { // currentCreationCounter=-1 - infinity tickets
            throw new TombolaNoLongerAvailableException("Not enough Tombola tickets available to be assigned");
        }
    }

    private String generateTombolaTicketCode(String tombolaId, String tombolaTicketCounter) {
        String toHash = tombolaId + tombolaTicketCounter;
        String hashCode = Integer.toString(toHash.hashCode());
        if(hashCode.length() > 12) {
            return hashCode.substring(0, 12);
        } else {
            return hashCode;
        }
    }

    @Override
    public List<TombolaBuyer> getTombolaBuyerList(String tombolaId) {
        int purchasedTicketsNumber = this.getPurchasedTicketsNumber(tombolaId);
        List<TombolaBuyer> tombolaBuyerList = new  ArrayList<>();

        Map<String, Boolean> addedBuyerMap = new HashMap<>();

        IntStream.range(0, purchasedTicketsNumber).forEach(i -> {
            TombolaTicket ticket = this.getTombolaTicket(tombolaId, String.valueOf(i));
            if(addedBuyerMap.get(ticket.getUserId()) == null) {
                UserTombolaInfo userTombolaInfo = getUserTombola(ticket.getUserId(), tombolaId);
                BigDecimal totalPrice = BigDecimal.ZERO;
                for (Bundle item : userTombolaInfo.getBundles()) {
                    totalPrice = totalPrice.add(item.getPrice());
                }
                tombolaBuyerList.add(new TombolaBuyer(ticket.getUserId(), userTombolaInfo.getTotalTicketsBought(),
                        totalPrice, userTombolaInfo.getTotalPrizesWon()));
                addedBuyerMap.put(ticket.getUserId(), true);
            }
        });

        return tombolaBuyerList;
    }
}
