package de.ascendro.f4m.service.analytics.data;

import java.math.BigDecimal;
import java.util.stream.LongStream;

import com.google.gson.Gson;

import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.EventRecord;
import de.ascendro.f4m.server.analytics.EventsDestinationMapper;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.GameEndEvent;
import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.model.InvoiceEvent;
import de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.PaymentEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.PromoCodeEvent;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.model.TombolaEndEvent;
import de.ascendro.f4m.server.analytics.model.UserRegistrationEvent;
import de.ascendro.f4m.server.analytics.model.VoucherCountEvent;
import de.ascendro.f4m.server.analytics.model.VoucherUsedEvent;
import de.ascendro.f4m.server.analytics.tracker.TrackerBuilders;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;
import de.ascendro.f4m.service.profile.model.Profile;

public class EventTestData {

    public static final String USER_ID = "1";
    public static final int PLAYER_GAMES = 5;

    public static final String APPLICATION_ID = "1";
    public static final String TENANT_ID = "tenantId";
    public static final String SESSION_IP = "0.0.0.0";


    public static final long PROMO_CODE_BONUS = 100L;
    public static final long REWARD_BONUS = 12L;
    public static final long USER_REGISTRATION_BONUS = 15L;
    public static final long USER_FULL_REGISTRATION_BONUS = 22L;
    public static final long TOTAL_BONUS_POINTS_EARNED = PROMO_CODE_BONUS
            + REWARD_BONUS + USER_REGISTRATION_BONUS + USER_FULL_REGISTRATION_BONUS;

    public static final long USER_REGISTRATION_CREDITS = 11L;
    public static final long USER_FULL_REGISTRATION_CREDITS = 14L;
    public static final long PROMO_CODE_CREDIT = 50L;
    public static final long AD_CREDIT_1 = 5L;
    public static final long AD_CREDIT_2 = 1L;
    public static final long TOTAL_CREDITS_EARNED = PROMO_CODE_CREDIT + AD_CREDIT_1 + AD_CREDIT_2
            + USER_REGISTRATION_CREDITS + USER_FULL_REGISTRATION_CREDITS;

    public static final double TOTAL_MONEY_EARNED = 40.8;
    private EventCounters createdEntriesCounter;
    private AnalyticsDaoImpl analyticsDaoImpl;

    public EventTestData(AnalyticsDaoImpl analyticsDaoImpl) {
        this.analyticsDaoImpl = analyticsDaoImpl;
        createdEntriesCounter = new EventCounters();
    }

    private void saveEventRecord(EventRecord testEventRecord) {
        analyticsDaoImpl.createAnalyticsEvent(testEventRecord);
        if (EventsDestinationMapper.isStatisticEvent(testEventRecord.getAnalyticsContent().getEventType())) {
            createdEntriesCounter.increaseStatisticEntriesCounter();
        } else if (EventsDestinationMapper.isNotificationEvent(testEventRecord.getAnalyticsContent().getEventType())) {
            createdEntriesCounter.increaseNotificationEntriesCounter();
        } else if (EventsDestinationMapper.isSparkEvent(testEventRecord.getAnalyticsContent().getEventType())) {
            createdEntriesCounter.increaseSparkEntriesCounter();
        } else if (EventsDestinationMapper.isLiveMapEvent(testEventRecord.getAnalyticsContent().getEventType())) {
            createdEntriesCounter.increaseLiveMapEntriesCounter();
        }
    }

    public EventCounters getEventCounter() {
        return createdEntriesCounter;
    }

    public void createInitialStatisticsData() {
        createAdEventData();
        createGameEndEventData();
        createInviteEventData();
        createPaymentEventData();
        createRewardEventData();
        createPromoCodeEventData();
        createVoucherUsedEventData();
        createVoucherCountEventData();
        createUserRegistrationEventData();
    }

    public void createInitialData() {
        createInitialStatisticsData();
    }

