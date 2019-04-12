package de.ascendro.f4m.service.friend.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class FriendManagerMessageSchemaMapper extends DefaultMessageSchemaMapper {
	private static final long serialVersionUID = -4575812985735172610L;

	private static final String FRIEND_SCHEMA_PATH = "friend.json";
	private static final String PAYMENT_SCHEMA_PATH = "payment.json";
	private static final String USER_MESSAGE_SCHEMA_PATH = "userMessage.json";

	@Override
	protected void init() {
		this.register(FriendManagerMessageSchemaMapper.class, "friend", FRIEND_SCHEMA_PATH);
		this.register(FriendManagerMessageSchemaMapper.class, "payment", PAYMENT_SCHEMA_PATH);
		this.register(FriendManagerMessageSchemaMapper.class, "userMessage", USER_MESSAGE_SCHEMA_PATH);

		super.init();
	}
}
