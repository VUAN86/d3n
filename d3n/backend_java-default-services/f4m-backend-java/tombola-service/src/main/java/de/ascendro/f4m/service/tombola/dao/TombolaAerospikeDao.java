package de.ascendro.f4m.service.tombola.dao;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.tombola.model.PurchasedBundle;
import de.ascendro.f4m.service.tombola.model.Tombola;
import de.ascendro.f4m.service.tombola.model.TombolaBuyer;
import de.ascendro.f4m.service.tombola.model.TombolaDrawing;
import de.ascendro.f4m.service.tombola.model.TombolaStatus;
import de.ascendro.f4m.service.tombola.model.TombolaTicket;
import de.ascendro.f4m.service.tombola.model.TombolaWinner;
import de.ascendro.f4m.service.tombola.model.UserStatistics;
import de.ascendro.f4m.service.tombola.model.UserTombolaInfo;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingListResponse;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaListResponse;

public interface TombolaAerospikeDao extends AerospikeDao {

    /**
     * Get Tombolas list given an appId.
     *
     * @param appId - the App Id
     * @return a list of Tombola
     */
    List<Tombola> getAvailableTombolaList(String appId);

    void removeTombolaFromAvailableTombolaList(String tombolaId, List<String> appIdList);

    void addTombolaToAvailableTombolaList(Tombola tombola, List<String> appIdList);

    /**
     * Get a tombola item given its id
     * @param tombolaId - the tombola Id
     * @return Tombola
     */
    Tombola getTombola(String tombolaId);

    void setTombolaStatus(String tombolaId, TombolaStatus status);

    void setTombolaStatusInAvailableTombolaList(String tombolaId, TombolaStatus status);

    List<String> buyTickets(String userId, String tombolaName, String imageId, String tombolaId, String tombolaDrawTargetDate,
            int totalAvailableTickets, int ticketsToBuy, BigDecimal price, Currency currency, String bundleImageId,
            String appId, String tenantId);


    void lockTickets(String userId, String tombolaId, int totalAvailableTickets, int ticketsToBuy);

    void cancelBuyTickets(String userId, String tombolaId, int totalAvailableTickets, int ticketsToBuy);

    /**
     * Get the list of tombola drawings based on app id and time interval
     * @param appId - the app Id
     */
    TombolaDrawingListResponse getTombolaDrawings(String appId, long offset, int limit,
            ZonedDateTime dateFrom, ZonedDateTime dateTo);

    TombolaDrawing getTombolaDrawing(String tombolaId);

    /**
     * Get user tombola history for a specified interval
     */
    UserTombolaListResponse getUserTombolaList(String userId, long offset, int limit, ZonedDateTime dateFrom,
            ZonedDateTime dateTo, boolean isPendingDrawing);

    UserTombolaInfo getUserTombola(String userId, String tombolaId);

    void updateUserTombolaAfterDrawing(String userId, String tenantId, String tombolaId, String drawDate,
            List<TombolaWinner> wonPrizes);

    void updateStatisticsAfterDrawing(String userId, String drawDate, int numberOfWinningTickets, String tenantId,
            List<String> appIdList);

    TombolaTicket getTombolaTicket(String tombolaId, String ticketId);

    void createTombolaDrawing(TombolaDrawing tombolaDrawing, List<String> applicationsIds);

    void updateDrawing(String tombolaId, TombolaDrawing drawing);

    int getPurchasedTicketsNumber(String tombolaId);

    int getBoostTicketsNumber(String tombolaId);

    UserStatistics getUserStatisticsByTenantId(String userId, String tenantId);

    UserStatistics getUserStatisticsByAppId(String userId, String appId);

    void updateUserTombolaHistoryForMonth(String userId, String tombolaId, UserTombolaInfo userTombolaInfo,
            String month);

    void updateUserTombolaHistoryForPending(String userId, String tombolaId, UserTombolaInfo userTombolaInfo);

    void updateUserTombola(String userId, String tombolaId, UserTombolaInfo userTombolaInfo);

    void deleteUserTombola(String userId, String tombolaId);

    void deleteUserTombolaHistoryItemFromMonth(String userId, String tombolaId, String month);

    void deleteUserTombolaHistoryItemFromPending(String userId, String tombolaId);

    List<String> getDrawnMonthsForUser(String userId);

    void setDrawnMonthsForUser(String userId, List<String> drawnMonths);

    void deleteDrawnMonthsForUser(String userId);

    List<UserTombolaInfo> getUserTombolaHistoryForMonth(String userId, String drawMonth);

    Map<String, String> getUserTombolaHistoryMapForMonth(String userId, String drawMonth);

    List<UserTombolaInfo> getUserTombolaHistoryForPending(String userId);

    Map<String, String> getUserTombolaHistoryMapForPending(String userId);

    void updateUserTicketsForBundle(String sourceUserId, String targetUserId, String tombolaId, PurchasedBundle bundle);

    List<String> getUserStatisticsAppIds(String userId);

    void setUserStatisticsAppIds(String userId, List<String> appIds);

    void deleteUserStatisticsAppIds(String userId);

    List<String> getUserStatisticsTenantIds(String userId);

    void setUserStatisticsTenantIds(String userId, List<String> tenantIds);

    void deleteUserStatisticsTenantIds(String userId);

    void updateUserStatisticsForTenantId(String userId, String tenantId, UserStatistics userStatistics);

    void deleteUserStatisticsForTenantId(String userId, String tenantId);

    void updateUserStatisticsForAppId(String userId, String appId, UserStatistics userStatistics);

    void deleteUserStatisticsForAppId(String userId, String appId);

    List<TombolaBuyer> getTombolaBuyerList(String tombolaId);

}
