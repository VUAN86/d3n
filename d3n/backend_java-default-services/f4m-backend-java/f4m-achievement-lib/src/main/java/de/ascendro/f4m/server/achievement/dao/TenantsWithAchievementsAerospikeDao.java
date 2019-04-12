package de.ascendro.f4m.server.achievement.dao;

import java.util.List;

import de.ascendro.f4m.server.AerospikeDao;

public interface TenantsWithAchievementsAerospikeDao extends AerospikeDao {
	
	List<String> getTenantIdList();

}
