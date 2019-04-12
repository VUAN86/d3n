package de.ascendro.f4m.server.analytics.tracker;

import static org.testng.Assert.assertEquals;

import org.junit.Test;

import de.ascendro.f4m.server.analytics.EventRecord;
import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.TombolaEndAnnouncementEvent;

public class TrackerBuildersTest {


    @Test(expected = F4MAnalyticsFatalErrorException.class)
    public void testEmptyBuilder() {
        new TrackerBuilders.EventBuilder()
                .build();
    }

    @Test
    public void testCorrectBuilder() {

        AdEvent adEvent = new AdEvent();
        adEvent.setGameId(101L);
        adEvent.setEarnCredits(5L);
        adEvent.setAdId(500L);

        TrackerBuilders.EventBuilder builder = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("u1")
                        .setApplicationId("100")
                        .setSessionIP("0.0.0.0"));

        assertEquals(builder.getAnalyticsContent().setEventData(adEvent).build().getUserId(), "u1");


        EventRecord eventRecord = new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("1")
                        .setApplicationId("100")
                        .setSessionIP("0.0.0.0")
                        .setEventData(adEvent))
                .build();
        assertEquals(((AdEvent) eventRecord.getAnalyticsContent().getEventData()).getAdId().longValue(), 500L);
    }

    @Test
    public void testAnonymousBuilder() {

        TombolaEndAnnouncementEvent tombolaEndAnnouncementEvent = new TombolaEndAnnouncementEvent();
        tombolaEndAnnouncementEvent.setTombolaId(1L);
        tombolaEndAnnouncementEvent.setMinutesToEnd(10);

        EventRecord eventRecord = new TrackerBuilders.AnonymousEventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setEventData(tombolaEndAnnouncementEvent)).build();

        int minutesToEnd = ((TombolaEndAnnouncementEvent) eventRecord.getAnalyticsContent().getEventData()).getMinutesToEnd().intValue();

        assertEquals(minutesToEnd, 10);
    }


    @Test(expected = F4MAnalyticsFatalErrorException.class)
    public void testBuilderForUserPresence() {

        AdEvent adEvent = new AdEvent();
        adEvent.setGameId(101L);
        adEvent.setEarnCredits(5L);
        adEvent.setAdId(500L);

        new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId(null)
                        .setApplicationId("100")
                        .setSessionIP("0.0.0.0")
                        .setEventData(adEvent))
                .build();
    }

    @Test(expected = F4MAnalyticsFatalErrorException.class)
    public void testBuilderForAppPresence() {

        AdEvent adEvent = new AdEvent();
        adEvent.setGameId(101L);
        adEvent.setEarnCredits(5L);
        adEvent.setAdId(500L);

        new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("u1")
                        .setApplicationId(null)
                        .setSessionIP("0.0.0.0")
                        .setEventData(adEvent))
                .build();
    }

    @Test(expected = F4MAnalyticsFatalErrorException.class)
    public void testBuilderForIPPresence() {

        AdEvent adEvent = new AdEvent();
        adEvent.setGameId(101L);
        adEvent.setEarnCredits(5L);
        adEvent.setAdId(500L);

        new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("u1")
                        .setApplicationId("100")
                        .setSessionIP(null)
                        .setEventData(adEvent))
                .build();
    }

    @Test(expected = F4MAnalyticsFatalErrorException.class)
    public void testBuilderForEventPresence() {

        AdEvent adEvent = new AdEvent();
        adEvent.setGameId(101L);
        adEvent.setEarnCredits(5L);
        adEvent.setAdId(500L);

        new TrackerBuilders.EventBuilder()
                .setAnalyticsContent(new TrackerBuilders.ContentBuilder()
                        .setUserId("u1")
                        .setApplicationId("100")
                        .setSessionIP("0.0.0.0"))
                .build();
    }

}