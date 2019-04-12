package de.ascendro.f4m.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.ResultCode;
import com.aerospike.client.cluster.Node;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class AerospikeDaoTest {

	private static final String AEROSPIKE_COMMUNITY_EDITION = "Aerospike Community Edition build 3.10.0.1";
	private static final String AEROSPIKE_ENTERPRISE_EDITION = "Aerospike Enterprise Edition build 3.10.1.1";
	
	@Mock
	private IAerospikeClient mockClient;
	@Mock
	private ServiceMonitoringRegister serviceMonitoringRegister;
	@Mock
	private Node node1;
	@Mock
	private Node node2;

	private Config config = new AerospikeConfigImpl();
	private JsonUtil jsonUtil = new JsonUtil();
	private PrimaryKeyUtil<String> primaryKeyUtil = new PrimaryKeyUtil<>(config);
	private Function<Node, String> requestInfoResultSupplier = null;

	private AerospikeDaoImpl<PrimaryKeyUtil<String>> aerospikeDao;
	private AerospikeClientProvider aerospikeClientProvider;
	private Key key;
	private List<Bin> binList;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		
		when(mockClient.getNodes()).thenReturn(new Node[] { node1, node2 });
		aerospikeClientProvider = new AerospikeClientProvider(config, serviceMonitoringRegister) {
			@Override
			protected void initAerospikeClient() {
				aerospikeClient = mockClient;
			}			
		};
		aerospikeDao = new AerospikeDaoImpl<PrimaryKeyUtil<String>>(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider){
			@Override
			protected String requestInfo(Node node, String name) throws AerospikeException {
				return requestInfoResultSupplier.apply(node);
			}
		};
		key = new Key("test", "setName", "key");
		binList = Arrays.asList(new Bin("name", "John"), new Bin("surname", "Kennedy"));
	}

	@Test
	public void testConnection() {
		aerospikeDao.isConnected();
		verify(mockClient, times(1)).isConnected();
	}

	@Test(expected = F4MEntryNotFoundException.class)
	public void testDeleteWhenDoesNotExist() {
		aerospikeDao.delete(key.setName, key.userKey.toString());

		verify(mockClient, times(1)).delete(any(), eq(key));
	}

	@Test
	public void testReadString() {
		Bin bin = binList.get(0);
		aerospikeDao.readString(key.setName, key.userKey.toString(), bin.name);

		verify(mockClient, times(1)).get(isNull(), eq(key));
	}

	@Test
	public void testReadJson() {
		Bin bin = binList.get(0);
		aerospikeDao.readJson(key.setName, key.userKey.toString(), bin.name);

		verify(mockClient, times(1)).get(isNull(), eq(key));
	}

	@Test
	public void testIsDurableDeleteSupported() {
		requestInfoResultSupplier = (node) -> AEROSPIKE_COMMUNITY_EDITION;
		assertFalse(aerospikeDao.isDurableDeleteSupported());
		
		aerospikeDao.durableDeleteSupported = null;//clear cache

		requestInfoResultSupplier = (node) -> AEROSPIKE_ENTERPRISE_EDITION;
		assertTrue(aerospikeDao.isDurableDeleteSupported());
	}
	
	@Test
	public void testIsDurableDeleteSupportedForFirstNodeFailing() {
		requestInfoResultSupplier = (node) -> {
			if(node == node1){
				throw new AerospikeException(ResultCode.KEY_BUSY);
			}else{
				return AEROSPIKE_ENTERPRISE_EDITION;
			}
		};

		assertTrue(aerospikeDao.isDurableDeleteSupported());
	}
	
	@Test
	public void testIsDurableDeleteSupportedForAllNodesFailing() {
		requestInfoResultSupplier = (node) -> {
			throw new AerospikeException(ResultCode.KEY_BUSY);
		};

		try{
			aerospikeDao.isDurableDeleteSupported();
			fail("Expected AerospikeException");
		}catch(AerospikeException aEx){
			assertNull(aerospikeDao.durableDeleteSupported);
		}
	}

}
