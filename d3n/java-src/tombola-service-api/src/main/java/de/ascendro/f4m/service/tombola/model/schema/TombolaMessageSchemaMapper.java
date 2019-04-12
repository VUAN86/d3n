package de.ascendro.f4m.service.tombola.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class TombolaMessageSchemaMapper extends DefaultMessageSchemaMapper {
    private static final long serialVersionUID = -4575812985735172611L;

    private static final String TOMBOLA_SCHEMA_PATH = "tombola.json";
    private static final String PAYMENT_SCHEMA_PATH = "payment.json";
    private static final String VOUCHER_SCHEMA_PATH = "voucher.json";
    private static final String PROFILE_SCHEMA_PATH = "profile.json";
    private static final String USER_MESSAGE_SCHEMA_PATH = "userMessage.json";


    @Override
    protected void init() {
        this.register(TombolaMessageSchemaMapper.class, "tombola", TOMBOLA_SCHEMA_PATH);
        this.register(TombolaMessageSchemaMapper.class, "profile", PROFILE_SCHEMA_PATH);
        this.register(TombolaMessageSchemaMapper.class, "payment", PAYMENT_SCHEMA_PATH);
        this.register(TombolaMessageSchemaMapper.class, "voucher", VOUCHER_SCHEMA_PATH);
        this.register(TombolaMessageSchemaMapper.class, "userMessage", USER_MESSAGE_SCHEMA_PATH);
        super.init();
    }
}
