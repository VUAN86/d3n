package de.ascendro.f4m.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.util.JsonLoader;

public class GlobalClientSessionDaoTest {
	public static final Type SESSION_INFO_TYPE = new TypeToken<Map<?, Map<?, ?>>>() {}.getType();

	
	private GlobalClientSessionDao globalClientSessionDao;
	@Mock
	private AerospikeOperateDao aerospikeDao;

	private GlobalClientSessionPrimaryKeyUtil globalClientSessionPrimaryKeyUtil;
	private JsonUtil jsonUtil;
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		jsonUtil = new JsonUtil();
		globalClientSessionPrimaryKeyUtil = new GlobalClientSessionPrimaryKeyUtil(null);
		globalClientSessionDao = new GlobalClientSessionDaoImpl(jsonUtil , aerospikeDao, globalClientSessionPrimaryKeyUtil);
	}

	@Test
	public void testSessionReturn() throws Exception {
		final String clientSessionJson = JsonLoader.getTextFromResources("globalClientSession.json", getClass());
		Map<Object, Object> fromJson = jsonUtil.fromJson(clientSessionJson, SESSION_INFO_TYPE);
		when(aerospikeDao.getAllMap(anyString(), anyString(), anyString())).thenReturn(fromJson);
		GlobalClientSessionInfo session = globalClientSessionDao.getGlobalClientSessionInfoByUserId("userId");
		assertNotNull(session);
		assertEquals("clientId2", session.getClientId());
		assertEquals("url2", session.getGatewayURL());
		assertEquals("TID-7898989", session.getAppConfig().getTenantId());
	}

	@Test
	public void testNoSession() throws Exception {
		String clientSessionJson = null;
		when(aerospikeDao.readJson(anyString(), anyString(), anyString())).thenReturn(clientSessionJson);
		GlobalClientSessionInfo session = globalClientSessionDao.getGlobalClientSessionInfoByUserId("userId");
		assertNull(session);
	}
}
