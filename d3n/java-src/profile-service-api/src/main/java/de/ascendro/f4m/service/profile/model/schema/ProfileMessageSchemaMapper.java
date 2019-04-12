package de.ascendro.f4m.service.profile.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class ProfileMessageSchemaMapper extends DefaultMessageSchemaMapper {
	private static final long serialVersionUID = -4575812985735172610L;

	private static final String PROFILE_SCHEMA_PATH = "profile.json";
	//TODO: more correct approach would be to do registration in service (not API) and use references to all attributes from API.
	private static final String FRIEND_MANAGER_SCHEMA_PATH = "friend.json";
	private static final String PAYMENT_MANAGER_SCHEMA_PATH = "payment.json";
	
	@Override
	protected void init() {
		this.register(ProfileMessageSchemaMapper.class, "profile", PROFILE_SCHEMA_PATH);
		this.register(ProfileMessageSchemaMapper.class, "friend", FRIEND_MANAGER_SCHEMA_PATH);
		this.register(ProfileMessageSchemaMapper.class, "payment", PAYMENT_MANAGER_SCHEMA_PATH);
		super.init();
	}
}
