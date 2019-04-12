package de.ascendro.f4m.service.tombola;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.analytics.model.TombolaEndAnnouncementEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.voucher.CommonVoucherAerospikeDao;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.tombola.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.tombola.client.PrizePayoutRequestInfo;
import de.ascendro.f4m.service.tombola.dao.TombolaAerospikeDao;
import de.ascendro.f4m.service.tombola.model.Bundle;
import de.ascendro.f4m.service.tombola.model.Prize;
import de.ascendro.f4m.service.tombola.model.PrizeType;
import de.ascendro.f4m.service.tombola.model.Tombola;
import de.ascendro.f4m.service.tombola.model.TombolaBuyer;
import de.ascendro.f4m.service.tombola.model.TombolaDrawing;
import de.ascendro.f4m.service.tombola.model.TombolaStatus;
import de.ascendro.f4m.service.tombola.model.TombolaWinner;
import de.ascendro.f4m.service.tombola.model.UserStatistics;
import de.ascendro.f4m.service.tombola.model.UserTombolaInfo;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingListResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaBuyerListRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaBuyerListResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaListRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaListResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaWinnerListRequest;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaListResponse;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.voucher.model.Voucher;


public class TombolaManager {

    private final TombolaAerospikeDao tombolaAerospikeDao;
    private final DependencyServicesCommunicator dependencyServicesCommunicator;
    private final Tracker tracker;
    private final CommonProfileAerospikeDao commonProfileDao;
    private final CommonVoucherAerospikeDao commonVoucherDao;

	@Inject
	public TombolaManager(TombolaAerospikeDao tombolaAerospikeDao,
			DependencyServicesCommunicator dependencyServicesCommunicator, Tracker tracker,
			CommonProfileAerospikeDao commonProfileDao, CommonVoucherAerospikeDao commonVoucherDao) {
		this.tombolaAerospikeDao = tombolaAerospikeDao;
		this.dependencyServicesCommunicator = dependencyServicesCommunicator;
		this.tracker = tracker;
		this.commonProfileDao = commonProfileDao;
		this.commonVoucherDao = commonVoucherDao;
	}

    public TombolaListResponse getTombolaListResponseByAppId(TombolaListRequest request, String appId){
        List<Tombola> tombolas = tombolaAerospikeDao.getAvailableTombolaList(appId);
        int total = tombolas.size();
        tombolas = removeExtraItemsFromTombolaList(tombolas, request.getOffset(), request.getLimit());
        tombolas.forEach(this::fillPurchasedTicketsAmount);
        return new TombolaListResponse(request.getLimit(), request.getOffset(), total, tombolas);
    }

    private void fillPurchasedTicketsAmount(Tombola tombola) {
        int purchasedAmount = tombolaAerospikeDao.getPurchasedTicketsNumber(tombola.getId()) + tombolaAerospikeDao.getBoostTicketsNumber(tombola.getId());
        purchasedAmount = (purchasedAmount > tombola.getTotalTicketsAmount() && tombola.getTotalTicketsAmount() > -1) ? tombola.getTotalTicketsAmount() : purchasedAmount;
        if (purchasedAmount == -1) purchasedAmount = 0;
        tombola.setPurchasedTicketsAmount(purchasedAmount);
    }

    private List<Tombola> removeExtraItemsFromTombolaList(
            List<Tombola> tombolaList, long offset, int limit) {
        return tombolaList.stream()
                .skip(offset < 0 ? 0 : offset)
                .limit(limit < 0 ? 0 : limit)
                .collect(Collectors.toList());
    }

    public Tombola getTombola(String tombolaId) {
        Tombola tombola = tombolaAerospikeDao.getTombola(tombolaId);
        this.fillPurchasedTicketsAmount(tombola);
        return tombola;
    }

    private void setTombolaStatus(String tombolaId, TombolaStatus status) {
        tombolaAerospikeDao.setTombolaStatus(tombolaId, status);
    }

    private void addTombolaToAvailableTombolaList(String tombolaId) {
        Tombola tombola = tombolaAerospikeDao.getTombola(tombolaId);
        tombolaAerospikeDao.addTombolaToAvailableTombolaList(tombola, tombola.getApplicationsIds());
    }

