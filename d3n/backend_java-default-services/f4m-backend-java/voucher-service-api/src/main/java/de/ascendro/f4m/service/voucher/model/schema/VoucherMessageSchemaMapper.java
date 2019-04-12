package de.ascendro.f4m.service.voucher.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class VoucherMessageSchemaMapper extends DefaultMessageSchemaMapper {
	private static final long serialVersionUID = -4575812985735172610L;

	private static final String VOUCHER_SCHEMA_PATH = "voucher.json";
	private static final String PROFILE_SCHEMA_PATH = "profile.json";
	private static final String USER_MESSAGE_SCHEMA_PATH = "userMessage.json";
	private static final String PAYMENT_SCHEMA_PATH = "payment.json";

	@Override
	protected void init() {
		this.register(VoucherMessageSchemaMapper.class, "voucher", VOUCHER_SCHEMA_PATH);
		this.register(VoucherMessageSchemaMapper.class, "profile", PROFILE_SCHEMA_PATH);
		this.register(VoucherMessageSchemaMapper.class, "userMessage", USER_MESSAGE_SCHEMA_PATH);
		this.register(VoucherMessageSchemaMapper.class, "payment", PAYMENT_SCHEMA_PATH);
		super.init();
	}
}
