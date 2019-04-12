package de.ascendro.f4m.server.game.instance;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.winning.model.WinningComponentType;

import java.util.List;

public interface CommonGameInstanceAerospikeDao extends AerospikeDao {
	char KEY_ITEM_SEPARATOR = ':';
	String BLOB_BIN_NAME = "value";
	String GAME_KEY_PREFIX = "game";
	String INSTANCE_KEY_PREFIX = "gameInstance";
	String ADS_BIN_NAME = "advertisements";

	GameInstance getGameInstance(String gameInstanceId);
	
	Game getGameByInstanceId(String gameInstanceId);

	List<String> getGameInstancesList(String gameid);

	List getAdvertisementShown(String gameInstanceId);

	void addUserWinningComponentIdToGameInstance(String gameInstanceId, String userWinningComponentId, WinningComponentType winningComponentType);

	void createRecordInTheDatabaseOfPaymentOfAdditionalWinnings(String mgiId, String userId);

	boolean isRecordInTheDatabaseOfPaymentOfAdditionalWinnings(String mgiId, String userId);
}