    public TombolaDrawingListResponse getTombolaDrawingListResponse(String appId, long offset, int limit,
            ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        TombolaDrawingListResponse response = tombolaAerospikeDao.getTombolaDrawings(appId, offset, limit, dateFrom, dateTo);
        response.getItems().forEach(item -> {
            item.setWinners(populateTombolaWinnerVoucherData(item.getWinners()));
        });

        return response;
    }

    public TombolaDrawing getTombolaDrawing(String tombolaId) {
        TombolaDrawing tombolaDrawing = tombolaAerospikeDao.getTombolaDrawing(tombolaId);
        if (tombolaDrawing == null) {
            throw new F4MEntryNotFoundException("Specified Tombola does not exist");
        }

        tombolaDrawing.setWinners(populateTombolaWinnerVoucherData(tombolaDrawing.getWinners()));

        return tombolaDrawing;
    }

	private List<TombolaWinner> populateTombolaWinnerVoucherData(List<TombolaWinner> tombolaWinners) {
		Map<String, Voucher> voucherMap = new HashMap<>();
		tombolaWinners.forEach(winner -> {
			if (winner.getVoucherId() != null) {

				if (voucherMap.get(winner.getVoucherId()) != null) {
					winner.setVoucher(voucherMap.get(winner.getVoucherId()));
				} else {
					Voucher voucher = commonVoucherDao.getVoucherById(winner.getVoucherId());

					winner.setVoucher(voucher);
					voucherMap.put(winner.getVoucherId(), voucher);

				}

			}
		});
        return tombolaWinners;
    }

    public UserTombolaListResponse getUserTombolaListResponse(String userId, long offset, int limit,
            ZonedDateTime dateFrom, ZonedDateTime dateTo, boolean isPendingDrawing) {
        return tombolaAerospikeDao.getUserTombolaList(userId, offset, limit, dateFrom, dateTo, isPendingDrawing);
    }

    public UserTombolaInfo getUserTombola(String userId, String tombolaId) {
        UserTombolaInfo userTombola = tombolaAerospikeDao.getUserTombola(userId, tombolaId);
        if (userTombola == null) {
            userTombola = new UserTombolaInfo();
        }
        return userTombola;
    }

    public Bundle getBundleToPurchase(Tombola tombola, Integer tickets) {
        Bundle purchaseBundle = null;

        for(Bundle bundle : tombola.getBundles()) {
            if(tickets.equals(bundle.getAmount())) {
                purchaseBundle = bundle;
                break;
            }
        }

        return purchaseBundle;
    }

    public void lockTickets(Bundle bundle, String userId, Tombola tombola) {
        tombolaAerospikeDao.lockTickets(userId, tombola.getId(), tombola.getTotalTicketsAmount(), bundle.getAmount());
    }

    public void buyTickets(int ticketsNumber, String userId, String tombolaId, BigDecimal price, Currency currency,
            String bundleImageId, String appId, String tenantId) {
        Tombola tombola = tombolaAerospikeDao.getTombola(tombolaId);
        tombolaAerospikeDao.buyTickets(userId, tombola.getName(), tombola.getImageId(), tombola.getId(), tombola.getTargetDate(),
                tombola.getTotalTicketsAmount(), ticketsNumber, price, currency, bundleImageId, appId, tenantId);
    }

    public void cancelBuyTickets(int ticketsNumber, String userId, String tombolaId) {
        Tombola tombola = tombolaAerospikeDao.getTombola(tombolaId);
        tombolaAerospikeDao.cancelBuyTickets(userId, tombola.getId(), tombola.getTotalTicketsAmount(), ticketsNumber);
    }

    public void initiateBuyTicketsPaymentTransfer(JsonMessage<? extends JsonMessageContent> message, Bundle bundle,
                                        SessionWrapper sessionWrapper, String userId, String tombolaId, String appId,
                                        String tenantId) {
        dependencyServicesCommunicator.initiateTombolaBuyTicketsPayment(message, sessionWrapper, userId, bundle,
                tombolaId, appId, tenantId);
    }

