package de.ascendro.f4m.service.game.engine.multiplayer;

import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.GAME_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class TestCustomGameConfigBuilder extends CustomGameConfigBuilder {

	protected TestCustomGameConfigBuilder(String creatorId) {
		super(creatorId);
		withTenant(TENANT_ID);
		forGame(GAME_ID);
	}	
	
	public static CustomGameConfigBuilder create(String creatorId){
		return new TestCustomGameConfigBuilder(creatorId);
	} 
	
	public static CustomGameConfigBuilder createDuel(String creatorId){
		return new TestCustomGameConfigBuilder(creatorId)
				.withMaxNumberOfParticipants(2)
				.withGameType(GameType.DUEL)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusMinutes(2))//
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusMinutes(2));
	} 
}
