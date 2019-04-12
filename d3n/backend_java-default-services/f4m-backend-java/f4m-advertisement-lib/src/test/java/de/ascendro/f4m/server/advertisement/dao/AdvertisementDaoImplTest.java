package de.ascendro.f4m.server.advertisement.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;

import de.ascendro.f4m.server.advertisement.config.AdvertisementConfig;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;

public class AdvertisementDaoImplTest extends RealAerospikeTestBase {

	private static final long PROVIDER_ID = 0;
	private static final Integer ADVERTISEMENT_COUNT = 100;

	private final Config config = new AdvertisementConfig();
	private final AdvertisementPrimaryKeyUtil advertisementPrimaryKeyUtil = new AdvertisementPrimaryKeyUtil(config);
	private final JsonUtil jsonUtil = new JsonUtil();

	private final String namespace = config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
	private final String testAdvertisementSet = config.getProperty(AdvertisementConfig.AEROSPIKE_ADVERTISEMENT_SET)
			+ "_test";
	private AdvertisementDao advertisementDao;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		config.setProperty(AdvertisementConfig.AEROSPIKE_ADVERTISEMENT_SET, testAdvertisementSet);
	}

	@Override
	protected void setUpAerospike() {
		advertisementDao = new AdvertisementDaoImpl(config, advertisementPrimaryKeyUtil, jsonUtil,
				aerospikeClientProvider);
	}
	
	@After
	@Override
	public void tearDown(){
		super.tearDown();
		clearSet(namespace, testAdvertisementSet);
	}

	@Test
	public void testGetAdvertisementCount() {
		//Prepare provider counter
		final String advKey = advertisementPrimaryKeyUtil.createPrimaryKey(PROVIDER_ID);
		assertEquals("provider:" + PROVIDER_ID + ":counter", advKey);
		
		final Bin counterBin = new Bin(AdvertisementDaoImpl.COUNTER_BIN_NAME, ADVERTISEMENT_COUNT);
		aerospikeClientProvider.get().put(null, new Key(namespace, testAdvertisementSet, advKey), counterBin);

		//assert value of counter
		assertEquals(advertisementDao.getAdvertisementCount(PROVIDER_ID), ADVERTISEMENT_COUNT);
	}
	
	@Test
	public void testGetAdvertisementCountIfNoProvider() {
		assertNull(advertisementDao.getAdvertisementCount(PROVIDER_ID));
	}

}
