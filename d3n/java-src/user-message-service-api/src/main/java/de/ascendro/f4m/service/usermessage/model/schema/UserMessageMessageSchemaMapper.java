package de.ascendro.f4m.service.usermessage.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class UserMessageMessageSchemaMapper extends DefaultMessageSchemaMapper {
	private static final long serialVersionUID = -4575812985735172610L;

	private static final String USER_MESSAGE_SCHEMA_PATH = "userMessage.json";
	private static final String PROFILE_SCHEMA_PATH = "profile.json";
	
	@Override
	protected void init() {
		this.register(UserMessageMessageSchemaMapper.class, "userMessage", USER_MESSAGE_SCHEMA_PATH);
		this.register(UserMessageMessageSchemaMapper.class, "profile", PROFILE_SCHEMA_PATH);
		super.init();
	}
}
