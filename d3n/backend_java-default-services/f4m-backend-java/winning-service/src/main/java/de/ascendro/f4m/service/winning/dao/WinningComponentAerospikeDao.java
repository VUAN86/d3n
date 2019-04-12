package de.ascendro.f4m.service.winning.dao;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.service.winning.model.WinningComponent;

public interface WinningComponentAerospikeDao extends AerospikeDao {

	public static final String BLOB_BIN_NAME = "value";
	
	WinningComponent getWinningComponent(String id);
	
}
