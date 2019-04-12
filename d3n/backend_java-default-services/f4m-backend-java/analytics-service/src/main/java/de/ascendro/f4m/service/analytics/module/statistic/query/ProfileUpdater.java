package de.ascendro.f4m.service.analytics.module.statistic.query;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.inject.Inject;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.PaymentEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.statistic.model.Profile;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.BaseUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.Renderer;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;
import de.ascendro.f4m.service.winning.dao.UserWinningComponentAerospikeDao;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.WinningOptionType;


public class ProfileUpdater extends BaseUpdater<Profile> implements ITableUpdater<Profile> {
    @InjectLogger
    private static Logger LOGGER;

    private final Gson gson;
    private final UserWinningComponentAerospikeDao userWinningComponentAerospikeDao;

    @Inject
    public ProfileUpdater(Config config, GsonProvider gsonProvider,
                          UserWinningComponentAerospikeDao userWinningComponentAerospikeDao,
                          NotificationCommon notificationUtil) {
        super(config, notificationUtil);
        this.userWinningComponentAerospikeDao = userWinningComponentAerospikeDao;
        this.gson = gsonProvider.get();
    }

    @Override
    public void processEvent(EventContent content) throws Exception {
        if (content.isOfType(AdEvent.class)) {
            process(content, new Renderer<Profile, AdEvent>() {
                @Override
                public void render(Profile table, AdEvent event) {
                    table.setId(content.getUserId());
                    table.setAdsViewed(1);
                }
            });
        } else if (content.isOfType(RewardEvent.class)) {
            process(content, new Renderer<Profile, RewardEvent>() {
                @Override
                public void render(Profile table, RewardEvent event) {
                    table.setId(content.getUserId());
                    table.setMoneyWon(checkBigDecimal(event.getMoneyWon()));
                    table.setBonusPointsWon(checkLong(event.getBonusPointsWon()));
                    table.setCreditsWon(checkLong(event.getCreditWon()));
                    table.setSuperPrizesWon(incrementIfTrue(event.isSuperPrizeWon()));
                    table.setVoucherWon(incrementIfTrue(event.isVoucherWon()));
                }
            });
        } else if (content.isOfType(MultiplayerGameEndEvent.class)) {
                process(content, new Renderer<Profile, MultiplayerGameEndEvent>() {
                    @Override
                    public void render(Profile table, MultiplayerGameEndEvent event) {
                        table.setId(content.getUserId());
                        table.setGamesWon(incrementIfTrue(event.isGameWon()));
                        table.setGamesLost(incrementIfTrue(event.isGameLost()));
                        table.setGamesDrawn(incrementIfTrue(event.isGameDraw()));
                    }
                });
        } else if (content.isOfType(PlayerGameEndEvent.class)) {
            process(content, new Renderer<Profile, PlayerGameEndEvent>() {
                @Override
                public void render(Profile table, PlayerGameEndEvent event) {
                    table.setId(content.getUserId());
                    table.setPaidWinningComponentsPlayed(incrementIfTrue(event.isPaidComponent()));
                    table.setFreeWinningComponentsPlayed(incrementIfTrue(event.isFreeComponent()));
                    table.setSkippedWinningComponents(incrementIfTrue(event.isPaidComponentSkipped()));
                    table.setRightAnswers(checkInt(event.getTotalCorrectQuestions()));
                    table.setWrongAnswers(checkInt(event.getTotalIncorrectQuestions()));
                    table.setSkippedQuestions(checkInt(event.getSkippedQuestions()));
                    table.setGamesPlayedWithFriends(incrementIfTrue(event.isPlayedWithFriend()));
                    table.setGamesPlayedWithPublic(incrementIfTrue(event.isPlayedWithPublic()));
                    table.setGamesPlayed(1);
                    table.setAverageAnswerSpeed(checkLong(event.getAverageAnswerSpeed()));
                    table.setHandicap(checkDouble(event.getPlayerHandicap()));

                }
            });
        } else if (content.isOfType(PaymentEvent.class)) {
            process(content, new Renderer<Profile, PaymentEvent>() {
                @Override
                public void render(Profile table, PaymentEvent event) {
                    table.setId(content.getUserId());
                    table.setTotalMoneyCharged(checkBigDecimal(event.getMoneyCharged()));
                    table.setTotalCreditsPurchased(checkLong(event.getCreditPurchased()));
                }
            });
        } else if (content.isOfType(InviteEvent.class)) {
            process(content, new Renderer<Profile, InviteEvent>() {
                @Override
                public void render(Profile table, InviteEvent event) {
                    table.setId(content.getUserId());
                    table.setFriendsInvited(event.getFriendsInvited());
                    table.setGamesInvitedFromFriends(incrementIfTrue(event.isInvitedFromFriends()));
                    table.setGamesFriendsInvitedToo(incrementIfTrue(event.isFriendsInvitedToo()));
                    table.setFriendsBlocked(checkLong(event.getFriendsBlocked()));
                    table.setFriendsUnblocked(checkLong(event.getFriendsUnblocked()));
                }
            });
        }
    }

    @Override
    protected Class<Profile> getTableClass() {
        return Profile.class;
    }

    private void fillPaymentFieldsFromDetailsJSON(EventContent content, PaymentEvent paymentEvent) {
        PaymentDetails paymentDetails = gson.fromJson(paymentEvent.getPaymentDetailsJSON(), PaymentDetails.class);
        try {
            UserWinningComponent userWinningComponent = userWinningComponentAerospikeDao.getUserWinningComponent(content.getTenantId(), content.getUserId(), paymentDetails.getUserWinningComponentId());
            if (userWinningComponent != null && userWinningComponent.getWinning() != null) {
                if (userWinningComponent.getWinning().getType() == WinningOptionType.MONEY) {
                    paymentEvent.setMoneyPaid(paymentEvent.getPaymentAmount());
                    paymentEvent.setMoneyCharged(paymentEvent.getPaymentAmount());
                } else if (userWinningComponent.getWinning().getType() == WinningOptionType.BONUS) {
                    paymentEvent.setBonusPointsPaid(paymentEvent.getPaymentAmount().longValue());
                } else if (userWinningComponent.getWinning().getType() == WinningOptionType.CREDITS) {
                    paymentEvent.setCreditPaid(paymentEvent.getPaymentAmount().longValue());
                    paymentEvent.setCreditPurchased(paymentEvent.getPaymentAmount().longValue());
                }
            } else {
                LOGGER.error("Invalid user winning component for payment: {}", paymentDetails.toString());
            }
        } catch (F4MEntryNotFoundException ex) {
            LOGGER.error("Invalid user winning component for payment: {}", paymentDetails.toString());
        }
    }

    @Override
    protected <T> void preProcess(EventContent content, T event) throws SQLException {
        if (event instanceof PaymentEvent) {
            PaymentEvent paymentEvent = (PaymentEvent) event;
            if (StringUtils.isNotBlank(paymentEvent.getPaymentDetailsJSON())) {
                fillPaymentFieldsFromDetailsJSON(content, paymentEvent);
            }
        }
    }
}
