package de.ascendro.f4m.spark.analytics;

import org.apache.spark.sql.streaming.StreamingQuery;

import de.ascendro.f4m.spark.analytics.model.BaseEventMessage;

public class ThriftStream {
    private StreamingQuery streamingQuery;
    private StreamStatus streamStatus;
    private BaseEventMessage event;

    public ThriftStream(BaseEventMessage event) {
        this.event = event;
    }

    public StreamingQuery getStreamingQuery() {
        return streamingQuery;
    }

    public void setStreamingQuery(StreamingQuery streamingQuery) {
        this.streamingQuery = streamingQuery;
    }

    public StreamStatus getStreamStatus() {
        return streamStatus;
    }

    public void setStreamStatus(StreamStatus streamStatus) {
        this.streamStatus = streamStatus;
    }

    public BaseEventMessage getEvent() {
        return event;
    }
}
