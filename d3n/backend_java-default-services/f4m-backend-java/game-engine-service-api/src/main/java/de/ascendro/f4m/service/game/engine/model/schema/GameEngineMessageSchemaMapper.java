package de.ascendro.f4m.service.game.engine.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class GameEngineMessageSchemaMapper extends DefaultMessageSchemaMapper {
	private static final long serialVersionUID = -4575812985735172610L;

	private static final String GAME_ENGINE_SCHEMA_PATH = "gameEngine.json";
	private static final String RESULT_ENGINE_SCHEMA_PATH = "resultEngine.json";
	private static final String PROFILE_SCHEMA_PATH = "profile.json";
	private static final String PAYMENT_SCHEMA_PATH = "payment.json";
	private static final String WINNING_SCHEMA_PATH = "winning.json";
	private static final String GAME_SELECTION_SCHEMA_PATH = "gameSelection.json";
	private static final String VOUCHER_SCHEMA_PATH = "voucher.json";

	@Override
	protected void init() {
		this.register(GameEngineMessageSchemaMapper.class, "gameEngine", GAME_ENGINE_SCHEMA_PATH);
		this.register(GameEngineMessageSchemaMapper.class, "resultEngine", RESULT_ENGINE_SCHEMA_PATH);
		this.register(GameEngineMessageSchemaMapper.class, "profile", PROFILE_SCHEMA_PATH);
		this.register(GameEngineMessageSchemaMapper.class, "payment", PAYMENT_SCHEMA_PATH);
		this.register(GameEngineMessageSchemaMapper.class, "winning", WINNING_SCHEMA_PATH);
		this.register(GameEngineMessageSchemaMapper.class, "gameSelection", GAME_SELECTION_SCHEMA_PATH);
		this.register(GameEngineMessageSchemaMapper.class, "voucher", VOUCHER_SCHEMA_PATH);
		super.init();
	}
}
