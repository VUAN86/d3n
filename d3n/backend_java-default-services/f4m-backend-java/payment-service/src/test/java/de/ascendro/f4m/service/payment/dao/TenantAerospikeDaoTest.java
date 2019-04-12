package de.ascendro.f4m.service.payment.dao;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.util.JsonLoader;

public class TenantAerospikeDaoTest extends TenantDaoTest {
	@Spy
	private JsonUtil jsonUtil = new JsonUtil();
	@Mock
	private AerospikeDao aerospikeDao;
	@InjectMocks
	private TenantAerospikeDaoImpl tenantDao;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Override
	public TenantDao getTenantDao() {
		return tenantDao;
	}

	@Override
	public void beforeTestLoadingAndRead() throws Exception {
		String tenantJson = JsonLoader.getTextFromResources("tenantInfos.json", getClass());
		tenantJson = tenantJson.substring(1, tenantJson.length() - 2); //removes leading [ and trailing ] to leave singe JSON instead of array
		when(aerospikeDao.readJson("tenant", "tenant:1", "value")).thenReturn(tenantJson);
	}

	@Override
	public void beforeTestTenantNotFound() throws Exception {
	}
}