    public void moveTombolas(String sourceUserId, String targetUserId) {
        moveUserDataForDrawnTombolas(sourceUserId, targetUserId);

        moveUserDataForPendingTombolas(sourceUserId, targetUserId);

        moveUserDrawnMonthsList(sourceUserId, targetUserId);

        moveUserStatisticsByTenantIds(sourceUserId, targetUserId);
        moveUserStatisticsByAppIds(sourceUserId, targetUserId);

        moveUserStatisticsTentantsList(sourceUserId, targetUserId);
        moveUserStatisticsAppsList(sourceUserId, targetUserId);
    }

    private void moveUserStatisticsTentantsList(String sourceUserId, String targetUserId) {
        List<String> sourceTenantsList = tombolaAerospikeDao.getUserStatisticsTenantIds(sourceUserId);
        if (sourceTenantsList != null) {
            List<String> targetTenantsList = tombolaAerospikeDao.getUserStatisticsTenantIds(targetUserId);
            Set<String> mergedSet = new HashSet<>();
            mergedSet.addAll(sourceTenantsList);
            if (targetTenantsList != null) {
                mergedSet.addAll(targetTenantsList);
            }
            tombolaAerospikeDao.setUserStatisticsTenantIds(targetUserId, new ArrayList<>(mergedSet));
            tombolaAerospikeDao.deleteUserStatisticsTenantIds(sourceUserId);
        }
    }

    private void moveUserStatisticsAppsList(String sourceUserId, String targetUserId) {
        List<String> sourceAppsList = tombolaAerospikeDao.getUserStatisticsAppIds(sourceUserId);
        if (sourceAppsList != null) {
            List<String> targetAppsList = tombolaAerospikeDao.getUserStatisticsAppIds(targetUserId);
            Set<String> mergedSet = new HashSet<>();
            mergedSet.addAll(sourceAppsList);
            if (targetAppsList != null) {
                mergedSet.addAll(targetAppsList);
            }
            tombolaAerospikeDao.setUserStatisticsAppIds(targetUserId, new ArrayList<>(mergedSet));
            tombolaAerospikeDao.deleteUserStatisticsAppIds(sourceUserId);
        }
    }

    private void moveUserStatisticsByAppIds(String sourceUserId, String targetUserId) {
        List<String> sourceAppIds = tombolaAerospikeDao.getUserStatisticsAppIds(sourceUserId);
        if (sourceAppIds != null) {
            sourceAppIds.forEach(appId -> {
                UserStatistics sourceStatistics = tombolaAerospikeDao.getUserStatisticsByAppId(sourceUserId,
                        appId);
                UserStatistics targetStatistics = tombolaAerospikeDao.getUserStatisticsByAppId(targetUserId,
                        appId);
                UserStatistics mergedStats = mergeUserStatistics(sourceStatistics, targetStatistics);
                if (mergedStats != null) {
                    tombolaAerospikeDao.updateUserStatisticsForAppId(targetUserId, appId, mergedStats);
                    tombolaAerospikeDao.deleteUserStatisticsForAppId(sourceUserId, appId);
                }
            });
        }
    }

    private void moveUserStatisticsByTenantIds(String sourceUserId, String targetUserId) {
        List<String> sourceTenantIds = tombolaAerospikeDao.getUserStatisticsTenantIds(sourceUserId);
        if (sourceTenantIds != null) {
            sourceTenantIds.forEach(tenantId -> {
                UserStatistics sourceStatistics = tombolaAerospikeDao.getUserStatisticsByTenantId(sourceUserId,
                        tenantId);
                UserStatistics targetStatistics = tombolaAerospikeDao.getUserStatisticsByTenantId(targetUserId,
                        tenantId);
                UserStatistics mergedStats = mergeUserStatistics(sourceStatistics, targetStatistics);
                if (mergedStats != null) {
                    tombolaAerospikeDao.updateUserStatisticsForTenantId(targetUserId, tenantId, mergedStats);
                    tombolaAerospikeDao.deleteUserStatisticsForTenantId(sourceUserId, tenantId);
                }
            });
        }
    }

    private UserStatistics mergeUserStatistics(UserStatistics sourceStatistics, UserStatistics targetStatistics) {
        UserStatistics mergedStats;

        if (sourceStatistics == null) {
            // this means that there is inconsistent data in aerospike
            return null;
        }

        if (targetStatistics == null) {
            mergedStats = sourceStatistics;
        } else {
            mergedStats = targetStatistics;

            mergeLastWinRelatedStats(sourceStatistics, mergedStats);

            mergedStats.setWinningTicketsNumber(targetStatistics.getWinningTicketsNumber() +
                    sourceStatistics.getWinningTicketsNumber());
            mergedStats.setBoughtTicketsNumber(targetStatistics.getBoughtTicketsNumber() +
                    sourceStatistics.getBoughtTicketsNumber());
        }

        return mergedStats;
    }


