package de.ascendro.f4m.service.analytics.module.statistic.query;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.Question;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.statistic.model.QuestionTranslation;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.BaseUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.Renderer;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.config.Config;

public class QuestionUpdater extends BaseUpdater<QuestionTranslation> implements ITableUpdater<QuestionTranslation> {
    @InjectLogger
    private static Logger LOGGER;

    @Inject
    public QuestionUpdater(Config config, NotificationCommon notificationUtil) {
        super(config, notificationUtil);
    }

    @Override
    public void processEvent(EventContent content) throws Exception {
        if (content.isOfType(PlayerGameEndEvent.class)) {
            processPlayerGameEndEventQuestions(content, content.getEventData());
        }
    }

    private void processPlayerGameEndEventQuestions(EventContent content, PlayerGameEndEvent gameEndEvent)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, SQLException, InstantiationException {
        if (gameEndEvent.getQuestionsMap() != null) {
            for (int i = 0; i < gameEndEvent.getQuestionsMap().size(); i++) {
                final int index = i;
                process(content, new Renderer<QuestionTranslation, PlayerGameEndEvent>() {
                    @Override
                    public void render(QuestionTranslation table, PlayerGameEndEvent event) {
                        Question question = event.getQuestion(index);
                        table.setId(question.getQuestionId());
                        table.setRightAnswers(incrementIfTrue(question.isAnswerCorrect()));
                        table.setWrongAnswers(incrementIfTrue(!question.isAnswerCorrect()));
                        //must be one per game
                        table.setGamesPlayed(1);
                    }
                });
            }
        }
    }

    @Override
    protected Class<QuestionTranslation> getTableClass() {
        return QuestionTranslation.class;
    }

    @Override
    protected <T> void preProcess(EventContent content, T event) throws SQLException {
        // No pre processing needed
    }

}
