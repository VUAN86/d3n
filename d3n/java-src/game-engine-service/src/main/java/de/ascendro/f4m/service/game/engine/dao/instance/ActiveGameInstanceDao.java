package de.ascendro.f4m.service.game.engine.dao.instance;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

import com.aerospike.client.query.PredExp;

import de.ascendro.f4m.service.game.engine.model.ActiveGameInstance;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;

public interface ActiveGameInstanceDao {
	
	void create(GameInstance gameInstance, ZonedDateTime invitationExpireDateTime);
	
	void updateStatus(String gameInstanceId, GameStatus gameStatus, GameEndStatus gameEndStatus);
	
	void delete(String gameInstanceId);
	
	void processActiveRecords(PredExp[] predExp, Consumer<ActiveGameInstance> entryCallback);

    default ActiveGameInstance getActiveGameInstance(String gameInstanceId) {
        return null;
    }

	boolean exists(String gameInstanceId);
}
