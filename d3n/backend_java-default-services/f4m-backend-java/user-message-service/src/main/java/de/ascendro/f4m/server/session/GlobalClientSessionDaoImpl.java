package de.ascendro.f4m.server.session;

import java.util.Map;

import javax.inject.Inject;

import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.server.util.JsonUtil;

public class GlobalClientSessionDaoImpl implements GlobalClientSessionDao {
	public static final String SESSION_SET_NAME = "session";
	public static final String SESSION_BIN_NAME = "sessionsMap";
	
	private AerospikeOperateDao aerospikeOperateDao;
	private JsonUtil jsonUtil;
	private GlobalClientSessionPrimaryKeyUtil globalClientSessionPrimaryKeyUtil;
	
	@Inject
	public GlobalClientSessionDaoImpl(JsonUtil jsonUtil, AerospikeOperateDao aerospikeDao, GlobalClientSessionPrimaryKeyUtil globalClientSessionPrimaryKeyUtil) {
		this.jsonUtil = jsonUtil;
		this.aerospikeOperateDao = aerospikeDao;
		this.globalClientSessionPrimaryKeyUtil = globalClientSessionPrimaryKeyUtil;
	}	

	@Override
	public GlobalClientSessionInfo getGlobalClientSessionInfoByUserId(String userId) {
		final String key = globalClientSessionPrimaryKeyUtil.createPrimaryKey(userId);
		
		Map<Object, Object> all = aerospikeOperateDao.getAllMap(SESSION_SET_NAME, key, SESSION_BIN_NAME);
		return all.values().stream()
				.map(e -> jsonUtil.fromJson(jsonUtil.toJson(e), GlobalClientSessionInfo.class))
				.sorted(GlobalClientSessionInfo.getNewestFirstComparator())
				.findFirst().orElse(null);
	}
}
