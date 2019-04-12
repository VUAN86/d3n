package de.ascendro.f4m.service.analytics.module.statistic.query;

import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.GameEndEvent;
import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.model.PaymentEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.statistic.model.Application;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.BaseUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.Renderer;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.config.Config;

public class ApplicationUpdater extends BaseUpdater<Application> implements ITableUpdater<Application> {
	
    @InjectLogger
    private static Logger LOGGER;

    @Inject
    public ApplicationUpdater(Config config, NotificationCommon notificationUtil) {
        super(config, notificationUtil);
    }

    @Override
    public void processEvent(EventContent content) throws Exception {
        if (content.isOfType(AdEvent.class)) {
            process(content, new Renderer<Application, AdEvent>() {
                @Override
                public void render(Application table, AdEvent event) {
                    table.setId(Long.valueOf(content.getAppId()));
                    table.setAdsViewed(1);
                }
            });
        } else if (content.isOfType(InviteEvent.class)) {
            process(content, new Renderer<Application, InviteEvent>() {
                @Override
                public void render(Application table, InviteEvent event) {
                    table.setId(Long.valueOf(content.getAppId()));
                    table.setFriendsInvited(event.getFriendsInvited());
                }
            });
        } else if (content.isOfType(GameEndEvent.class)) {
            process(content, new Renderer<Application, GameEndEvent>() {
                @Override
                public void render(Application table, GameEndEvent event) {
                    table.setId(Long.valueOf(content.getAppId()));
                    table.setGamePlayed(1);
                }
            });
        } else if (content.isOfType(PlayerGameEndEvent.class)) {
            process(content, new Renderer<Application, PlayerGameEndEvent>() {
                @Override
                public void render(Application table, PlayerGameEndEvent event) {
                    table.setId(Long.valueOf(content.getAppId()));
                    table.setPlayers(incrementIfTrue(event.isNewAppPlayer()));
                }
            });
        } else if (content.isOfType(PaymentEvent.class)) {
            process(content, new Renderer<Application, PaymentEvent>() {
                @Override
                public void render(Application table, PaymentEvent event) {
                    table.setId(Long.valueOf(content.getAppId()));
                    table.setMoneyCharged(checkBigDecimal(event.getMoneyCharged()));
                    table.setCreditsPurchased(checkLong(event.getCreditPurchased()));
                }
            });
        } else if (content.isOfType(RewardEvent.class)) {
            process(content, new Renderer<Application, RewardEvent>() {
                @Override
                public void render(Application table, RewardEvent event) {
                    table.setId(Long.valueOf(content.getAppId()));
                    table.setMoneyWon(checkBigDecimal(event.getMoneyWon()));
                    table.setBonusPointsWon(checkLong(event.getBonusPointsWon()));
                    table.setCreditsWon(checkLong(event.getCreditWon()));
                    table.setVoucherWon(incrementIfTrue(event.isVoucherWon()));
                }
            });
        }
    }

    @Override
    protected Class<Application> getTableClass() {
        return Application.class;
    }

    @Override
    protected <T> void preProcess(EventContent content, T event) throws SQLException {
        // No pre processing needed
    }
}
