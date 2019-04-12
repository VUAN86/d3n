package de.ascendro.f4m.server;

import static de.ascendro.f4m.server.onesignal.OneSignalWrapper.TAG_APP;
import static de.ascendro.f4m.server.onesignal.OneSignalWrapper.TAG_PROFILE;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.currencyfair.onesignal.OneSignal;
import com.currencyfair.onesignal.model.notification.CreateNotificationResponse;
import com.currencyfair.onesignal.model.notification.Field;
import com.currencyfair.onesignal.model.notification.Filter;
import com.currencyfair.onesignal.model.notification.NotificationRequest;
import com.currencyfair.onesignal.model.notification.NotificationRequestBuilder;
import com.currencyfair.onesignal.model.notification.Relation;
import com.currencyfair.onesignal.model.notification.ViewNotificationResponse;
import com.currencyfair.onesignal.model.player.ViewDeviceResponse;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.onesignal.OneSignalWrapper;
import de.ascendro.f4m.server.onesignal.config.OneSignalConfigImpl;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class OneSignalExplorationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(OneSignalExplorationTest.class);
	
	@ClassRule
	public static final TemporaryFolder keystoreFolder = new TemporaryFolder();

	private String oneSignalAppId;
	private String oneSignalAppRestApiKey;
	private String profileIdTag;
	private String appIdTag;
	private boolean appCredentialsSpecified;

	private OneSignalConfigImpl config;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		KeyStoreTestUtil.initKeyStore(keystoreFolder);
	}
	
	@Before
	public void setUp() throws Exception {
		config = new OneSignalConfigImpl();
		oneSignalAppId = config.getProperty(OneSignalConfigImpl.ONE_SIGNAL_APP_ID);
		oneSignalAppRestApiKey = config.getProperty(OneSignalConfigImpl.ONE_SIGNAL_APP_REST_API_KEY);
		appIdTag = config.getProperty(OneSignalConfigImpl.APP_ID_TAG);
		profileIdTag = config.getProperty(OneSignalConfigImpl.PROFILE_ID_TAG);
		oneSignalAppRestApiKey = config.getProperty(OneSignalConfigImpl.ONE_SIGNAL_APP_REST_API_KEY);
		appCredentialsSpecified = StringUtils.isNotBlank(oneSignalAppId) && StringUtils.isNotBlank(oneSignalAppRestApiKey);
		Assume.assumeTrue("Please provide OneSignal App REST API Key in VM arguments to execute test",
				appCredentialsSpecified);

		Assume.assumeTrue("Please provide appIdTag and profileIdTag (used in filters for tests) in VM arguments to execute test",
				StringUtils.isNotBlank(appIdTag) && StringUtils.isNotBlank(profileIdTag));
	}
	
	@Test
	public void setPushToUserWithWrapper() throws Exception {
		Assume.assumeTrue(appCredentialsSpecified);
		OneSignalWrapper wrapper = new OneSignalWrapper(config);
		String result = wrapper.sendMobilePushToUser(profileIdTag, null,
			ImmutableMap.of(ISOLanguage.EN, "Hello message in English", ISOLanguage.DE, "Hallo Meldung auf deutsch"));
		assertThat(result, not(isEmptyString()));
	}

	@Test
	public void sendPushNotificationWithAppCredentials() throws Exception {
		Assume.assumeTrue(appCredentialsSpecified);
		sendPushNotification(null, oneSignalAppId, oneSignalAppRestApiKey, null);
		
		// test sending push notifications with delays
		CreateNotificationResponse response = sendPushNotification(null, oneSignalAppId, oneSignalAppRestApiKey, DateTimeUtil.getCurrentDateTime().plus(2, ChronoUnit.MINUTES));
		
		String notificationId = response.getId();
		assertNotNull(notificationId);
		
		ViewNotificationResponse viewNotification = OneSignal.viewNotification(oneSignalAppRestApiKey, oneSignalAppId, notificationId);
		
		assertNotNull(viewNotification);
		
		cancelNotification(notificationId);		
	}
	
	private void cancelNotification(String notificationId) {
		Assume.assumeTrue(appCredentialsSpecified);
		OneSignalWrapper wrapper = new OneSignalWrapper(config);
		wrapper.cancelNotification(notificationId);
	}

	private CreateNotificationResponse sendPushNotification(List<String> appIds, String appId, String authKey, ZonedDateTime scheduledDateTime) {
		List<Filter> filters = Arrays.asList(
				//How mgiId will be specified - new tag with empty/no value?
				//new Filter(Field.TAG, "mgiId:60551c1e-a718-11e6-555-76304dec7eb7", Relation.EXISTS, null),
				new Filter(Field.TAG, TAG_APP, Relation.EQUALS, appIdTag),
				new Filter(Field.TAG, TAG_PROFILE, Relation.EQUALS, profileIdTag)
				);
		NotificationRequestBuilder builder = NotificationRequestBuilder.aNotificationRequest()
				.withFilters(filters);
		if (appIds != null) {
			builder.withAppIds(appIds); //user auth key must be used to send to multiple appIds
			builder.withFilters(null);
		} else {
			builder.withAppId(appId); //App REST API Key must be used to send to send to single appId 
		}
		
		builder.withHeading(ISOLanguage.EN.toString(), "Heading in English")
			.withContent(ISOLanguage.EN.toString(), "Hello message in English");
		builder.withHeading(ISOLanguage.DE.toString(), "Ueberschrift auf deutsch")
			.withContent(ISOLanguage.DE.toString(), "Hallo Meldung auf deutsch");
		builder.withData(ImmutableMap.of("additionalDataField1", "value1", 
				"additionalDataField2", "value2"));

		if(scheduledDateTime != null) {
			builder.withSendAfter(scheduledDateTime.toString());
		}
		
		NotificationRequest notificationRequest = builder.build();
		CreateNotificationResponse response = OneSignal.createNotification(authKey, notificationRequest);
		LOGGER.info("Pushed notification {} with response {}", notificationRequest, response);
		assertNotNull(response);
		assertNull(response.getErrors());
		return response;
	}

	@Test
	public void testSendOnesignalMessageViaWrapper() {
		OneSignalWrapper wrapper = new OneSignalWrapper(config);
		Map<ISOLanguage, String> messages = new HashMap<>();
		messages.put(ISOLanguage.EN, "Message with payload");
		messages.put(ISOLanguage.DE, "Message with payload in german");

		JsonObject payload = new JsonObject();

		payload.addProperty("accept", true);
		payload.addProperty("inviteeId", "test");
		payload.addProperty("multiplayerGameInstanceId", "MGI-123");
		payload.addProperty("inviterId", "user_id_0");
		wrapper.sendMobilePushToUser(profileIdTag, null, messages,
				WebsocketMessageType.TEXT.toString(), payload.toString());
	}

	@Test
	public void testOneSignalViewDeviceAndEdit() {
		OneSignalWrapper wrapper = new OneSignalWrapper(config);
		Map<String, String> tags = new HashMap<>();
		tags.put("TESTING_ONESIGNAL_TAGS", "TEST");
		ViewDeviceResponse device = wrapper.viewDevice("73431135-4126-4ba6-91d4-d7f07efdb295");
		Map<String, String> initialTags = device.getTags();
		boolean response = wrapper.setTags("73431135-4126-4ba6-91d4-d7f07efdb295", tags);
		assertEquals(true, response);
		device = wrapper.viewDevice("73431135-4126-4ba6-91d4-d7f07efdb295");
		tags = device.getTags();
		assertEquals("TEST", tags.get("TESTING_ONESIGNAL_TAGS"));
		tags.remove("TESTING_ONESIGNAL_TAGS");
		response = wrapper.setTags("73431135-4126-4ba6-91d4-d7f07efdb295", initialTags);
		assertEquals(true, response);
	}
}
