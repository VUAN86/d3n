package de.ascendro.f4m.service.winning.dao;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.service.winning.model.SuperPrize;

public interface SuperPrizeAerospikeDao extends AerospikeDao {

	public static final String BLOB_BIN_NAME = "value";
	
	SuperPrize getSuperPrize(String superPrizeId);
	
}
