package de.ascendro.f4m.service.analytics.module.jobs;


import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.PromoCodeEvent;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.model.UserRegistrationEvent;
import de.ascendro.f4m.service.analytics.activemq.impl.ActiveMqBrokerManager;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.BaseModule;
import de.ascendro.f4m.service.analytics.module.achievement.AchievementProcessor;
import de.ascendro.f4m.service.analytics.module.jobs.util.JobsUtil;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.config.Config;

public class JobProcessor extends BaseModule implements AnalyticMessageListener {
    private Gson gson;
    private JobsUtil jobsUtil;
    private final Config config;
    private AchievementProcessor achievementProcessor;

    @InjectLogger
    private static Logger LOGGER;

    @Inject
    private JobProcessor(JobsUtil jobsUtil, AchievementProcessor achievementProcessor, Config config, NotificationCommon notificationUtil) {
        super(notificationUtil);
        this.jobsUtil = jobsUtil;
        this.config = config;
        this.achievementProcessor = achievementProcessor;
        gson = new GsonBuilder().create();
    }

    @Override
    public void onModuleMessage(Message message) {
        final TextMessage textMessage = (TextMessage) message;
        try {
            EventContent content = gson.fromJson(textMessage.getText(), EventContent.class);
            if (content.isOfType(MultiplayerGameEndEvent.class)) {
                handleMultiplayerGameEndEvent(content);
            } else if (content.isOfType(PlayerGameEndEvent.class)) {
                jobsUtil.updateProfileGamesCount(content, (PlayerGameEndEvent) content.getEventData());
            } else if (content.isOfType(RewardEvent.class)) {
                jobsUtil.updateProfileWinnings(content, (RewardEvent) content.getEventData());
            } else if (content.isOfType(PromoCodeEvent.class)) {
                jobsUtil.updateProfileWinnings(content, (PromoCodeEvent) content.getEventData());
            } else if (content.isOfType(AdEvent.class)) {
                jobsUtil.updateProfileWinnings(content, (AdEvent) content.getEventData());
            } else if (content.isOfType(InviteEvent.class)) {
               handleInviteEvent(content);
            } else if (content.isOfType(UserRegistrationEvent.class)) {

                jobsUtil.payoutBonusAndCreditOnRegistration(content.getUserId(), content.getAppId(), content.getTenantId(), content.getEventData());
            }
            achievementProcessor.handleEvent(content);

            LOGGER.debug("Message received: {} ", textMessage.getText());
        } catch (Exception e) {
            LOGGER.error("Error on message type", e);
        }
    }

    private void handleMultiplayerGameEndEvent(EventContent content) {
        MultiplayerGameEndEvent event = content.getEventData();
        if (Boolean.TRUE.equals(event.isGameWon()) || Boolean.TRUE.equals(event.isGameLost())) {
            jobsUtil.updateProfileWithGameOutcome(content, event);
        }
    }

    private void handleInviteEvent(EventContent content) {
        if (config.getPropertyAsLong(AnalyticsConfig.MONTHLY_BONUS_NUMBER_OF_FRIENDS) > 0
                && config.getPropertyAsLong(AnalyticsConfig.MONTHLY_BONUS_VALUE) > 0) {
            InviteEvent inviteEvent = content.getEventData();
            if (inviteEvent.isBonusInvite()) {
                jobsUtil.processMonthlyInviteReward(content, inviteEvent);
            }
        }
    }

    @Override
    public String getTopic() {
        return ActiveMqBrokerManager.JOB_TOPIC;
    }

    @Override
    public void initAdvisorySupport() throws JMSException {
        //No advisory
    }
}
