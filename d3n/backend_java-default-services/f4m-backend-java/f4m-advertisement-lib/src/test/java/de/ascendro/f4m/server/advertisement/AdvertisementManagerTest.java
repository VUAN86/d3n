package de.ascendro.f4m.server.advertisement;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.advertisement.config.AdvertisementConfig;
import de.ascendro.f4m.server.advertisement.dao.AdvertisementDao;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.util.random.RandomUtil;

public class AdvertisementManagerTest {

    private static final String BLOB_KEY = "blob";
    private static final String ADVERTISEMENT_BLOB_KEY_MATCHE_REGEXP = "^provider_(\\d+)_advertisement_(\\d+).json$";
    private static final int ADVERTISEMENT_COUNT_MIN = 3;
    private static final int ADVERTISEMENT_COUNT_MAX = 100;
    private static final int COUNT = 5;
    private static final long PROVIDER_ID = 558;

    @Mock
    private Tracker tracker;
    @Mock
    private AdvertisementDao advertisementDao;


    private AdvertisementConfig config = new AdvertisementConfig();

    @Mock
    private RandomUtil randomUtil;

    private AdvertisementManager manager;
    private ClientInfo clientInfo;
    private static final Logger LOGGER = LoggerFactory.getLogger(AdvertisementManagerTest.class);

    private ArgumentCaptor<ClientInfo> clientInfoArgument;
    private ArgumentCaptor<AdEvent> adEventArgument;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        manager = new AdvertisementManager(tracker, config, advertisementDao, randomUtil);
        clientInfoArgument = ArgumentCaptor.forClass(ClientInfo.class);
        adEventArgument = ArgumentCaptor.forClass(AdEvent.class);

        setClientInfo(ANONYMOUS_CLIENT_INFO);

    }

    private void setClientInfo(ClientInfo clientInfo){
        try {
            this.clientInfo = (ClientInfo) BeanUtils.cloneBean(clientInfo);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                | NoSuchMethodException e) {
            LOGGER.error("Failed to init default client info", e);
        }
    }

    @Test
    public void testMarkAdvertisementShown() {
        manager.markAdvertisementShown(clientInfo, BLOB_KEY, null);
        verify(tracker, times(1)).addEvent(clientInfoArgument.capture(), adEventArgument.capture());
        assertEquals(clientInfo, clientInfoArgument.getValue());
        assertEquals(BLOB_KEY, adEventArgument.getValue().getBlobKey());
        assertEquals(0, adEventArgument.getValue().getGameId(), 0);
    }

    @Test
    public void testMarkAdvertisementShownWithGameId() {
        manager.markAdvertisementShown(clientInfo, BLOB_KEY, "1");
        verify(tracker, times(1)).addEvent(clientInfoArgument.capture(), adEventArgument.capture());
        assertEquals(clientInfo, clientInfoArgument.getValue());
        assertEquals(BLOB_KEY, adEventArgument.getValue().getBlobKey());
        assertEquals(1, adEventArgument.getValue().getGameId(), 0);
    }

    @Test(expected = F4MEntryNotFoundException.class)
    public void testExceptionAdvertisementShownWhenNullBlob() {
        manager.markAdvertisementShown(clientInfo, null, null);
    }

    @Test
    public void testGetRandomAdvertisementBlobKeysWithNoProvider(){
        when(advertisementDao.getAdvertisementCount(PROVIDER_ID)).thenReturn(null);
        final String[] randomAdvertisementBlobKeys = manager.getRandomAdvertisementBlobKeys(10, PROVIDER_ID);
        assertNotNull(randomAdvertisementBlobKeys);
        assertEquals(0, randomAdvertisementBlobKeys.length);
    }

    @Test
    public void testGetRandomAdvertisementBlobKeys() {
        //not enough advertisements
        when(advertisementDao.getAdvertisementCount(PROVIDER_ID)).thenReturn(ADVERTISEMENT_COUNT_MIN);
        final String[] limitedAdvertisementBlobKeys = manager.getRandomAdvertisementBlobKeys(COUNT, PROVIDER_ID);
        verify(randomUtil, times(0)).nextLong(anyLong());
        assertThat(limitedAdvertisementBlobKeys, equalTo(new String[] {
                manager.getBlobKey(PROVIDER_ID, 1),
                manager.getBlobKey(PROVIDER_ID, 2),
                manager.getBlobKey(PROVIDER_ID, 3),
                manager.getBlobKey(PROVIDER_ID, 1),
                manager.getBlobKey(PROVIDER_ID, 2)
                }
        ));

        //plenty of advertisements
        when(advertisementDao.getAdvertisementCount(PROVIDER_ID)).thenReturn(ADVERTISEMENT_COUNT_MAX);
        final String[] randomAdvertisementBlobKeys = manager.getRandomAdvertisementBlobKeys(COUNT, PROVIDER_ID);
        verify(randomUtil, times(COUNT)).nextLong(anyLong());

        F4MAssert.assertSize(COUNT, Arrays.asList(randomAdvertisementBlobKeys));
        for(String advBlobKey : randomAdvertisementBlobKeys){
            assertTrue(advBlobKey + " does not match pattern " + ADVERTISEMENT_BLOB_KEY_MATCHE_REGEXP,
                    advBlobKey.matches(ADVERTISEMENT_BLOB_KEY_MATCHE_REGEXP));
        }

        final Pattern numberMatcher = Pattern.compile("(\\d+)");
        final Set<Integer> advertisementIndexSet = Arrays.stream(randomAdvertisementBlobKeys)
                .map(numberMatcher::matcher)
                .filter(Matcher::find)//find provider id
                .filter(Matcher::find)//find advertisement index
                .map(m -> m.group(1))//pick advertisement index
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
        F4MAssert.assertSize(COUNT, advertisementIndexSet);
        assertThat(advertisementIndexSet, everyItem(greaterThan(0)));
        assertThat(advertisementIndexSet, everyItem(lessThanOrEqualTo(ADVERTISEMENT_COUNT_MAX)));
    }

    @Test
    public void testGetBlobKey(){
        assertEquals("provider_5_advertisement_12.json", manager.getBlobKey(5, 12));
    }

}
