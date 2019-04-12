package de.ascendro.f4m.service.game.selection.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class GameSelectionMessageSchemaMapper extends DefaultMessageSchemaMapper {
	private static final long serialVersionUID = -4575812985735172610L;

	private static final String GAME_SELECTION_SCHEMA_PATH = "gameSelection.json";
	private static final String USER_MESSAGE_SCHEMA_PATH = "userMessage.json";
	private static final String FRIEND_MANAGER_SCHEMA_PATH = "friend.json";
	private static final String PAYMENT_SCHEMA_PATH = "payment.json";
	private static final String PROFILE_SCHEMA_PATH = "profile.json";
	private static final String GAME_ENGINE_SCHEMA_PATH = "gameEngine.json";

	@Override
	protected void init() {
		this.register(GameSelectionMessageSchemaMapper.class, "gameSelection", GAME_SELECTION_SCHEMA_PATH);
		this.register(GameSelectionMessageSchemaMapper.class, "userMessage", USER_MESSAGE_SCHEMA_PATH);
		this.register(GameSelectionMessageSchemaMapper.class, "friend", FRIEND_MANAGER_SCHEMA_PATH);
		this.register(GameSelectionMessageSchemaMapper.class, "payment", PAYMENT_SCHEMA_PATH);
		this.register(GameSelectionMessageSchemaMapper.class, "profile", PROFILE_SCHEMA_PATH);
		this.register(GameSelectionMessageSchemaMapper.class, "gameEngine", GAME_ENGINE_SCHEMA_PATH);
		super.init();
	}
}
