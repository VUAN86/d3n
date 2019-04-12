package de.ascendro.f4m.service.game.selection.dao;

import java.util.Map;

import com.google.gson.JsonArray;

import de.ascendro.f4m.server.game.GameAerospikeDao;

/**
 * Game Selection Service Data Access Object interface for Game
 * 
 */
public interface GameSelectorAerospikeDao extends GameAerospikeDao {

	/**
	 * Get list of games by mandatory attributes Tenant and App IDs
	 * 
	 * @param tenantId
	 *            {@link String}
	 * @param appId
	 *            {@link String}
	 * @return {@link JsonArray} of games basic info
	 */
	JsonArray getGameList(String tenantId, String appId);

	/**
	 * Get list of games by mandatory attributes Tenant and App IDs and
	 * additional search field
	 * 
	 * @param tenantId
	 *            {@link String}
	 * @param appId
	 *            {@link String}
	 * @param searchField
	 *            {@link String}
	 * @param searchValue
	 *            {@link String}
	 * @return {@link JsonArray} of games basic info
	 */
    JsonArray getGameList(String tenantId, String appId, String searchField, String searchValue);

	/**
	 * Get list of games by mandatory attributes Tenant and App IDs and
	 * additional search fields
	 * 
	 * @param tenantId
	 *            {@link String}
	 * @param appId
	 *            {@link String}
	 * @param searchFields
	 *            {@link Map<String, String>}
	 * @return {@link JsonArray} of games basic info
	 */
    JsonArray getGameList(String tenantId, String appId, Map<String, String> searchFields);

}
