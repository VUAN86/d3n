package de.ascendro.f4m.server.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.aerospike.client.IAerospikeClient;
import com.github.srini156.aerospike.client.MockAerospikeClient;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.server.profile.util.ApplicationConfigrationPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;

public class ApplicationConfigurationAerospikeDaoImplTest {
	@Spy //necessary because AerospikeDaoImpl uses some values
	private AerospikeConfigImpl config;
	@Mock
	private ApplicationConfigrationPrimaryKeyUtil applicationConfigrationPrimaryKeyUtil;
	@Spy
	private JsonUtil jsonUtil;
	@Mock
	private AerospikeClientProvider aerospikeClientProvider;	
	@InjectMocks
	private ApplicationConfigurationAerospikeDaoImpl appConfigDao;
	@Mock
	private IAerospikeClient aerospikeClient;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		//cannot use AerospikeClientMock, because it is on the wrong side of dependency
		//consider injecting not extending AerospikeDaoImpl in ApplicationConfigurationAerospikeDaoImpl
		//it would help creating unit test and also shows possible flaw in the design
		aerospikeClient = new MockAerospikeClient();
		when(aerospikeClientProvider.get()).thenReturn(aerospikeClient);
	}

	@Test
	public void testGetAppConfiguration() throws Exception {
		when(applicationConfigrationPrimaryKeyUtil.createPrimaryKey("tenantId", "appId")).thenReturn("tenant-appKey");
		String appConfigValue = IOUtils.toString(this.getClass().getResourceAsStream("appConfig.json"), "UTF-8");
		appConfigDao.createJson(appConfigDao.getSet(), "tenant-appKey", ApplicationConfigurationAerospikeDao.BLOB_BIN_NAME, appConfigValue );
		
		AppConfig appConfig = appConfigDao.getAppConfiguration("tenantId", "appId");
		assertNotNull(appConfig);
		assertEquals("Intellex d.o.o.", appConfig.getTenant().getName());
		assertEquals("http://www.intellex.rs", appConfig.getTenant().getUrl());
		assertEquals("Working App, don't change!", appConfig.getApplication().getTitle());
		assertEquals("http://f4m-nightly-medias.s3.amazonaws.com/", appConfig.getApplication().getConfiguration().getCdnMedia());
		assertEquals("friends4media GmbH", appConfig.getFriendsForMedia().getName());
		assertEquals("http://www.f4m.tv/", appConfig.getFriendsForMedia().getUrl());
	}

	@Test
	public void getAppConfigurationWhenNotFound() throws Exception {
		when(applicationConfigrationPrimaryKeyUtil.createPrimaryKey("tenantId", "appId")).thenReturn("tenant-appKey");
		try {
			AppConfig appConfig = appConfigDao.getAppConfiguration("tenantId", "appId");
			assertNotNull(appConfig);
			fail();
		} catch (F4MEntryNotFoundException e) {
			assertEquals("AppConfig not found", e.getMessage());
		}
	}

	@Test
	public void getAppConfigurationAsJsonElementWhenNotFound() throws Exception {
		when(applicationConfigrationPrimaryKeyUtil.createPrimaryKey("tenantId", "appId")).thenReturn("tenant-appKey");
		try {
			appConfigDao.getAppConfigurationAsJsonElement("tenantId", "appId");
			fail();
		} catch (F4MEntryNotFoundException e) {
			assertEquals("AppConfig not found", e.getMessage());
		}
	}
}
