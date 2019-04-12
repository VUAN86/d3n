package de.ascendro.f4m.service.analytics.module.jobs.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZonedDateTime;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.PromoCodeEvent;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.model.UserRegistrationEvent;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.server.profile.model.AppConfigApplicationConfiguration;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.analytics.client.BonusPayoutRequestInfo;
import de.ascendro.f4m.service.analytics.client.MonthlyInvitesPayoutRequestInfo;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentDetailsBuilder;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransferFundsRequest;
import de.ascendro.f4m.service.payment.model.TransferFundsRequestBuilder;
import de.ascendro.f4m.service.profile.model.ProfileStats;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class JobsUtil {
    @InjectLogger
    private static Logger LOGGER;

    private final CommonProfileAerospikeDao profileDao;
    private final Config config;
    private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
    private final ServiceRegistryClient serviceRegistryClient;
    private final JsonMessageUtil jsonMessageUtil;
    private final TransactionLogAerospikeDao transactionLogAerospikeDao;
    private final ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;

    private static final String PRIZE_PAYOUT_REASON = "Prize for monthly invites";
    private static final String REGISTRATION_BONUS = "Prize for registration";
    private static final String FULL_REGISTRATION_BONUS = "Prize for full registration";

    @Inject
    private JobsUtil(CommonProfileAerospikeDao profileDao, Config config,
                     JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool,
                     ServiceRegistryClient serviceRegistryClient,
                     JsonMessageUtil jsonMessageUtil,
                     TransactionLogAerospikeDao transactionLogAerospikeDao,
                     ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao) {
        this.profileDao = profileDao;
        this.config = config;
        this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
        this.serviceRegistryClient = serviceRegistryClient;
        this.jsonMessageUtil = jsonMessageUtil;
        this.transactionLogAerospikeDao = transactionLogAerospikeDao;
        this.applicationConfigurationAerospikeDao = applicationConfigurationAerospikeDao;
    }

    public void updateProfileGamesCount(EventContent content, PlayerGameEndEvent event) {
        checkUserAndTenant(content);

        final ProfileStats profileStats = new ProfileStats();

        profileStats.setTotalGames(1);

        if (Boolean.TRUE.equals(event.isGameTypeTournament())) {
            profileStats.setTotalTournamentGamesCount(1);
        } else if (Boolean.TRUE.equals(event.isGameTypeDuel())) {
            profileStats.setTotalDuelGamesCount(1);
        } else if (Boolean.TRUE.equals(event.isGameTypeQuizz())) {
            profileStats.setTotalQuickQuizGamesCount(1);
        }

        updateProfile(content.getUserId(), content.getTenantId(), profileStats);
    }

    public void updateProfileWithGameOutcome(EventContent content, MultiplayerGameEndEvent event) {
        checkUserAndTenant(content);

        final ProfileStats profileStats = new ProfileStats();

        if (Boolean.TRUE.equals(event.isGameWon())) {
            profileStats.setGamesWon(1);
            if (Boolean.TRUE.equals(event.isDuelGameWon())) {
                profileStats.setWonDuelGamesCount(1);
            }
            if (Boolean.TRUE.equals(event.isTournamentGameWon())) {
                profileStats.setWonTournamentGamesCount(1);
            }

            updateProfile(content.getUserId(), content.getTenantId(), profileStats);
        } else if (Boolean.TRUE.equals(event.isGameLost())) {
            profileStats.setGamesLost(1);
            if (Boolean.TRUE.equals(event.isDuelGameLost())) {
                profileStats.setLostDuelGamesCount(1);
            }
            if (Boolean.TRUE.equals(event.isTournamentGameLost())) {
                profileStats.setLostTournamentGamesCount(1);
            }

            updateProfile(content.getUserId(), content.getTenantId(), profileStats);
        }
    }

    public void updateProfileWinnings(EventContent content, RewardEvent event) {
        checkUserAndTenant(content);

        final ProfileStats profileStats = new ProfileStats();

        if (event.getCreditWon()>0) {
            profileStats.setCreditWon(event.getCreditWon().intValue());
        }
        if (event.getBonusPointsWon()>0) {
            profileStats.setPointsWon(event.getBonusPointsWon().intValue());
        }
        if (event.getMoneyWon() != null && event.getMoneyWon().signum()>0) {
            profileStats.setMoneyWon(event.getMoneyWon().setScale(2, RoundingMode.DOWN).doubleValue());
        }

        updateProfile(content.getUserId(), content.getTenantId(), profileStats);
    }

    public void updateProfileWinnings(EventContent content, PromoCodeEvent event) {
        checkUserAndTenant(content);

        final ProfileStats profileStats = new ProfileStats();

        if (event.getCreditsPaid()>0) {
            profileStats.setCreditWon(event.getCreditsPaid().intValue());
        }
        if (event.getBonusPointsPaid()>0) {
            profileStats.setPointsWon(event.getBonusPointsPaid().intValue());
        }
        if (event.getMoneyPaid() != null && event.getMoneyPaid().signum()>0) {
            profileStats.setMoneyWon(event.getMoneyPaid().setScale(2, RoundingMode.DOWN).doubleValue());
        }

        updateProfile(content.getUserId(), content.getTenantId(), profileStats);
    }

    public void updateProfileWinnings(EventContent content, AdEvent event) {
        checkUserAndTenant(content);

        final ProfileStats profileStats = new ProfileStats();

        if (event.getEarnCredits()>0) {
            profileStats.setCreditWon(event.getEarnCredits().intValue());
        }

        updateProfile(content.getUserId(), content.getTenantId(), profileStats);
    }

    public void payoutBonusAndCreditOnRegistration(String userId, String applicationId, String tenantId, UserRegistrationEvent event) throws SQLException {
    	LOGGER.debug("Processing registration bonus payout event userId: {}, applicationId: {}, tenantId: {}, registered: {}, registeredFully: {}", userId, applicationId, tenantId, event.isRegistered(), event.isFullyRegistered());
        AppConfig appConfig = applicationConfigurationAerospikeDao.getAppConfiguration(tenantId, applicationId);

        AppConfigApplicationConfiguration appConfigApplicationConfiguration = appConfig.getApplication().getConfiguration();

        if (event.isFullyRegistered()) {
            payoutBonusAndCreditOnRegistration(appConfigApplicationConfiguration.getFullRegistrationBonusPoints(),
                    appConfigApplicationConfiguration.getFullRegistrationCredits(), userId, tenantId, applicationId, true);
        } else if (event.isRegistered()) {
        	payoutBonusAndCreditOnRegistration(appConfigApplicationConfiguration.getSimpleRegistrationBonusPoints(),
                    appConfigApplicationConfiguration.getSimpleRegistrationCredits(), userId, tenantId, applicationId, false);
        }
    }

	private void payoutBonusAndCreditOnRegistration(BigDecimal bonusPoints, BigDecimal credits, String userId, String tenantId, String appId, boolean fullRegistration) throws SQLException {
    	LOGGER.debug("Paying out bonusPoints: {}, credits: {}", bonusPoints, credits);
        if (bonusPoints!=null && bonusPoints.compareTo(BigDecimal.ZERO)>0) {
        	payoutBonusAndCreditOnRegistration(bonusPoints, Currency.BONUS, userId, tenantId, appId, fullRegistration);
        }
        if (credits!=null && credits.compareTo(BigDecimal.ZERO) > 0) {
        	payoutBonusAndCreditOnRegistration(credits, Currency.CREDIT, userId, tenantId, appId, fullRegistration);
        }
    }

	private void payoutBonusAndCreditOnRegistration(BigDecimal amount, Currency currency, String userId, String tenantId, String appId, boolean fullRegistration) throws SQLException {
		final String reason = fullRegistration ? FULL_REGISTRATION_BONUS : REGISTRATION_BONUS;
        final TransferFundsRequestBuilder builder = new TransferFundsRequestBuilder()
                .fromTenantToProfile(tenantId, userId)
                .amount(amount, currency)
                .withPaymentDetails(new PaymentDetailsBuilder()
                        .additionalInfo(reason)
                        .appId(appId)
                        .build());
        TransferFundsRequest transactionRequest = builder.buildSingleUserPaymentForGame();
        final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonMessageUtil.createNewMessage(
                builder.getBuildSingleUserPaymentForGameRequestType(), transactionRequest);
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setAppId(appId);
        clientInfo.setTenantId(tenantId);
        clientInfo.setUserId(userId);
        transactionRequestMessage.setClientInfo(clientInfo);
        
        final String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(transactionRequest, currency, reason, appId));
        final BonusPayoutRequestInfo requestInfo =  new BonusPayoutRequestInfo();
        requestInfo.setAppId(appId);
        requestInfo.setTenantId(tenantId);
        requestInfo.setUserId(userId);
        requestInfo.setCurrency(currency);
        requestInfo.setAmount(amount);
        requestInfo.setTransactionLogId(transactionLogId);
        requestInfo.setFullRegistration(fullRegistration);

        try {
        	LOGGER.debug("Sending message: " + transactionRequestMessage);
            jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(
                    serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME),
                    transactionRequestMessage, requestInfo);
        } catch (F4MValidationFailedException | F4MIOException e) {
            throw new F4MFatalErrorException("Unable to send transferFunds request to payment service", e);
        }
	}
	
    private void checkUserAndTenant(EventContent content) {
        if (StringUtils.isBlank(content.getUserId())) {
            throw new F4MAnalyticsFatalErrorException("Invalid user in event content");
        }

        if (StringUtils.isBlank(content.getTenantId())) {
            throw new F4MAnalyticsFatalErrorException("Invalid tenant in event content");
        }
    }

    public synchronized void processMonthlyInviteReward(EventContent content, InviteEvent inviteEvent) {
        checkUserAndTenant(content);

        final ProfileStats profileStats = new ProfileStats();
        profileStats.setCurrentMonth(ZonedDateTime.ofInstant(Instant.ofEpochMilli(content.getEventTimestamp()), DateTimeUtil.TIMEZONE));
        profileStats.setMonthlyInvites(inviteEvent.getFriendsInvited());
        profileDao.updateStats(content.getUserId(), content.getTenantId(), profileStats);

        ProfileStats currentProfileStats = profileDao.getStatsFromBlob(content.getUserId(), content.getTenantId());
        int targetInvites = config.getPropertyAsInteger(AnalyticsConfig.MONTHLY_BONUS_NUMBER_OF_FRIENDS);

        if (currentProfileStats.getMonthlyInvites()>=targetInvites
                && currentProfileStats.getMonthlyInvites() - inviteEvent.getFriendsInvited()<targetInvites) {
            MonthlyInvitesPayoutRequestInfo monthlyInvitesPayoutRequestInfo =  new MonthlyInvitesPayoutRequestInfo();
            monthlyInvitesPayoutRequestInfo.setAppId(content.getAppId());
            monthlyInvitesPayoutRequestInfo.setTenantId(content.getTenantId());
            monthlyInvitesPayoutRequestInfo.setUserId(content.getUserId());
            monthlyInvitesPayoutRequestInfo.setCurrency(config.getProperty(AnalyticsConfig.MONTHLY_BONUS_CURRENCY));
            monthlyInvitesPayoutRequestInfo.setAmount(new BigDecimal(config.getProperty(AnalyticsConfig.MONTHLY_BONUS_VALUE)));
            monthlyInvitesPayoutRequestInfo.setTargetInvites(config.getPropertyAsInteger(AnalyticsConfig.MONTHLY_BONUS_NUMBER_OF_FRIENDS));

            initiateMonthlyInvitesPayout(monthlyInvitesPayoutRequestInfo);
        }
    }

    public void initiateMonthlyInvitesPayout(MonthlyInvitesPayoutRequestInfo monthlyInvitesPayoutRequestInfo) {
        final TransferFundsRequestBuilder builder = new TransferFundsRequestBuilder()
                .fromTenantToProfile(monthlyInvitesPayoutRequestInfo.getTenantId(), monthlyInvitesPayoutRequestInfo.getUserId())
                .amount(monthlyInvitesPayoutRequestInfo.getAmount(), monthlyInvitesPayoutRequestInfo.getCurrency())
                .withPaymentDetails(new PaymentDetailsBuilder()
                        .appId(monthlyInvitesPayoutRequestInfo.getAppId())
                        .additionalInfo(PRIZE_PAYOUT_REASON)
                        .build());

        TransferFundsRequest transactionRequest = builder.buildSingleUserPaymentForGame();
        final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonMessageUtil.createNewMessage(
                builder.getBuildSingleUserPaymentForGameRequestType(), transactionRequest);
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setAppId(monthlyInvitesPayoutRequestInfo.getAppId());
        clientInfo.setTenantId(monthlyInvitesPayoutRequestInfo.getTenantId());
        clientInfo.setUserId(monthlyInvitesPayoutRequestInfo.getUserId());
        transactionRequestMessage.setClientInfo(clientInfo);

        String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(transactionRequest,
                monthlyInvitesPayoutRequestInfo.getCurrency(), PRIZE_PAYOUT_REASON, monthlyInvitesPayoutRequestInfo.getAppId()));
        monthlyInvitesPayoutRequestInfo.setTransactionLogId(transactionLogId);

        try {
            jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(
                    serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME),
                    transactionRequestMessage, monthlyInvitesPayoutRequestInfo);
        } catch (F4MValidationFailedException | F4MIOException e) {
            throw new F4MFatalErrorException("Unable to send transferFunds request to payment service", e);
        }
    }

    private void updateProfile(String userId, String tenantId, ProfileStats profileStats) {
        profileDao.updateStats(userId, tenantId, profileStats);
    }
}
