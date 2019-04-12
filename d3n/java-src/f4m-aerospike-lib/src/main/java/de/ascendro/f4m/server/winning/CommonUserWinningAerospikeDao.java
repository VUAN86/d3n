package de.ascendro.f4m.server.winning;

import com.google.gson.JsonObject;
import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.winning.model.UserWinning;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.WinningComponent;

import java.util.List;

public interface CommonUserWinningAerospikeDao extends AerospikeOperateDao {
	public static final String BLOB_BIN_NAME = "value";
	public static final String WINNINGS_BIN_NAME = "winnings";

	void saveUserWinning(String appId, String userId, UserWinning userWinning);

	UserWinning getUserWinning(String appId, String userWinningId);

	/**
	 * Returns list of user winnings
	 */
	List<JsonObject> getUserWinnings(String appId, String userId, int limit, long offset, List<OrderBy> orderBy);

	/**
	 * Move user winnings from source user to target user.
	 * @param sourceUserId Source user ID
	 * @param targetUserId Target user ID
	 */
	void moveWinnings(String appId, String sourceUserId, String targetUserId);

	UserWinningComponent getWinningComponentResult(String componentId);

	WinningComponent getWinningComponent(String id);
}
