package de.ascendro.f4m.service.payment.dao;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import de.ascendro.f4m.server.util.JsonUtil;

public class TenantFileDaoImplTest extends TenantDaoTest {
	@Spy
	JsonUtil jsonUtil = new JsonUtil();
	@InjectMocks
	private TenantFileDaoImpl tenantFileDaoImpl;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Override
	public TenantDao getTenantDao() {
		return tenantFileDaoImpl;
	}

	private void loadData() throws IOException {
		try (InputStream is = this.getClass().getResourceAsStream("tenantInfos.json")) {
			tenantFileDaoImpl.loadTenantInfo(is);
		}
	}
	
	@Override
	public void beforeTestLoadingAndRead() throws Exception {
		loadData();
	}

	@Override
	public void beforeTestTenantNotFound() throws Exception {
		loadData();
	}

}
