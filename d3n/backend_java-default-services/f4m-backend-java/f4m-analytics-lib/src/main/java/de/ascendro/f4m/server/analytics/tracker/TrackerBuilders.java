package de.ascendro.f4m.server.analytics.tracker;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.EventRecord;
import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;
import de.ascendro.f4m.server.analytics.model.base.BaseEvent;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class TrackerBuilders {

    private TrackerBuilders() {
    }

    @FunctionalInterface
    private interface BaseBuilder<T> {
        T build();
    }

    public static class EventBuilder extends AnonymousEventBuilder implements TrackerBuilders.BaseBuilder<EventRecord> {
        @Override
        public EventBuilder setAnalyticsContent(ContentBuilder contentBuilder) {
            return (EventBuilder)super.setAnalyticsContent(contentBuilder);
        }

        @Override
        public EventRecord build() {
            EventRecord eventRecord = super.build();
            checkContent(eventRecord.getAnalyticsContent());

            return  eventRecord;
        }

        private void checkContent(EventContent content) {
        	BaseEvent event = content.getEventData();
            //Test if user id is present
            if (event.isUserIdRequired() && content.getUserId() == null) {
                throw new F4MAnalyticsFatalErrorException("Error on event build. User ID cannot be empty.");
            }
            //Test if ip is present
            if (event.isSessionIpRequired() && StringUtils.isEmpty(content.getSessionIp())) {
                throw new F4MAnalyticsFatalErrorException("Error on event build. IP cannot be empty.");
            }
            //Test if application id is present
            if (event.isAppIdRequired() && content.getAppId() == null) {
                throw new F4MAnalyticsFatalErrorException("Error on event build. Application ID cannot be empty.");
            }
            //Test if tenant id is present
            if (event.isTenantIdRequired() && content.getTenantId() == null) {
                throw new F4MAnalyticsFatalErrorException("Error on event build. Tenant ID cannot be empty.");
            }
        }
    }

    public static class AnonymousEventBuilder implements TrackerBuilders.BaseBuilder<EventRecord> {
        final EventRecord record = new EventRecord();
        ContentBuilder contentBuilder;

        public AnonymousEventBuilder setAnalyticsContent(ContentBuilder contentBuilder) {
            this.contentBuilder = contentBuilder;
            return this;
        }

        public ContentBuilder getAnalyticsContent() {
            return this.contentBuilder;
        }

        @Override
        public EventRecord build() {
            if (this.contentBuilder == null) {
                throw new F4MAnalyticsFatalErrorException("Error on event build. Analytics content cannot be empty.");
            }
            //set event timestamp
            record.setTimestamp(DateTimeUtil.getUTCTimestamp());
            record.setAnalyticsContent(this.contentBuilder.setTimestamp(record.getTimestamp()).build());

            return record;
        }
    }

    public static class ContentBuilder implements TrackerBuilders.BaseBuilder<EventContent> {
        final EventContent content = new EventContent();

        public ContentBuilder setCountryCode(String countryCode) {
            content.setCountryCode(countryCode);
            return this;
        }

        public ContentBuilder setCountryCode(ISOCountry isoCountryCode) {
            if (isoCountryCode!=null) {
                content.setCountryCode(isoCountryCode.toString());
            }
            return this;
        }

        public ContentBuilder setUserId(String userId) {
            content.setUserId(userId);
            return this;
        }

        public ContentBuilder setApplicationId(String applicationId) {
            content.setAppId(applicationId);
            return this;
        }

        public ContentBuilder setTenantId(String tenantId) {
            content.setTenantId(tenantId);
            return this;
        }

        public ContentBuilder setSessionIP(String sessionIP) {
            content.setSessionIp(sessionIP);
            return this;
        }

        public ContentBuilder setTimestamp(long timestamp) {
            content.setEventTimestamp(timestamp);
            return this;
        }

        public ContentBuilder setEventData(BaseEvent eventData) {
            content.setEventData(eventData);
            if (content.getEventType() == null && eventData!=null)
                content.setEventType(eventData.getClass().getCanonicalName());
            return this;
        }

        @Override
		public EventContent build() {
            //Test if an event is present in content
            if (StringUtils.isEmpty(content.getEventType())) {
                throw new F4MAnalyticsFatalErrorException("Error on event content build. Event type cannot be empty.");
            }
            return content;
        }

    }

}