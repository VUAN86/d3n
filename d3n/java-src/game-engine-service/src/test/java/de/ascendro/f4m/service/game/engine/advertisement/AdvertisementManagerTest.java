package de.ascendro.f4m.service.game.engine.advertisement;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.advertisement.AdvertisementManager;
import de.ascendro.f4m.server.advertisement.dao.AdvertisementDao;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.util.random.RandomUtil;

public class AdvertisementManagerTest {

	private static final String ADVERTISEMENT_BLOB_KEY_MATCHE_REGEXP = "^provider_(\\d+)_advertisement_(\\d+).json$";
	private static final int ADVERTISEMENT_COUNT_MIN = 3;
	private static final int ADVERTISEMENT_COUNT_MAX = 100;
	private static final int COUNT = 5;
	private static final long PROVIDER_ID = 558;
	
	@Mock
	private RandomUtil randomUtil;
	@Mock
	private AdvertisementDao advertisementDao;
	@Mock	
	private GameInstanceAerospikeDao gameInstanceAerospikeDao;
	@Mock
	private Tracker tracker;

	private AdvertisementManager advertisementManager;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(randomUtil.nextLong(anyLong()))
			.thenAnswer((args) -> RandomUtils.nextLong(0, (Long) args.getArgument(0)));
		advertisementManager = new AdvertisementManager(tracker, new GameEngineConfig(), advertisementDao, randomUtil);
	}
	
	@Test
	public void testGetRandomAdvertisementBlobKeysWithNoProvider(){
		when(advertisementDao.getAdvertisementCount(PROVIDER_ID)).thenReturn(null);
		final String[] randomAdvertisementBlobKeys = advertisementManager.getRandomAdvertisementBlobKeys(10, PROVIDER_ID);
		assertNotNull(randomAdvertisementBlobKeys);
		assertEquals(0, randomAdvertisementBlobKeys.length);
	}

	@Test
	public void testGetRandomAdvertisementBlobKeys() {		
		//not enough advertisements
		when(advertisementDao.getAdvertisementCount(PROVIDER_ID)).thenReturn(ADVERTISEMENT_COUNT_MIN);
		final String[] limitedAdvertisementBlobKeys = advertisementManager.getRandomAdvertisementBlobKeys(COUNT, PROVIDER_ID);
		verify(randomUtil, never()).nextLong(anyLong());
		assertThat(limitedAdvertisementBlobKeys, equalTo(new String[] { 
				advertisementManager.getBlobKey(PROVIDER_ID, 1), 
				advertisementManager.getBlobKey(PROVIDER_ID, 2),
				advertisementManager.getBlobKey(PROVIDER_ID, 3), 
				advertisementManager.getBlobKey(PROVIDER_ID, 1),
				advertisementManager.getBlobKey(PROVIDER_ID, 2) 
			}
		));
		
		//plenty of advertisements
		when(advertisementDao.getAdvertisementCount(PROVIDER_ID)).thenReturn(ADVERTISEMENT_COUNT_MAX);		
		final String[] randomAdvertisementBlobKeys = advertisementManager.getRandomAdvertisementBlobKeys(COUNT, PROVIDER_ID);
		verify(randomUtil, times(COUNT)).nextLong(anyLong());

		F4MAssert.assertSize(COUNT, Arrays.asList(randomAdvertisementBlobKeys));
		for(String advBlobKey : randomAdvertisementBlobKeys){
			assertTrue(advBlobKey + " does not match pattern " + ADVERTISEMENT_BLOB_KEY_MATCHE_REGEXP, 
					advBlobKey.matches(ADVERTISEMENT_BLOB_KEY_MATCHE_REGEXP));
		}
		
		final Pattern numberMatcher = Pattern.compile("(\\d+)");		
		final Set<Integer> advertisementIndexSet = Arrays.stream(randomAdvertisementBlobKeys)
			.map(key -> numberMatcher.matcher(key))
			.filter(m -> m.find())//find provider id
			.filter(m -> m.find())//find advertisement index
			.map(m -> m.group(1))//pick advertisement index
			.map(key -> Integer.valueOf(key))
			.collect(Collectors.toSet());
		F4MAssert.assertSize(COUNT, advertisementIndexSet);
		assertThat(advertisementIndexSet, everyItem(greaterThan(0)));
		assertThat(advertisementIndexSet, everyItem(lessThanOrEqualTo(ADVERTISEMENT_COUNT_MAX)));
	}
	
	@Test
	public void testGetBlobKey(){
		assertEquals("provider_5_advertisement_12.json", advertisementManager.getBlobKey(5, 12));
	}	
}
