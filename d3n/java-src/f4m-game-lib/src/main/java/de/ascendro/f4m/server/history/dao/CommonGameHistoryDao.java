package de.ascendro.f4m.server.history.dao;

import java.util.List;

import de.ascendro.f4m.server.AerospikeDao;

public interface CommonGameHistoryDao extends AerospikeDao {

	List<String> getUserGameHistory(String userId);
	
}
