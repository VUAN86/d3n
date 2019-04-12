package de.ascendro.f4m.service.ping;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class PingPongMessageSchemaMapper extends DefaultMessageSchemaMapper {
	private static final long serialVersionUID = -4575812985735172610L;

	private static final String TEST_SCHEMA_PATH = "de/ascendro/f4m/service/ping/model/schema/ping-schema.json";
	
	@Override
	protected void init() {
		this.register(PingPongMessageSchemaMapper.class, "test", TEST_SCHEMA_PATH);
		super.init();
	}
}
