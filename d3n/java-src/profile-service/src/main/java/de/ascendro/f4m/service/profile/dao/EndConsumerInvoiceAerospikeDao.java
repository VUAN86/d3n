package de.ascendro.f4m.service.profile.dao;

import java.util.List;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeOperateDao;

public interface EndConsumerInvoiceAerospikeDao extends AerospikeOperateDao {
	
	public static final String BLOB_BIN_NAME = "endConsInvoice";
	
	/**
	 * Returns a list of end consumer invoices belonging to specified tenant and user
	 * @param tenantId
	 * @param userId
	 * @param offset
	 * @param limit
	 * @return a list of JsonObject objects representing invoices
	 */
	List<JsonObject> getInvoiceList(String tenantId, String userId, long offset, int limit);

}
