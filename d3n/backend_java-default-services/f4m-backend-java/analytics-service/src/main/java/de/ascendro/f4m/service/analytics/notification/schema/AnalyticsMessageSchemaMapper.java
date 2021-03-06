package de.ascendro.f4m.service.analytics.notification.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class AnalyticsMessageSchemaMapper extends DefaultMessageSchemaMapper {
    private static final long serialVersionUID = -4575812985735172610L;

    private static final String USER_MESSAGE_SCHEMA_PATH = "userMessage.json";
    private static final String PAYMENT_SCHEMA_PATH = "payment.json";

    @Override
    protected void init() {
        this.register(AnalyticsMessageSchemaMapper.class, "userMessage", USER_MESSAGE_SCHEMA_PATH);
        this.register(AnalyticsMessageSchemaMapper.class, "payment", PAYMENT_SCHEMA_PATH);
        super.init();
    }
}
