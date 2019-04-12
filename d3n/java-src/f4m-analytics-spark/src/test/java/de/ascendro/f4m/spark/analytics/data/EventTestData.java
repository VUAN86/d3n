package de.ascendro.f4m.spark.analytics.data;

import java.math.BigDecimal;
import java.util.Random;
import java.util.stream.IntStream;

import de.ascendro.f4m.server.analytics.EventRecord;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.Question;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.tracker.TrackerBuilders;

public class EventTestData {

    public static EventRecord createRewardEvent() {
        RewardEvent rewardEvent = new RewardEvent();
        rewardEvent.setGameId(1L);
        rewardEvent.setBonusPointsWon(12L);
        rewardEvent.setMoneyWon(new BigDecimal(30.8));
        rewardEvent.setSuperPrizeWon(true);
        return new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("2")
                        .setApplicationId("1")
                        .setSessionIP("0.0.0.0")
                        .setEventData(rewardEvent))
                .build();
    }

    public static EventRecord createPlayerGameEndEvent() {
        int totalQuestions = 10;
        PlayerGameEndEvent playerGameEndEvent = new PlayerGameEndEvent();
        playerGameEndEvent.setGameId(1L);
        playerGameEndEvent.setPlayedWithFriend(true);
        playerGameEndEvent.setTotalQuestions(totalQuestions);
        playerGameEndEvent.setAverageAnswerSpeed(50L);
        playerGameEndEvent.setPlayerHandicap(77D);
        IntStream.of(totalQuestions).forEach(i -> addQuestion(playerGameEndEvent));
        return new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("2334432433564b15f79ba-73dd-4a4c-b1ed-8b4a87d99bcc")
                        .setApplicationId("1")
                        .setTimestamp(System.currentTimeMillis())
                        .setSessionIP("0.0.0.0")
                        .setEventData(playerGameEndEvent))
                .build();
    }

    private static void addQuestion(PlayerGameEndEvent playerGameEndEvent) {
        Question question = new Question();
        long questionId = playerGameEndEvent.getQuestionsMap().size() + 1;
        question.setQuestionId(questionId);
        question.setAnswerIsCorrect(new Random().nextBoolean());

        playerGameEndEvent.addQuestion(question);
    }
}