    private void mergeLastWinRelatedStats(UserStatistics sourceStatistics, UserStatistics targetStatistics) {
        String sourceWinDateString = sourceStatistics.getLastWinDate();
        if (sourceWinDateString != null) {
            String targetWinDateString = targetStatistics.getLastWinDate();
            if (targetWinDateString == null || DateTimeUtil.parseISODateTimeString(sourceWinDateString)
                    .isAfter(DateTimeUtil.parseISODateTimeString(targetWinDateString))) {
                targetStatistics.setLastWinDate(sourceWinDateString);
                targetStatistics.setTicketsBoughtSinceLastWin(
                        sourceStatistics.getTicketsBoughtSinceLastWin());
            }
        }
    }

    private void moveUserDrawnMonthsList(String sourceUserId, String targetUserId) {
        List<String> sourceDrawnMonthsList = tombolaAerospikeDao.getDrawnMonthsForUser(sourceUserId);
        if (sourceDrawnMonthsList != null) {
            List<String> targetDrawnMonthsList = tombolaAerospikeDao.getDrawnMonthsForUser(targetUserId);
            Set<String> mergedSet = new HashSet<>();
            mergedSet.addAll(sourceDrawnMonthsList);
            if (targetDrawnMonthsList != null) {
                mergedSet.addAll(targetDrawnMonthsList);
            }
            tombolaAerospikeDao.setDrawnMonthsForUser(targetUserId, new ArrayList<>(mergedSet));
            tombolaAerospikeDao.deleteDrawnMonthsForUser(sourceUserId);
        }
    }

    private void moveUserDataForPendingTombolas(String sourceUserId, String targetUserId) {
        moveUserDataForMap(sourceUserId, targetUserId, null, true);
    }

    /*
     * Move and merge all the already drawn user tombolas, including tickets and drawings prize user references
     */
    private void moveUserDataForDrawnTombolas(String sourceUserId, String targetUserId) {
        List<String> drawnMonthsForSourceUser =
                Optional.ofNullable(tombolaAerospikeDao.getDrawnMonthsForUser(sourceUserId))
                        .orElse(Collections.emptyList());
        for (String month : drawnMonthsForSourceUser) {
            moveUserDataForMap(sourceUserId, targetUserId, month, false);
        }
    }

    private void moveUserDataForMap(String sourceUserId, String targetUserId, String month, boolean isPending) {
        Map<String, String> sourceHistoryMap;

        // get source history MAP
        if (isPending) {
            sourceHistoryMap = tombolaAerospikeDao.getUserTombolaHistoryMapForPending(sourceUserId);
        } else {
            sourceHistoryMap = tombolaAerospikeDao.getUserTombolaHistoryMapForMonth(sourceUserId,
                    month);
        }
        // for each tombola id in source map key set
        for (String tombolaId : sourceHistoryMap.keySet()) {
            if (!isPending) {
                updatePrizeInformation(sourceUserId, targetUserId, tombolaId);
            }
            updateTicketUserReferences(sourceUserId, targetUserId, tombolaId);

            // get merged user tombola object
            UserTombolaInfo mergedUserTombolaInfo = mergeUserTombola(sourceUserId, targetUserId, tombolaId);
            // null means source user tombola info doesn't exist, so nothing to merge
            if (mergedUserTombolaInfo != null) {
                updateUserHistory(targetUserId, month, isPending, tombolaId, mergedUserTombolaInfo);
                deleteSourceUserHistory(sourceUserId, month, isPending, tombolaId);
            }
        }
    }

    private void updateUserHistory(String userId, String month, boolean isPending, String tombolaId,
            UserTombolaInfo userTombolaInfo) {
        // update target user history detail object
        tombolaAerospikeDao.updateUserTombola(userId, tombolaId, userTombolaInfo);

        // update target user history list
        if (isPending) {
            tombolaAerospikeDao.updateUserTombolaHistoryForPending(userId, tombolaId,
                    userTombolaInfo);
        } else {
            tombolaAerospikeDao.updateUserTombolaHistoryForMonth(userId, tombolaId,
                    userTombolaInfo, month);
        }
    }

