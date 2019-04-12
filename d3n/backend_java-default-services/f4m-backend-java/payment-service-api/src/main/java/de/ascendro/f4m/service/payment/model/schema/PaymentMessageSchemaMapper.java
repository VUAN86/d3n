package de.ascendro.f4m.service.payment.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class PaymentMessageSchemaMapper extends DefaultMessageSchemaMapper {
    private static final long serialVersionUID = -4575812985735172611L;

    private static final String PAYMENT_SCHEMA_PATH = "payment.json";
    private static final String USER_MESSAGE_SCHEMA_PATH = "userMessage.json";
    private static final String PROFILE_SCHEMA_PATH = "profile.json";

    @Override
    protected void init() {
        this.register(PaymentMessageSchemaMapper.class, "payment", PAYMENT_SCHEMA_PATH);
        this.register(PaymentMessageSchemaMapper.class, "userMessage", USER_MESSAGE_SCHEMA_PATH);
        this.register(PaymentMessageSchemaMapper.class, "profile", PROFILE_SCHEMA_PATH);
        super.init();
    }
}
