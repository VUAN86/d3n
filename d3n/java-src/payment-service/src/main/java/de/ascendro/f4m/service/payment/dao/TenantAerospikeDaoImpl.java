package de.ascendro.f4m.service.payment.dao;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.payment.exception.F4MTentantNotFoundException;

public class TenantAerospikeDaoImpl implements TenantDao {
	public static final String TENANT_SET_NAME = "tenant";
	public static final String TENANT_BIN_NAME = "value";
	public static final String TENANT_KEY_PREFIX = "tenant" +  PrimaryKeyUtil.KEY_ITEM_SEPARATOR;
	
	private AerospikeDao aerospikeDao;
	private JsonUtil jsonUtil;

	@Inject
	public TenantAerospikeDaoImpl(JsonUtil jsonUtil, AerospikeDao aerospikeDao) {
		this.jsonUtil = jsonUtil;
		this.aerospikeDao = aerospikeDao;
	}
	
	@Override
	public TenantInfo getTenantInfo(String tenantId) {
		TenantInfo tenantInfo = null;
		final String key = getPrimaryKey(tenantId);
		//Probably some kind of caching might be used, because tenantConfig should not change too often. But here is the hard question - how can we know if it has been changed?
		String tenantAsString = aerospikeDao.readJson(TENANT_SET_NAME, key, TENANT_BIN_NAME);
		if (StringUtils.isNotBlank(tenantAsString)) {
			tenantInfo = jsonUtil.fromJson(tenantAsString, TenantInfo.class);
			tenantInfo.setExchangeRates(TenantUtil.filterExchangeRates(tenantInfo));
		}
		if (tenantInfo == null) {
			throw new F4MTentantNotFoundException("Tenant not found " + tenantId);
		}
		return tenantInfo;
	}
	
	private String getPrimaryKey(String userId) {
		return TENANT_KEY_PREFIX + userId;
	}
}
