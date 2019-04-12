package de.ascendro.f4m.service.analytics.module.statistic.query;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.google.inject.Inject;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.GameEndEvent;
import de.ascendro.f4m.server.analytics.model.PaymentEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.model.base.BaseEvent;
import de.ascendro.f4m.server.analytics.model.base.GameBaseEvent;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.statistic.model.Game;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.BaseUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.Renderer;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.analytics.util.AnalyticServiceUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.model.GameInstance;

public class GameUpdater extends BaseUpdater<Game> implements ITableUpdater<Game> {
	
    @InjectLogger
    private static Logger LOGGER;
    protected final AnalyticServiceUtil analyticServiceUtil;

    @Inject
    public GameUpdater(Config config, AnalyticServiceUtil analyticServiceUtil, NotificationCommon notificationUtil) {
        super(config, notificationUtil);
        this.analyticServiceUtil = analyticServiceUtil;
    }

    @Override
    public void processEvent(EventContent content) throws Exception {
        if (content.isOfType(AdEvent.class)) {
            process(content, new Renderer<Game, AdEvent>() {
                @Override
                public void render(Game table, AdEvent event) {
                    table.setId(event.getGameId());
                    table.setAdsViewed(1);
                }
            });
        } else if (content.isOfType(PaymentEvent.class)) {
            process(content, new Renderer<Game, PaymentEvent>() {
                @Override
                public void render( Game table, PaymentEvent event) {
                    table.setId(event.getGameId());
                    table.setCreditsPayed(checkLong(event.getCreditPaid()));
                    table.setMoneyPayed(checkBigDecimal(event.getMoneyPaid()));
                    table.setBonusPointsPaid(checkLong(event.getBonusPointsPaid()));
                }
            });
        } else if (content.isOfType(GameEndEvent.class)) {
            process(content, new Renderer<Game, GameEndEvent>() {
                @Override
                public void render(Game table, GameEndEvent event) {
                    table.setId(event.getGameId());
                    table.setGamesPlayed(1);
                }
            });
        } else if (content.isOfType(PlayerGameEndEvent.class)) {
            process(content, new Renderer<Game, PlayerGameEndEvent>() {
                @Override
                public void render(Game table, PlayerGameEndEvent event) {
                    table.setId(event.getGameId());
                    table.setPlayers(incrementIfTrue(event.isNewGamePlayer()));
                    table.setQuestionsAnswered(checkInt(event.getTotalQuestions()));
                    table.setQuestionsAnsweredRight(checkInt(event.getTotalCorrectQuestions()));
                    table.setQuestionsAnsweredWrong(checkInt(event.getTotalIncorrectQuestions()));
                    table.setAverageAnswerSpeed(checkLong(event.getAverageAnswerSpeed()));
                }
            });
        } else if (content.isOfType(RewardEvent.class)) {
            process(content, new Renderer<Game, RewardEvent>() {
                @Override
                public void render(Game table, RewardEvent event) {
                    table.setId(event.getGameId());
                    table.setMoneyWon(checkBigDecimal(event.getMoneyWon()));
                    table.setBonusPointsWon(checkLong(event.getBonusPointsWon()));
                    table.setCreditsWon(checkLong(event.getCreditWon()));
                    table.setVoucherWon(incrementIfTrue(event.isVoucherWon()));
                }
            });
        }
    }

    @Override
    protected <E extends BaseEvent> void process(EventContent content, Renderer<Game, E> renderer) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, SQLException {
        if ((new GameBaseEvent(content.getEventData().getJsonObject())).getGameId()!=null)
            super.process(content, renderer);
    }

    @Override
    protected <T> void preProcess(EventContent content, T event) throws SQLException {
        if (event instanceof RewardEvent) {
            RewardEvent rewardEvent = (RewardEvent) event;
            if (StringUtils.isNotBlank(rewardEvent.getGameInstanceId())) {
                GameInstance gameInstance = analyticServiceUtil.getGameInstance(rewardEvent.getGameInstanceId());
                rewardEvent.setGameId(gameInstance.getGame().getGameId());
            }
        }
    }

    @Override
    protected Class<Game> getTableClass() {
        return Game.class;
    }
}
