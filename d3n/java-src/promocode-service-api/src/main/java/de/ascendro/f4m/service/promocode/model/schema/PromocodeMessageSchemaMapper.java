package de.ascendro.f4m.service.promocode.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class PromocodeMessageSchemaMapper extends DefaultMessageSchemaMapper {
	private static final long serialVersionUID = -4575812985735172610L;

	private static final String PROMOCODE_SCHEMA_PATH = "promocode.json";
	private static final String PROFILE_SCHEMA_PATH = "profile.json";
	private static final String PAYMENT_SCHEMA_PATH = "payment.json";

	@Override
	protected void init() {
		this.register(PromocodeMessageSchemaMapper.class, "promocode", PROMOCODE_SCHEMA_PATH);
		this.register(PromocodeMessageSchemaMapper.class, "profile", PROFILE_SCHEMA_PATH);
		this.register(PromocodeMessageSchemaMapper.class, "payment", PAYMENT_SCHEMA_PATH);
		super.init();
	}
}
