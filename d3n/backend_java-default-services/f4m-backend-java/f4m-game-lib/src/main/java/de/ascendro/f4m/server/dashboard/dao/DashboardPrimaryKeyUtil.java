package de.ascendro.f4m.server.dashboard.dao;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

public class DashboardPrimaryKeyUtil extends PrimaryKeyUtil<String> {

	private static final String DASHBOARD = "dashboard";

	@Inject
	public DashboardPrimaryKeyUtil(Config config) {
		super(config);
	}

	/**
	 * @param userId
	 * @return User Info key as dashboard:tenant:[tenantId]:user:[userId]
	 */
	public String createUserInfoKey(String tenantId, String userId) {
		return String.format("dashboard:tenant:%s:user:%s", tenantId, userId);
	}

	/**
	 * @param gameId
	 * @return Game Info key as dashboard:game:[gameId]
	 */
	public String createGameInfoKey(String gameId) {
		return String.format("dashboard:game:%s", gameId);
	}

	@Override
	protected String getServiceName() {
		return DASHBOARD;
	}

}
