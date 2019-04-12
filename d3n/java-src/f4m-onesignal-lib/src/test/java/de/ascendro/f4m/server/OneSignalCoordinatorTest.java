package de.ascendro.f4m.server;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonElement;

import de.ascendro.f4m.server.onesignal.OneSignalCoordinator;
import de.ascendro.f4m.server.onesignal.OneSignalWrapper;
import de.ascendro.f4m.server.onesignal.model.OneSignalPushData;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;
import de.ascendro.f4m.service.util.ProfileServiceTestHelper;

public class OneSignalCoordinatorTest {
	private static final String NOTIF_ID2 = "notif-id2";
	private static final String NOTIF_ID = "notif-id";
	@Mock
	private CommonProfileAerospikeDao profileAerospikeDao;
	@Mock
	private OneSignalWrapper wrapper;

	@InjectMocks
	private OneSignalCoordinator coordinator;
	
	private ArgumentCaptor<Map<ISOLanguage,String>> messageArg;
	private final static String USER_ID = "123";

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(profileAerospikeDao.getProfile(any())).thenReturn(ProfileServiceTestHelper.getTestProfile());
		when(wrapper.sendMobilePushToTag(any(), any(), any())).thenReturn("xxx");

		messageArg = ArgumentCaptor.forClass(Map.class);
	}

	@Test
	public void testSendMobilePushWithPlaceholders() {
		// this test is redundant, since placeholders and translations are not handled within OneSignalCoordinator, maybe it should be removed?
		Map<ISOLanguage, String> messages = new HashMap<>();
		messages.put(ISOLanguage.EN, "Welcome Name to here");
		OneSignalPushData message = new OneSignalPushData(KeyStoreTestUtil.REGISTERED_USER_ID, WebsocketMessageType.GAME_END.toString(),
				messages, new String[] {KeyStoreTestUtil.APP_ID},
				mock(JsonElement.class).toString());
		coordinator.sendMobilePush(message);
		verify(wrapper).sendMobilePushToUser(any(), any(), messageArg.capture(), any(), any());
		assertThat(messageArg.getValue().get(ISOLanguage.EN), equalTo("Welcome Name to here"));
	}

	@Test
	public void testCancelMobilePush() {
		String[] notificationIds = new String[] {NOTIF_ID, NOTIF_ID2};
		coordinator.cancelMobilePush(notificationIds);
		verify(wrapper).cancelNotification(eq(NOTIF_ID));
		verify(wrapper).cancelNotification(eq(NOTIF_ID2));
	}

	@Test
	public void testSetTags() throws Exception {
		Map<String, String> testTags = new HashMap<>();
		testTags.put("TESTTAG1", "TEST");
		testTags.put("TESTTAG2", "TEST2");
		coordinator.setTags(USER_ID, testTags);
		verify(wrapper).setTags("one_signal_id_1", testTags);
		verify(wrapper).setTags("one_signal_id_2", testTags);
	}
}
