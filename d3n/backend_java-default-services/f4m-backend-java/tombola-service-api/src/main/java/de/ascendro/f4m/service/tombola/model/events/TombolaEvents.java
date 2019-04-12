package de.ascendro.f4m.service.tombola.model.events;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class TombolaEvents extends JsonObjectWrapper {

    // Event topics
    private static final String TOPIC_WILDCARD_ANY = "*";
	public static final String TOMBOLA_DRAW_EVENT_TOPIC_PREFIX = "tombola/draw/";
	public static final String TOMBOLA_OPEN_CHECKOUT_EVENT_TOPIC_PREFIX = "tombola/openCheckout/";
	public static final String TOMBOLA_CLOSE_CHECKOUT_EVENT_TOPIC_PREFIX = "tombola/closeCheckout/";
	public static final String TOMBOLA_PRE_CLOSE_CHECKOUT_EVENT_TOPIC_PREFIX = "tombola/preCloseCheckout/";

    private static final String PROPERTY_SOURCE_TOMBOLA_ID = "tombolaId";
    private static final String PROPERTY_TENANT_ID = "tenantId";
    private static final String PROPERTY_MINUTES_TO_CHECKOUT = "minutesToCheckout";

	public TombolaEvents(JsonObject object) {
		super(object);
	}

    public TombolaEvents() {
        // Empty constructor
    }

	public String getTombolaId() {
        return getPropertyAsString(PROPERTY_SOURCE_TOMBOLA_ID);
	}

	public void setTombolaId(String tombolaId) {
		setProperty(PROPERTY_SOURCE_TOMBOLA_ID, tombolaId);
	}

    public String getTenantId() {
        return getPropertyAsString(PROPERTY_TENANT_ID);
    }

    public void setTenantId(String tenantId) {
        setProperty(PROPERTY_TENANT_ID, tenantId);
    }

    public int getMinutesToCheckout() {
        return getPropertyAsInt(PROPERTY_MINUTES_TO_CHECKOUT);
    }

    public void setMinutesToCheckout(int minutesToCheckout) {
        setProperty(PROPERTY_MINUTES_TO_CHECKOUT, minutesToCheckout);
    }

    public static String getDrawEventTopic() {
        return TOMBOLA_DRAW_EVENT_TOPIC_PREFIX + TOPIC_WILDCARD_ANY;
    }

    public static String getDrawEventTopic(String tombolaId) {
        return TOMBOLA_DRAW_EVENT_TOPIC_PREFIX + tombolaId;
    }

    public static String getOpenCheckoutTopic() {
        return TOMBOLA_OPEN_CHECKOUT_EVENT_TOPIC_PREFIX + TOPIC_WILDCARD_ANY;
    }

    public static String getOpenCheckoutTopic(String tombolaId) {
        return TOMBOLA_OPEN_CHECKOUT_EVENT_TOPIC_PREFIX + tombolaId;
    }

    public static String getCloseCheckoutTopic() {
        return TOMBOLA_CLOSE_CHECKOUT_EVENT_TOPIC_PREFIX + TOPIC_WILDCARD_ANY;
    }

    public static String getCloseCheckoutTopic(String tombolaId) {
        return TOMBOLA_CLOSE_CHECKOUT_EVENT_TOPIC_PREFIX + tombolaId;
    }

    public static String getPreCloseCheckoutTopic() {
        return TOMBOLA_PRE_CLOSE_CHECKOUT_EVENT_TOPIC_PREFIX + TOPIC_WILDCARD_ANY;
    }

    public static String getPreCloseCheckoutTopic(String tombolaId) {
        return TOMBOLA_PRE_CLOSE_CHECKOUT_EVENT_TOPIC_PREFIX + tombolaId;
    }

    public static boolean isDrawEventTopic(String topic) {
        return StringUtils.startsWith(topic, TOMBOLA_DRAW_EVENT_TOPIC_PREFIX);
    }

    public static boolean isOpenCheckoutTopic(String topic) {
        return StringUtils.startsWith(topic, TOMBOLA_OPEN_CHECKOUT_EVENT_TOPIC_PREFIX);
    }

    public static boolean isCloseCheckoutTopic(String topic) {
        return StringUtils.startsWith(topic, TOMBOLA_CLOSE_CHECKOUT_EVENT_TOPIC_PREFIX);
    }

    public static boolean isPreCloseCheckoutTopic(String topic) {
        return StringUtils.startsWith(topic, TOMBOLA_PRE_CLOSE_CHECKOUT_EVENT_TOPIC_PREFIX);
    }
}