    private void deleteSourceUserHistory(String userId, String month, boolean isPending, String tombolaId) {
        // delete source user history detail object
        tombolaAerospikeDao.deleteUserTombola(userId, tombolaId);

        // delete source user history list
        if (isPending) {
            tombolaAerospikeDao.deleteUserTombolaHistoryItemFromPending(userId, tombolaId);
        } else {
            tombolaAerospikeDao.deleteUserTombolaHistoryItemFromMonth(userId, tombolaId, month);
        }
    }

    /*
     * update user references in tombola drawing prize information in drawing detail and list objects
     */
    private void updatePrizeInformation(String sourceUserId, String targetUserId, String tombolaId) {
        TombolaDrawing drawing = tombolaAerospikeDao.getTombolaDrawing(tombolaId);
        Predicate<TombolaWinner> isWinnerSourceUser = winner -> StringUtils.equals(winner.getUser().getUserId(), sourceUserId);
		if (drawing != null && drawing.getWinners().stream().anyMatch(isWinnerSourceUser)) {
            drawing.getWinners().stream()
            	.filter(isWinnerSourceUser)
            	.forEach(matchingWinner -> {
            		ApiProfileBasicInfo userInfo = commonProfileDao.getProfileBasicInfo(targetUserId);
            		matchingWinner.setUser(userInfo);
            	});
            tombolaAerospikeDao.updateDrawing(tombolaId, drawing);
        }
    }

    /*
     * update user references for tickets based on bundles bought by source user
     */
    private void updateTicketUserReferences(String sourceUserId, String targetUserId, String tombolaId) {
        UserTombolaInfo sourceTombolaInfo = tombolaAerospikeDao.getUserTombola(sourceUserId, tombolaId);
        if (sourceTombolaInfo != null) {
            sourceTombolaInfo.getBundles().forEach(bundle -> tombolaAerospikeDao.updateUserTicketsForBundle(
                    sourceUserId, targetUserId, tombolaId, bundle));
        }
    }

    private UserTombolaInfo mergeUserTombola(String sourceUserId, String targetUserId, String sourceTombolaId) {
        UserTombolaInfo sourceTombolaInfo = tombolaAerospikeDao.getUserTombola(sourceUserId, sourceTombolaId);
        UserTombolaInfo targetTombolaInfo = tombolaAerospikeDao.getUserTombola(targetUserId, sourceTombolaId);

        // this means that there is inconsistent data in aerospike, so we'll ignore this tombolaId
        if (sourceTombolaInfo == null) {
            return null;
        }

        if (targetTombolaInfo == null) {
            targetTombolaInfo = sourceTombolaInfo;
        } else {
            targetTombolaInfo.setTotalTicketsBought(
                    targetTombolaInfo.getTotalTicketsBought() + sourceTombolaInfo.getTotalTicketsBought());
            targetTombolaInfo.getBundles().addAll(sourceTombolaInfo.getBundles());
            if (!targetTombolaInfo.isPending()) {
                targetTombolaInfo.setTotalPrizesWon(targetTombolaInfo.getTotalPrizesWon() +
                        sourceTombolaInfo.getTotalPrizesWon());
                targetTombolaInfo.getPrizes().addAll(sourceTombolaInfo.getPrizes());
            }
        }

        return targetTombolaInfo;
    }

    public void sendPushNotificationOnOpenTombola(Tombola tombola) {
        dependencyServicesCommunicator.initiatePushNotificationForNewTombola(tombola);
    }

    public void handleCloseCheckoutEvent(String tombolaId) {
        setTombolaStatus(tombolaId, TombolaStatus.EXPIRED);
        tombolaAerospikeDao.setTombolaStatusInAvailableTombolaList(tombolaId, TombolaStatus.EXPIRED);
    }

    public void handleOpenCheckoutEvent(String tombolaId) {
        setTombolaStatus(tombolaId, TombolaStatus.ACTIVE);
        addTombolaToAvailableTombolaList(tombolaId);
    }

