package de.ascendro.f4m.server.analytics;


public class EventRecord {


    private long timestamp;
    private EventContent analyticsContent;

    public EventContent getAnalyticsContent() {
        return analyticsContent;
    }

    public void setAnalyticsContent(EventContent analyticsContent) {
        this.analyticsContent = analyticsContent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
