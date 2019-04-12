package de.ascendro.f4m.service.analytics.module.sparc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.util.Assert;

import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.EventRecord;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.tracker.TrackerBuilders;

public class TestSparkProcessor {

    @Test
    public void testSparkJsonStream() throws Exception {
        PlayerGameEndEvent gameEndEvent = new PlayerGameEndEvent();
        gameEndEvent.setGameId(1L);
        gameEndEvent.setPlayedWithFriend(true);
        gameEndEvent.setTotalQuestions(10);
        gameEndEvent.setAverageAnswerSpeed(50L);
        gameEndEvent.setPlayerHandicap(77D);
        EventRecord gameEventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("2")
                        .setApplicationId("1")
                        .setSessionIP("0.0.0.0")
                        .setEventData(gameEndEvent))
                .build();

        String response = new SparkProcessor(null, null, null, null).prepareSparkStreamJson(gameEventRecord.getAnalyticsContent());

        Assert.isTrue(hasNullPropery(response, PlayerGameEndEvent.TOTAL_CORRECT_QUESTIONS_PROPERTY));
        Assert.isTrue(hasNullPropery(response, PlayerGameEndEvent.TOTAL_INCORRECT_QUESTIONS_PROPERTY));
        Assert.isTrue(hasNullPropery(response, PlayerGameEndEvent.PLAYED_WITH_PUBLIC_PROPERTY));
        Assert.isTrue(hasNullPropery(response, PlayerGameEndEvent.PAID_COMPONENT_PROPERTY));

        EventContent content = new Gson().fromJson(response, EventContent.class);
        JsonObject jsonObject = content.getEventData().getJsonObject();
        assertEquals(jsonObject.get(PlayerGameEndEvent.TOTAL_CORRECT_QUESTIONS_PROPERTY), JsonNull.INSTANCE);
        assertEquals(jsonObject.get(PlayerGameEndEvent.TOTAL_INCORRECT_QUESTIONS_PROPERTY), JsonNull.INSTANCE);
        assertEquals(jsonObject.get(PlayerGameEndEvent.PLAYED_WITH_PUBLIC_PROPERTY), JsonNull.INSTANCE);
        assertEquals(jsonObject.get(PlayerGameEndEvent.PAID_COMPONENT_PROPERTY), JsonNull.INSTANCE);
    }

    private boolean hasNullPropery(String source, String propertyName) {
        return source.replace("\\s+","").contains("\"" + propertyName + "\":null");
    }
}