    public void handlePreCloseCheckoutEvent(String tombolaId, int minutesToCheckout) {
        TombolaEndAnnouncementEvent tombolaEndAnnouncementEvent = new TombolaEndAnnouncementEvent();
        tombolaEndAnnouncementEvent.setTombolaId(Long.valueOf(tombolaId));
        tombolaEndAnnouncementEvent.setMinutesToEnd(minutesToCheckout);
        tracker.addAnonymousEvent(tombolaEndAnnouncementEvent);
    }

    /**
     * Return the drawing with the winners ordered by the published prize list
     * @param request - the request
     * @return tombolaDrawing
     */
    public TombolaDrawing getTombolaWinnerList(TombolaWinnerListRequest request) {
        TombolaDrawing tombolaDrawing = tombolaAerospikeDao.getTombolaDrawing(request.getTombolaId());
        if (tombolaDrawing == null) {
            throw new F4MEntryNotFoundException("Specified Tombola does not exist");
        }

        // get the tombola so we can perform the sorting :
        Tombola tombola = this.getTombola(request.getTombolaId());
        List<TombolaWinner> winnersSortedByPublishedPrizeList = new ArrayList<>();

        // reorder the winners by the order in which the prizes are published on tombola
        for(Prize prize : tombola.getPrizes()) {
            for(TombolaWinner winner : tombolaDrawing.getWinners()) {
                if(winner.getPrizeId().equals(prize.getId())) {
                    winnersSortedByPublishedPrizeList.add(winner);
                }
            }
        }

        for(TombolaWinner winner : tombolaDrawing.getWinners()) {
            if(winner.getPrizeId().equals(tombola.getConsolationPrize().getId())) {
                winnersSortedByPublishedPrizeList.add(winner);
            }
        }

        tombolaDrawing.setWinners(winnersSortedByPublishedPrizeList);
        return tombolaDrawing;
    }

    public TombolaBuyerListResponse getTombolaBuyerListResponse(TombolaBuyerListRequest request) {
        List<TombolaBuyer> tombolaBuyers = tombolaAerospikeDao.getTombolaBuyerList(request.getTombolaId());
        tombolaBuyers = removeExtraItemsFromTombolaBuyerList(tombolaBuyers, request.getOffset(), request.getLimit());
        return new TombolaBuyerListResponse(request.getLimit(), request.getOffset(), tombolaBuyers.size(),
                tombolaBuyers);

    }

    private List<TombolaBuyer> removeExtraItemsFromTombolaBuyerList(
            List<TombolaBuyer> tombolaBuyers, long offset, int limit) {
        return tombolaBuyers.stream()
                .skip(offset < 0 ? 0 : offset)
                .limit(limit < 0 ? 0 : limit)
                .collect(Collectors.toList());
    }

    public void sendEmailToAdmin(PrizePayoutRequestInfo prizePayoutRequestInfo, String errorMessage) {
        dependencyServicesCommunicator.sendEmailToAdmin(Messages.TOMBOLA_PRIZE_PAYOUT_ERROR_TO_ADMIN_SUBJECT, null,
                Messages.TOMBOLA_PRIZE_PAYOUT_ERROR_TO_ADMIN_CONTENT,
                getParametersFromPrizePayout(prizePayoutRequestInfo, errorMessage));
    }

    private String[] getParametersFromPrizePayout(PrizePayoutRequestInfo prizePayoutRequestInfo, String errorMessage) {
        return new String[]{
                Optional.ofNullable(errorMessage).orElse("-"),
                Optional.ofNullable(prizePayoutRequestInfo.getTombolaId()).orElse("-"),
                Optional.ofNullable(prizePayoutRequestInfo.getTransactionLogId()).orElse("-"),
                prizePayoutRequestInfo.getAmount() != null ? prizePayoutRequestInfo.getAmount().toString() : "-",
                Optional.ofNullable(PrizeType.fromCurrency(prizePayoutRequestInfo.getCurrency()))
                        .orElse(PrizeType.VOUCHER).toString(),
                Optional.ofNullable(prizePayoutRequestInfo.getTenantId()).orElse("-"),
                Optional.ofNullable(prizePayoutRequestInfo.getUserId()).orElse("-"),
                Optional.ofNullable(prizePayoutRequestInfo.getVoucherId()).orElse("-")
        };
    }
}
