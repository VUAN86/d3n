package de.ascendro.f4m.service.winning.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class WinningMessageSchemaMapper extends DefaultMessageSchemaMapper {
	private static final long serialVersionUID = -4575812985735172610L;

	private static final String WINNING_SCHEMA_PATH = "winning.json";
	private static final String RESULT_SCHEMA_PATH = "resultEngine.json";
	private static final String PAYMENT_SCHEMA_PATH = "payment.json";
	private static final String VOUCHER_SCHEMA_PATH = "voucher.json";
	private static final String USER_MESSAGE_SCHEMA_PATH = "userMessage.json";

	@Override
	protected void init() {
		this.register(WinningMessageSchemaMapper.class, "winning", WINNING_SCHEMA_PATH);
		this.register(WinningMessageSchemaMapper.class, "resultEngine", RESULT_SCHEMA_PATH);
		this.register(WinningMessageSchemaMapper.class, "payment", PAYMENT_SCHEMA_PATH);
		this.register(WinningMessageSchemaMapper.class, "voucher", VOUCHER_SCHEMA_PATH);
		this.register(WinningMessageSchemaMapper.class, "userMessage", USER_MESSAGE_SCHEMA_PATH);
		super.init();
	}
}
