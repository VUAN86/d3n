package de.ascendro.f4m.service.tombola;

import de.ascendro.f4m.server.analytics.model.TombolaEndEvent;
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
import de.ascendro.f4m.service.tombola.exception.TombolaAlreadyDrawnException;
import de.ascendro.f4m.service.tombola.model.*;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.random.OutOfUniqueRandomNumbersException;
import de.ascendro.f4m.service.util.random.RandomSequenceGenerator;
import de.ascendro.f4m.service.util.random.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class TombolaDrawingEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(TombolaDrawingEngine.class);

    private static final int HIGH_PURCHASE_RATE = 20;

    private final TombolaAerospikeDao tombolaAerospikeDao;
    private final RandomUtil randomUtil;
    private final Tracker tracker;
    private final DependencyServicesCommunicator dependencyServicesCommunicator;
    private final CommonProfileAerospikeDao commonProfileDao;
    private final CommonVoucherAerospikeDao commonVoucherDao;

	@Inject
	public TombolaDrawingEngine(TombolaAerospikeDao tombolaAerospikeDao, RandomUtil randomUtil, Tracker tracker,
			DependencyServicesCommunicator dependencyServicesCommunicator, CommonProfileAerospikeDao commonProfileDao,
            CommonVoucherAerospikeDao commonVoucherDao) {
		this.tombolaAerospikeDao = tombolaAerospikeDao;
		this.randomUtil = randomUtil;
		this.tracker = tracker;
		this.dependencyServicesCommunicator = dependencyServicesCommunicator;
		this.commonProfileDao = commonProfileDao;
		this.commonVoucherDao = commonVoucherDao;
	}

	public void tombolaDraw(String tombolaId, String tenantId,
            JsonMessage<? extends JsonMessageContent> originalMessage) {
        Tombola tombola = tombolaAerospikeDao.getTombola(tombolaId);
        int purchasedTicketsNumber = tombolaAerospikeDao.getPurchasedTicketsNumber(tombolaId);

        checkPreDrawQuitConditions(tombolaId, tombola);

        if (purchasedTicketsNumber == 0) {
            saveDrawingWithNoTicketsResults(tombolaId, tombola);
            releaseUnassignedVouchers(originalMessage, tombola, new ArrayList<>());
            LOGGER.info("There are no tickets bought for tombola {}, drawing skipped and tombola archived.", tombolaId);
        } else {
            RandomSequenceGenerator randomSequenceGenerator = new RandomSequenceGenerator(purchasedTicketsNumber,
                    randomUtil);

            List<TombolaWinner> winners = performDrawing(tenantId, tombola, purchasedTicketsNumber,
                    randomSequenceGenerator);

            // Prepare and save objects in aerospike
            saveDrawingResults(tombolaId, tenantId, tombola, purchasedTicketsNumber, winners);

            payoutPrizes(originalMessage, tombola, tenantId, winners);

            releaseUnassignedVouchers(originalMessage, tombola, winners);

            // Call event on analytics lib to notify participants
            TombolaEndEvent tombolaEndEvent = new TombolaEndEvent();
            tombolaEndEvent.setTombolaId(Long.valueOf(tombolaId));
            tracker.addAnonymousEvent(tombolaEndEvent);
        }
    }

    private void releaseUnassignedVouchers(JsonMessage<? extends JsonMessageContent> originalMessage,
            Tombola tombola, List<TombolaWinner> winners) {
        tombola.getPrizes().forEach(prize -> {
            if (prize.getType() == PrizeType.VOUCHER) {
                releaseUnassignedVouchersForPrize(prize, winners, originalMessage);
            }
        });
        if (tombola.getConsolationPrize().getType() == PrizeType.VOUCHER) {
            releaseUnassignedVouchersForPrize(tombola.getConsolationPrize(), winners, originalMessage);
        }
        cleanReservedVouchersForTombola(tombola.getId());
    }

    private void releaseUnassignedVouchersForPrize(Prize prize, List<TombolaWinner> winners,
            JsonMessage<? extends JsonMessageContent> sourceMessage) {
        int numberOfIntendedDraws = prize.getDraws();
        int numberOfActualDraws = getNumberOfActualDrawsForPrize(prize.getId(), winners);
        if (numberOfIntendedDraws > numberOfActualDraws) {
            String voucherId = prize.getVoucherId();
            IntStream.range(0, numberOfIntendedDraws - numberOfActualDraws).forEach(i ->
                    dependencyServicesCommunicator.requestUserVoucherRelease(sourceMessage, voucherId));
        }
    }

    private void cleanReservedVouchersForTombola(String tombolaId){
        commonVoucherDao.cleanTombolaVouchers(tombolaId);
    }

    private int getNumberOfActualDrawsForPrize(String prizeId, List<TombolaWinner> winners) {
        // number of draws is int, so number of assignations cannot be larger than int -> cast is ok
        return (int) winners.stream().filter(tombolaWinner -> tombolaWinner.getPrizeId().equals(prizeId)).count();
    }

    private void payoutPrizes(JsonMessage<? extends JsonMessageContent> originalMessage,
            Tombola tombola, String tenantId, List<TombolaWinner> winners) {
        winners.forEach(tombolaWinner -> initiatePrizePayout(originalMessage, tombola, tenantId, tombolaWinner));
    }

    private void initiatePrizePayout(JsonMessage<? extends JsonMessageContent> sourceMessage,
            Tombola tombola, String tenantId, TombolaWinner tombolaWinner) {
        final PrizePayoutRequestInfo requestInfo = new PrizePayoutRequestInfo(tombola, tombolaWinner);
        requestInfo.setSourceMessage(sourceMessage);
        requestInfo.setSourceMessageSource(sourceMessage.getMessageSource());
        if (PrizeType.VOUCHER == tombolaWinner.getType()) {
            requestInfo.setTenantId(tenantId);
            requestInfo.setAppId(tombolaWinner.getAppId());
            requestInfo.setVoucherId(tombolaWinner.getVoucherId());
            requestInfo.setUserId(tombolaWinner.getUser().getUserId());
            dependencyServicesCommunicator.requestUserVoucherAssign(tombola.getName(), requestInfo);
        } else {
            Currency currency = tombolaWinner.getType().toCurrency();
            requestInfo.setAmount(tombolaWinner.getAmount());
            requestInfo.setCurrency(currency);
            requestInfo.setTenantId(tenantId);
            requestInfo.setAppId(tombolaWinner.getAppId());
            requestInfo.setUserId(tombolaWinner.getUser().getUserId());
            dependencyServicesCommunicator.initiatePrizePayment(requestInfo);
        }
    }

    private void checkPreDrawQuitConditions(String tombolaId, Tombola tombola) {
        if (tombola == null) {
            throw new F4MEntryNotFoundException("There is no tombola with id " + tombolaId + " available!");
        }
        if (tombola.getStatus() == TombolaStatus.ARCHIVED) {
            throw new TombolaAlreadyDrawnException("Tombola with id " + tombolaId + " was already drawn " +
                    "(status is Archived)!");
        }
    }

    private List<TombolaWinner> performDrawing(String tenantId, Tombola tombola, int purchasedTicketsNumber,
            RandomSequenceGenerator randomSeqGenForTickets) {
        List<TombolaWinner> winners = new ArrayList<>();

        // Draw prizes
        winners.addAll(drawPrizes(tombola, purchasedTicketsNumber, randomSeqGenForTickets));

        // CON-89 - change paying consolation prize
        // Draw consolation prizes
        // slow - a full ticket list parse is required to determine high rollers that have a guarantied consolation win
//        winners.addAll(drawConsolationPrizes(tenantId, tombola, winners, purchasedTicketsNumber,
//                randomSeqGenForTickets));
        return winners;
    }

    private void saveDrawingResults(String tombolaId, String tenantId, Tombola tombola, int purchasedTicketsNumber,
            List<TombolaWinner> winners) {
        tombolaAerospikeDao.removeTombolaFromAvailableTombolaList(tombolaId, tombola.getApplicationsIds());

        TombolaDrawing drawing = createTombolaDrawing(tombola, purchasedTicketsNumber, winners);
        tombolaAerospikeDao.createTombolaDrawing(drawing, tombola.getApplicationsIds());

        //TODO: optimization in future task: keep list of users participating in tombola separately in aerospike
        // to be able to identify losing users easily as well

        // slow - will need to do another full ticket list parse to update all objects
        Set<String> handledUserIds = new HashSet<>();
        IntStream.range(0, purchasedTicketsNumber).forEach(ticketId ->
                handleUserDataForTicket(ticketId, tombolaId, tenantId, tombola, winners, drawing, handledUserIds));

        tombolaAerospikeDao.setTombolaStatus(tombolaId, TombolaStatus.ARCHIVED);
    }

    private void saveDrawingWithNoTicketsResults(String tombolaId, Tombola tombola) {
        tombolaAerospikeDao.removeTombolaFromAvailableTombolaList(tombolaId, tombola.getApplicationsIds());

        TombolaDrawing drawing = createTombolaDrawing(tombola, 0, new ArrayList<>());
        tombolaAerospikeDao.createTombolaDrawing(drawing, tombola.getApplicationsIds());

        tombolaAerospikeDao.setTombolaStatus(tombolaId, TombolaStatus.ARCHIVED);
    }

    private void handleUserDataForTicket(int ticketId, String tombolaId, String tenantId, Tombola tombola,
            List<TombolaWinner> winners, TombolaDrawing drawing, Set<String> handledUserIds) {
        TombolaTicket tombolaTicket = tombolaAerospikeDao.getTombolaTicket(tombolaId, String.valueOf(ticketId));

        if (isTicketValidAndUserNotHandled(handledUserIds, tombolaTicket)) {
            List<TombolaWinner> matchingWinners = getMatchingWinnersForTicketOwner(winners, tombolaTicket);
            // for all participating users, once: update drawing details and history list
            tombolaAerospikeDao.updateUserTombolaAfterDrawing(tombolaTicket.getUserId(), tenantId, tombolaId,
                    drawing.getDrawDate(), matchingWinners);

            tombolaAerospikeDao.updateStatisticsAfterDrawing(tombolaTicket.getUserId(), drawing.getDrawDate(),
                    matchingWinners.size(), tenantId, tombola.getApplicationsIds());

            handledUserIds.add(tombolaTicket.getUserId());
        }
    }

    private List<TombolaWinner> getMatchingWinnersForTicketOwner(List<TombolaWinner> winners,
            TombolaTicket tombolaTicket) {
        return winners.stream().filter(tombolaWinner -> tombolaWinner.getUser().getUserId().equals(tombolaTicket.getUserId()))
                    .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean isTicketValidAndUserNotHandled(Set<String> handledUserIds, TombolaTicket tombolaTicket) {
        return tombolaTicket != null && tombolaTicket.getUserId() != null &&
                !handledUserIds.contains(tombolaTicket.getUserId());
    }

    private TombolaDrawing createTombolaDrawing(Tombola tombola, int purchasedTicketsNumber,
            List<TombolaWinner> winners) {
        return new TombolaDrawing(tombola, purchasedTicketsNumber, winners,
                DateTimeUtil.getCurrentTimestampInISOFormat());
    }

    private List<TombolaWinner> drawPrizes(Tombola tombola, int poolSize,
            RandomSequenceGenerator randomSeqGenForTickets) {
        List<TombolaWinner> winners = new ArrayList<>();
        List<Prize> prizeListForExtraction = preparePrizeListForExtraction(tombola);
        Prize consolationPrize = tombola.getConsolationPrize();
        if (consolationPrize.getDraws()>0) prizeListForExtraction.add(consolationPrize);   // CON-89 - change paying consolation prize

        final RandomSequenceGenerator randomSeqGenForPrizes = new RandomSequenceGenerator(
                prizeListForExtraction.size(), randomUtil);

        while (randomSeqGenForPrizes.hasNumbersLeft() && randomSeqGenForTickets.hasNumbersLeft()) {
            try {
                Prize prizeForExtraction = prizeListForExtraction.get(randomSeqGenForPrizes.nextInt());
                while (randomSeqGenForTickets.hasNumbersLeft()) {
                    TombolaWinner winner = extractWinnerForPrize(tombola.getId(), prizeForExtraction,
                            randomSeqGenForTickets, poolSize);
                    if (!winners.contains(winner)) {
                        winners.add(winner);
                        break;
                    }
                }
            } catch (OutOfUniqueRandomNumbersException e) {
                // Should not be reached since we check hasNumbersLeft, but in this case just end the loop
                LOGGER.error("Ran out of random numbers even though we shouldn't have.", e);
                break;
            }
        }
        return winners;
    }

    private List<Prize> preparePrizeListForExtraction(Tombola tombola) {
        List<Prize> prizeListForExtraction = new ArrayList<>();
        tombola.getPrizes().forEach(prize -> addPrizesForExtraction(prizeListForExtraction, prize));
        return prizeListForExtraction;
    }

    private void addPrizesForExtraction(List<Prize> prizeListForExtraction, Prize prize) {
        IntStream.range(0, prize.getDraws()).forEach(i -> {
            Prize prizeForExtraction = new Prize(prize.getId(), prize.getName(), prize.getType(), prize.getAmount(),
                    prize.getImageId(), prize.getVoucherId(), 1);
            prizeListForExtraction.add(prizeForExtraction);
        });
    }

    private List<TombolaWinner> drawConsolationPrizes(String tenantId, Tombola tombola,
            List<TombolaWinner> existingWinners, int poolSize, RandomSequenceGenerator randomSeqGenForTickets) {
        List<TombolaWinner> consolationWinners = new ArrayList<>();

        List<TombolaTicket> highPriorityTickets = getHighPriorityTickets(tenantId, tombola.getId(), poolSize,
                existingWinners);
        final RandomSequenceGenerator randomSeqGenForHighPriority = new RandomSequenceGenerator(
                highPriorityTickets.size(), randomUtil);

        Prize consolationPrize = tombola.getConsolationPrize();

        int numberOfHighPriorityExtractions = consolationPrize.getDraws() < highPriorityTickets.size() ?
                consolationPrize.getDraws() : highPriorityTickets.size();
        extractHighPriorityTickets(consolationWinners, highPriorityTickets, randomSeqGenForHighPriority,
                consolationPrize, numberOfHighPriorityExtractions);

        int remainingExtractions = consolationPrize.getDraws() > numberOfHighPriorityExtractions ?
                consolationPrize.getDraws() - numberOfHighPriorityExtractions : 0;
        extractRemainingConsolationPrizes(tombola, poolSize, randomSeqGenForTickets, consolationWinners,
                highPriorityTickets, consolationPrize, remainingExtractions);

        return consolationWinners;
    }

    private void extractRemainingConsolationPrizes(Tombola tombola, int poolSize,
            RandomSequenceGenerator randomSeqGenForTickets, List<TombolaWinner> consolationWinners,
            List<TombolaTicket> highPriorityTickets, Prize consolationPrize, int remainingExtractions) {
        List<Integer> ticketsToExclude = highPriorityTickets.stream().map(TombolaTicket::getId)
                .collect(Collectors.toCollection(ArrayList::new));
        int remainingExtractionsAux = remainingExtractions;
        while (randomSeqGenForTickets.hasNumbersLeft() && remainingExtractionsAux > 0) {
            TombolaTicket extractedTicket = null;
            try {
                extractedTicket = getRandomTicketFromPool(tombola.getId(), randomSeqGenForTickets,
                        poolSize, ticketsToExclude);
            } catch (OutOfUniqueRandomNumbersException e) {
                // Should not be reached since we check hasNumbersLeft, but in this case just end the loop
                LOGGER.error("Ran out of random numbers even though we shouldn't have.", e);
                break;
            }
            if (extractedTicket != null) {
                TombolaWinner winner = getTombolaWinner(consolationPrize, extractedTicket);
                consolationWinners.add(winner);
            }
            remainingExtractionsAux--;
        }
    }

    private void extractHighPriorityTickets(List<TombolaWinner> consolationWinners,
            List<TombolaTicket> highPriorityTickets, RandomSequenceGenerator randomSeqGenForHighPriority,
            Prize consolationPrize, int numberOfHighPriorityExtractions) {
        IntStream.range(0, numberOfHighPriorityExtractions).forEach(i -> {
            TombolaTicket extractedTicket;
            extractedTicket = highPriorityTickets.get(randomSeqGenForHighPriority.nextIntWithAutoReset());
            TombolaWinner winner = getTombolaWinner(consolationPrize, extractedTicket);
            consolationWinners.add(winner);
        });
    }

    /*
    Returns a list of tickets that have priority in winning the consolation prize, one ticket per separate user.
     */
    private List<TombolaTicket> getHighPriorityTickets(String tenantId, String tombolaId, int purchasedTicketsNumber,
            List<TombolaWinner> existingWinners) {
        List<TombolaTicket> highPriorityTickets = new ArrayList<>();
        Set<String> highPriorityUserIds = new HashSet<>();
        Set<String> existingWinningUserIds = existingWinners.stream().map(w -> w.getUser().getUserId())
                .collect(Collectors.toCollection(HashSet::new));
        IntStream.range(0, purchasedTicketsNumber).forEach(i -> {
            TombolaTicket tombolaTicket = tombolaAerospikeDao.getTombolaTicket(tombolaId, String.valueOf(i));
            if (isQualifiedForHighPriority(tombolaTicket, existingWinningUserIds, highPriorityUserIds, tenantId)) {
                tombolaTicket.setId(i);
                highPriorityTickets.add(tombolaTicket);
                highPriorityUserIds.add(tombolaTicket.getUserId());
            }
        });
        return highPriorityTickets;
    }

    private boolean isQualifiedForHighPriority(TombolaTicket tombolaTicket, Set<String> existingWinningUserIds,
            Set<String> alreadySelectedUserIds, String tenantId) {
        boolean result = false;
        if (tombolaTicket != null && tombolaTicket.getUserId() != null) {
            String userId = tombolaTicket.getUserId();
            if (!existingWinningUserIds.contains(userId) && !alreadySelectedUserIds.contains(userId)) {
                result = isHighPurchasePlayer(userId, tenantId);
            }
        }
        return result;
    }

    private boolean isHighPurchasePlayer(String userId, String tenantId) {
        UserStatistics userStatistics = tombolaAerospikeDao.getUserStatisticsByTenantId(userId, tenantId);
        return userStatistics != null && userStatistics.getTicketsBoughtSinceLastWin() > HIGH_PURCHASE_RATE;
    }

    private TombolaWinner extractWinnerForPrize(String tombolaId, Prize prize,
            RandomSequenceGenerator randomSequenceGenerator, int poolSize) throws OutOfUniqueRandomNumbersException {
        TombolaTicket tombolaTicket = readRandomTicket(tombolaId, randomSequenceGenerator, poolSize);
        if (tombolaTicket == null) {
            throw new F4MEntryNotFoundException("There are no valid tickets in the pool! We must stop the tombola!");
        }

        return getTombolaWinner(prize, tombolaTicket);
    }

    private TombolaWinner getTombolaWinner(Prize prize, TombolaTicket tombolaTicket) {
    	ApiProfileBasicInfo userInfo = commonProfileDao.getProfileBasicInfo(tombolaTicket.getUserId());
        return new TombolaWinner(prize, tombolaTicket, userInfo);
    }

    private TombolaTicket getRandomTicketFromPool(String tombolaId, RandomSequenceGenerator randomSeqGenForTickets,
            int poolSize, List<Integer> ticketIdsToExclude) throws OutOfUniqueRandomNumbersException {
        TombolaTicket ticket = null;

        if(poolSize > 0){
            boolean isExcluded;
            do {// exclude tickets from exclusion list once each
                // (if we exhaust the pool then we don't exclude them anymore)
                ticket = readRandomTicket(tombolaId, randomSeqGenForTickets, poolSize);
                if (ticket == null) {
                    throw new F4MEntryNotFoundException(
                            "There are no valid tickets in the pool! We must stop the tombola!");
                }
                isExcluded = ticketIdsToExclude.contains(ticket.getId());
            } while (isExcluded && randomSeqGenForTickets.hasNumbersLeft());
            if (isExcluded) {
                // We ran out of tickets to extract and the current one is already excluded
                ticket = null;
            }
        }
        return ticket;
    }

    private TombolaTicket readRandomTicket(String tombolaId, RandomSequenceGenerator randomSequenceGenerator,
            int maxRepeatsRetryCount) throws OutOfUniqueRandomNumbersException {
        int retryCount = 0;

        TombolaTicket ticket;
        do{//select ticket
            final int ticketId = randomSequenceGenerator.nextInt();
            ticket = tombolaAerospikeDao.getTombolaTicket(tombolaId, Integer.toString(ticketId));
            if(ticket == null || ticket.getUserId() == null){
                LOGGER.warn("Failed to obtain ticket by code [{}] ", ticketId);
            } else {
                ticket.setId(ticketId);
            }
            retryCount++;
        }while((ticket == null || ticket.getUserId() == null) && retryCount < maxRepeatsRetryCount &&
                randomSequenceGenerator.hasNumbersLeft());

        return ticket;
    }
}
