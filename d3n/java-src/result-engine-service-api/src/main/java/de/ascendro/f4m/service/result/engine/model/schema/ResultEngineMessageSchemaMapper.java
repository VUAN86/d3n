package de.ascendro.f4m.service.result.engine.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;
import de.ascendro.f4m.service.payment.model.schema.PaymentMessageSchemaMapper;
import de.ascendro.f4m.service.profile.model.schema.ProfileMessageSchemaMapper;
import de.ascendro.f4m.service.usermessage.model.schema.UserMessageMessageSchemaMapper;
import de.ascendro.f4m.service.voucher.model.schema.VoucherMessageSchemaMapper;

public class ResultEngineMessageSchemaMapper extends DefaultMessageSchemaMapper {

	private static final long serialVersionUID = -4575812985735172611L;

	private static final String RESULT_ENGINE_SCHEMA_PATH = "resultEngine.json";
	private static final String PROFILE_SCHEMA_PATH = "profile.json";
	private static final String USER_MESSAGE_SCHEMA_PATH = "userMessage.json";
	private static final String VOUCHER_SCHEMA_PATH = "voucher.json";
	private static final String PAYMENT_SCHEMA_PATH = "payment.json";

	@Override
	protected void init() {
		register(ResultEngineMessageSchemaMapper.class, "resultEngine", RESULT_ENGINE_SCHEMA_PATH);
		register(ProfileMessageSchemaMapper.class, "profile", PROFILE_SCHEMA_PATH);
		register(UserMessageMessageSchemaMapper.class, "userMessage", USER_MESSAGE_SCHEMA_PATH);
		register(VoucherMessageSchemaMapper.class, "voucher", VOUCHER_SCHEMA_PATH);
		register(PaymentMessageSchemaMapper.class, "payment", PAYMENT_SCHEMA_PATH);
		super.init();
	}

}
