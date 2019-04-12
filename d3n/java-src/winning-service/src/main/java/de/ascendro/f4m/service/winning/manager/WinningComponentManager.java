package de.ascendro.f4m.service.winning.manager;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.winning.dao.UserWinningComponentAerospikeDao;
import de.ascendro.f4m.service.winning.dao.WinningComponentAerospikeDao;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.WinningComponent;
import de.ascendro.f4m.service.winning.model.WinningOption;

public class WinningComponentManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(WinningComponentManager.class);

	private final WinningComponentAerospikeDao winningComponentAerospikeDao;
	private final UserWinningComponentAerospikeDao userWinningComponentAerospikeDao;
	private final CommonGameInstanceAerospikeDao commonGameInstanceAerospikeDao;
	private final PrimaryKeyUtil<String> primaryKeyUtil;

	@Inject
	public WinningComponentManager(WinningComponentAerospikeDao winningComponentAerospikeDao,
			UserWinningComponentAerospikeDao userWinningComponentAerospikeDao,
			CommonGameInstanceAerospikeDao commonGameInstanceAerospikeDao, PrimaryKeyUtil<String> primaryKeyUtil) {
		this.winningComponentAerospikeDao = winningComponentAerospikeDao;
		this.userWinningComponentAerospikeDao = userWinningComponentAerospikeDao;
		this.commonGameInstanceAerospikeDao = commonGameInstanceAerospikeDao;
		this.primaryKeyUtil = primaryKeyUtil;
	}

	public WinningComponent getWinningComponent(String tenantId, String winningComponentId) {
		WinningComponent winningComponent = winningComponentAerospikeDao.getWinningComponent(winningComponentId);
		if (winningComponent == null) {
			LOGGER.error("Winning component not found (winningComponentId=[{}])", winningComponentId);
			throw new F4MEntryNotFoundException("Winning component not found with id " + winningComponentId);
		}
		if (!winningComponent.getTenants().contains(tenantId)) {
			throw new F4MEntryNotFoundException(
					"Winning component not found with id " + winningComponentId + " for tenant " + tenantId);
		}
		return winningComponent;
	}

	public UserWinningComponent getUserWinningComponent(String appId, String userId, String userWinningComponentId) {
		return userWinningComponentAerospikeDao.getUserWinningComponent(appId, userId, userWinningComponentId);
	}
	
	public List<JsonObject> getUserWinningComponents(String appId, String userId, int limit, long offset, List<OrderBy> orderBy) {
		return userWinningComponentAerospikeDao.getUserWinningComponents(appId, userId, limit, offset, orderBy);
	}

	public void markUserWinningComponentUsed(String appId, String userId, String userWinningComponentId) {
		userWinningComponentAerospikeDao.markUserWinningComponentUsed(appId, userId, userWinningComponentId);
	}

	public void saveUserWinningComponentWinningOption(String appId, String userId, String userWinningComponentId, WinningOption winning) {
		userWinningComponentAerospikeDao.saveUserWinningComponentWinningOption(appId, userId, userWinningComponentId, winning);
	}

	public void markUserWinningComponentFiled(String appId, String userId, String userWinningComponentId) {
		userWinningComponentAerospikeDao.markUserWinningComponentFiled(appId, userId, userWinningComponentId);
	}

	public UserWinningComponent assignWinningComponentToUser(String tenantId, String appId, String userId, String gameInstanceId,
			WinningComponent winningComponent, GameWinningComponentListItem winningComponentConfiguration) {
		if (! winningComponent.getTenants().contains(tenantId)) {
			throw new F4MFatalErrorException(String.format("Cannot assign winning component - wrong tenant (expected: %s, got: %s)", winningComponent.getTenants(), tenantId));
		}
		final UserWinningComponent userWinningComponent;
		final Game game = commonGameInstanceAerospikeDao.getGameByInstanceId(gameInstanceId);
		if (game != null) {
			final String userWinningComponentId = primaryKeyUtil.generateId();
			userWinningComponent = new UserWinningComponent(userWinningComponentId, winningComponent, winningComponentConfiguration, gameInstanceId,
					game.getGameId(), game.getType());
			commonGameInstanceAerospikeDao.addUserWinningComponentIdToGameInstance(gameInstanceId, userWinningComponentId, userWinningComponent.getType());
			userWinningComponentAerospikeDao.saveUserWinningComponent(appId, userId, userWinningComponent);
		} else {
			throw new F4MEntryNotFoundException("Game not found by gameInstanceId [" + gameInstanceId + "]");
		}
		return userWinningComponent;
	}

	public void moveWinningComponents(String sourceUserId, String targetUserId, Set<String> appIds) {
		if (appIds != null) {
			for (String appId : appIds) {
				userWinningComponentAerospikeDao.moveUserWinningComponents(appId, sourceUserId, targetUserId);
			}
		}
	}

}
