package de.ascendro.f4m.service.payment.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.payment.exception.F4MTentantNotFoundException;

public class TenantFileDaoImpl implements TenantDao {
	private JsonUtil jsonUtil;
	private Map<String, TenantInfo> tenantMap;
	private String path;

	@Inject
	public TenantFileDaoImpl(JsonUtil jsonUtil) {
		this.jsonUtil = jsonUtil;
	}
	
	protected synchronized void loadTenantInfoFromFile() {
		try (InputStream is = new FileInputStream(getPath())) {
			loadTenantInfo(is);
		} catch (IOException e) {
			throw new F4MFatalErrorException("Could not read tenant configuration from file", e);
		}
	}
	
	protected void loadTenantInfo(InputStream is) throws IOException {
		String json = IOUtils.toString(is, StandardCharsets.UTF_8);
		final List<TenantInfo> tenants = jsonUtil.<TenantInfo> getEntityListFromJsonString(json,
				new TypeToken<List<TenantInfo>>() {}.getType());
		tenantMap = new HashMap<>();
		for (TenantInfo tenantInfo : tenants) {
			tenantMap.put(tenantInfo.getTenantId(), tenantInfo);
		}
	}
	
	private boolean isInitialized() {
		return tenantMap != null;
	}

	@Override
	public TenantInfo getTenantInfo(String tenantId) {
		if (!isInitialized()) {
			this.loadTenantInfoFromFile();
		}
		TenantInfo tenantInfo = tenantMap.get(tenantId);
		if (tenantInfo != null) {
			tenantInfo.setExchangeRates(TenantUtil.filterExchangeRates(tenantInfo));
		} else {
			throw new F4MTentantNotFoundException("Tenant not found " + tenantId);
		}
		return tenantInfo;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
