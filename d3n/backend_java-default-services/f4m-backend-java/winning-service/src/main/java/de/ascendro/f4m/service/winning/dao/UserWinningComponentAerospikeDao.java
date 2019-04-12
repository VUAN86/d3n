package de.ascendro.f4m.service.winning.dao;

import java.util.List;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.service.exception.client.F4MEntryAlreadyExistsException;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.WinningOption;

/**
 * Winning Data Access Object interface.
 */
public interface UserWinningComponentAerospikeDao extends AerospikeOperateDao {

	static final String BLOB_BIN_NAME = "value";
	static final String COMPONENTS_BIN_NAME = "components";

	/**
	 * Assign winning component to user
	 * @throws F4MEntryAlreadyExistsException
	 *             if user already has winning component of the same game
	 *             instance
	 */
	void saveUserWinningComponent(String appId, String userId, UserWinningComponent component) throws F4MEntryAlreadyExistsException;

	/**
	 * Get winning component by specified ID
	 */
	UserWinningComponent getUserWinningComponent(String appId, String userId, String componentId);
	
	/**
	 * Return list of user winning components
	 */
	public List<JsonObject> getUserWinningComponents(String appId, String userId, int limit, long offset, List<OrderBy> orderBy);

	/**
	 * Move winning components from source to target user.
	 */
	public void moveUserWinningComponents(String appId, String sourceUserId, String targetUserId);
	
	/**
	 * Mark the user winning component used.
	 */
	void markUserWinningComponentUsed(String appId, String userId, String userWinningComponentId);

	/**
	 * Store the winning option in the user winning component.
	 */
	void saveUserWinningComponentWinningOption(String appId, String userId, String userWinningComponentId, WinningOption winning);

	/**
	 * Mark the user winning component filed.
	 */
	void markUserWinningComponentFiled(String appId, String userId, String userWinningComponentId);

}