    public void createVoucherUsedEventData() {
        VoucherUsedEvent voucherUsedEvent = new VoucherUsedEvent();
        voucherUsedEvent.setVoucherId(1L);
        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setSessionIP(SESSION_IP)
                        .setTenantId(TENANT_ID)
                        .setEventData(voucherUsedEvent))
                .build();
        saveEventRecord(testEventRecord);
    }

    public void createVoucherCountEventData() {
        VoucherCountEvent voucherCountEvent = new VoucherCountEvent();
        voucherCountEvent.setVoucherId(1L);
        voucherCountEvent.setVoucherCount(100L);
        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setSessionIP(SESSION_IP)
                        .setTenantId(TENANT_ID)
                        .setEventData(voucherCountEvent))
                .build();
        saveEventRecord(testEventRecord);
    }


    public void createUserRegistrationEventData() {
        UserRegistrationEvent userRegistrationEvent = new UserRegistrationEvent();
        userRegistrationEvent.setRegistered(true);
        userRegistrationEvent.setFullyRegistered(false);
        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setSessionIP(SESSION_IP)
                        .setTenantId(TENANT_ID)
                        .setEventData(userRegistrationEvent))
                .build();
        saveEventRecord(testEventRecord);

        userRegistrationEvent.setRegistered(false);
        userRegistrationEvent.setFullyRegistered(true);
        saveEventRecord(testEventRecord);

    }

    public void createInvoiceEvents() {
        InvoiceEvent invoiceEvent = new InvoiceEvent();
        invoiceEvent.setPaymentType(InvoiceEvent.PaymentType.ENTRY_FEE);
        invoiceEvent.setPaymentAmount(new BigDecimal(6));
        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setSessionIP(SESSION_IP)
                        .setCountryCode(ISOCountry.DE)
                        .setTenantId(TENANT_ID)
                        .setEventData(invoiceEvent))
                .build();
        saveEventRecord(testEventRecord);
    }

    public void createPromoCodeEventData() {
        PromoCodeEvent promoCodeEvent = new PromoCodeEvent();
        promoCodeEvent.setPromoCode("ACGG23S");
        promoCodeEvent.setPromoCodeCampaignId(1L);
        promoCodeEvent.setBonusPointsPaid(PROMO_CODE_BONUS);
        promoCodeEvent.setCreditsPaid(PROMO_CODE_CREDIT);
        promoCodeEvent.setMoneyPaid(new BigDecimal(10));
        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setSessionIP(SESSION_IP)
                        .setTenantId(TENANT_ID)
                        .setEventData(promoCodeEvent))
                .build();
        saveEventRecord(testEventRecord);
    }

    public void createTombolaEvents() {
        TombolaEndEvent tombolaEndEvent = new TombolaEndEvent();
        tombolaEndEvent.setTombolaId(Long.valueOf(TombolaTestData.DEFAULT_TEST_TOMBOLA_ID));
        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setSessionIP(SESSION_IP)
                        .setEventData(tombolaEndEvent))
                .build();
        saveEventRecord(testEventRecord);
    }

    public EventRecord createMultiplayerEventRecord(Profile profile, String gameInstanceId) {
        MultiplayerGameEndEvent multiplayerGameEndEvent = new MultiplayerGameEndEvent();
        multiplayerGameEndEvent.setGameId(1L);
        multiplayerGameEndEvent.setGameInstanceId(gameInstanceId);
        multiplayerGameEndEvent.setOpponentId(profile.getUserId());
        multiplayerGameEndEvent.setGameWon(true);
        multiplayerGameEndEvent.setPlacement(1L);
        return new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setTenantId(TENANT_ID)
                        .setSessionIP(SESSION_IP)
                        .setEventData(multiplayerGameEndEvent))
                .build();
    }

    public void createMultiplayerEvent(Profile profile, String gameId) {
        //Register event 1
        EventRecord testEventRecord = createMultiplayerEventRecord(profile, gameId);
        saveEventRecord(testEventRecord);
    }

    private void createAdEventData() {
        //Register event 1
        AdEvent adEvent = new AdEvent();
        adEvent.setGameId(1L);
        adEvent.setBlobKey("provider_5_advertisement_1.json");
        adEvent.setEarnCredits(AD_CREDIT_1);
        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setTenantId(TENANT_ID)
                        .setSessionIP(SESSION_IP)
                        .setEventData(adEvent))
                .build();
        saveEventRecord(testEventRecord);

        //Register event 2
        adEvent = new AdEvent();
        adEvent.setGameId(1L);
        adEvent.setBlobKey("provider_8_advertisement_1.json");
        adEvent.setEarnCredits(AD_CREDIT_2);
        testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setTenantId(TENANT_ID)
                        .setSessionIP(SESSION_IP)
                        .setEventData(adEvent))
                .build();
        saveEventRecord(testEventRecord);
    }


    private void createGameEndEventData() {
        LongStream.rangeClosed(1, PLAYER_GAMES)
                .forEach(i -> {
                    PlayerGameEndEvent playerGameEndEvent = new PlayerGameEndEvent();
                    playerGameEndEvent.setGameId(i);
                    playerGameEndEvent.setPlayedWithFriend(true);
                    playerGameEndEvent.setTotalQuestions(10);
                    playerGameEndEvent.setAverageAnswerSpeed(50L);
                    playerGameEndEvent.setPlayerHandicap(77D);
                    EventRecord gameEventRecord = new TrackerBuilders.EventBuilder()
                            .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                                    .setUserId(USER_ID)
                                    .setApplicationId(APPLICATION_ID)
                                    .setTenantId(TENANT_ID)
                                    .setSessionIP(SESSION_IP)
                                    .setEventData(playerGameEndEvent))
                            .build();
                    saveEventRecord(gameEventRecord);

                    GameEndEvent gameEndEvent = new GameEndEvent();
                    gameEndEvent.setGameId(i);
                    gameEventRecord = new TrackerBuilders.EventBuilder()
                            .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                                    .setUserId(USER_ID)
                                    .setApplicationId(APPLICATION_ID)
                                    .setSessionIP(SESSION_IP)
                                    .setEventData(gameEndEvent))
                            .build();
                    saveEventRecord(gameEventRecord);
                });
    }

    private void createInviteEventData() {
        InviteEvent inviteEvent = new InviteEvent();
        inviteEvent.setGameId(1L);
        inviteEvent.setFriendsInvited(10);
        inviteEvent.setBonusInvite(true);
        inviteEvent.setInvitedFromFriends(true);
        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setTenantId(TENANT_ID)
                        .setSessionIP(SESSION_IP)
                        .setEventData(inviteEvent))
                .build();
        saveEventRecord(testEventRecord);
    }


    private void createPaymentEventData() {
        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setGameId(1L);
        paymentEvent.setCreditPaid(12L);
        paymentEvent.setMoneyPaid(new BigDecimal(4.5));
        paymentEvent.setBonusPointsPaid(4L);
        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setTenantId(TENANT_ID)
                        .setSessionIP(SESSION_IP)
                        .setEventData(paymentEvent))
                .build();
        saveEventRecord(testEventRecord);


        PaymentDetails paymentDetails = new PaymentDetails();
        paymentDetails.setGameInstanceId("14891468456074e383fdf-1027-4a4a-a894-590f2b86e1de");
        paymentDetails.setAdditionalInfo("Jackpot winning");
        paymentDetails.setGameId("1");
        paymentDetails.setAppId(APPLICATION_ID);

        Gson g = new Gson();

        paymentEvent = new PaymentEvent();
        paymentEvent.setGameId(1L);
        paymentEvent.setPaymentAmount(new BigDecimal(12));
        paymentEvent.setPaymentDetailsJSON(g.toJson(paymentDetails));

        testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setTenantId(TENANT_ID)
                        .setSessionIP(SESSION_IP)
                        .setEventData(paymentEvent))
                .build();
        saveEventRecord(testEventRecord);
    }

    private void createRewardEventData() {
        RewardEvent rewardEvent = new RewardEvent();
        rewardEvent.setGameId(1L);
        rewardEvent.setBonusPointsWon(REWARD_BONUS);
        rewardEvent.setMoneyWon(new BigDecimal(30.8));
        rewardEvent.setSuperPrizeWon(true);
        EventRecord testEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(USER_ID)
                        .setApplicationId(APPLICATION_ID)
                        .setTenantId(TENANT_ID)
                        .setSessionIP(SESSION_IP)
                        .setEventData(rewardEvent))
                .build();
        saveEventRecord(testEventRecord);
    }

    public class EventCounters {
        private int statisticEntriesCounter = 0;
        private int notificationEntriesCounter = 0;
        private int sparkEntriesCounter = 0;
        private int liveMapEntriesCounter = 0;

        public int getStatisticEntriesCounter() {
            return this.statisticEntriesCounter;
        }

        private void increaseStatisticEntriesCounter() {
            statisticEntriesCounter++;
        }

        public int getNotificationEntriesCounter() {
            return this.notificationEntriesCounter;
        }

        private void increaseNotificationEntriesCounter() {
            notificationEntriesCounter++;
        }

        public int getSparkEntriesCounter() {
            return this.sparkEntriesCounter;
        }

        private void increaseSparkEntriesCounter() {
            sparkEntriesCounter++;
        }

        public int getLiveMapEntriesCounter() {
            return this.liveMapEntriesCounter;
        }

        private void increaseLiveMapEntriesCounter() {
            liveMapEntriesCounter++;
        }
    }
}
