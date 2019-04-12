package de.ascendro.f4m.server.onesignal;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.currencyfair.onesignal.OneSignal;
import com.currencyfair.onesignal.model.SuccessResponse;
import com.currencyfair.onesignal.model.notification.CreateNotificationResponse;
import com.currencyfair.onesignal.model.notification.Field;
import com.currencyfair.onesignal.model.notification.Filter;
import com.currencyfair.onesignal.model.notification.NotificationRequest;
import com.currencyfair.onesignal.model.notification.NotificationRequestBuilder;
import com.currencyfair.onesignal.model.notification.Relation;
import com.currencyfair.onesignal.model.player.AddEditDeviceRequest;
import com.currencyfair.onesignal.model.player.AddEditDeviceRequestBuilder;
import com.currencyfair.onesignal.model.player.ViewDeviceResponse;

import de.ascendro.f4m.server.onesignal.config.OneSignalConfigImpl;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class OneSignalWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(OneSignalWrapper.class);

	public static final String TAG_APP = "APP";
	public static final String TAG_PROFILE = "PROFILE";
	private static final String TYPE = "type";
	private static final String PAYLOAD = "payload";
	private static final String USER_ID = "userId";


	private String oneSignalAppId;
	private String oneSignalAppRestApiKey;
	
	@Inject
	public OneSignalWrapper(OneSignalConfigImpl config) {
		oneSignalAppId = config.getProperty(OneSignalConfigImpl.ONE_SIGNAL_APP_ID);
		oneSignalAppRestApiKey = config.getProperty(OneSignalConfigImpl.ONE_SIGNAL_APP_REST_API_KEY);
	}
	
	public void cancelNotification(String notificationId) {
		OneSignal.cancelNotification(oneSignalAppRestApiKey, oneSignalAppId, notificationId);
	}

	public String sendMobilePushToUser(String profileId, String appId, Map<ISOLanguage, String> messages,
			String websocketMessageType, String payload) {
		return sendMobilePush(messages, appId, new Filter(Field.TAG, TAG_PROFILE, Relation.EQUALS, profileId),
				websocketMessageType, null, payload);
	}

	public String sendMobilePushToUser(String profileId, String appId, Map<ISOLanguage, String> messages) {
		return sendMobilePush(messages, appId, new Filter(Field.TAG, TAG_PROFILE, Relation.EQUALS, profileId), null,
				null, null);
	}

	public String sendMobilePushToTag(String tag, String appId, Map<ISOLanguage, String> messages) {
		return sendMobilePush(messages, appId, new Filter(Field.TAG, tag, Relation.EXISTS, null), null, null, null);
	}

	public String sendMobilePushWithScheduledTime(String profileId, String appId, Map<ISOLanguage, String> messages,
                                                  String websocketMessageType, String payload, ZonedDateTime scheduledTime) {
		return sendMobilePush(messages, appId, new Filter(Field.TAG, TAG_PROFILE, Relation.EQUALS, profileId), websocketMessageType,
				scheduledTime, payload);
	}

	private String sendMobilePush(Map<ISOLanguage, String> messages, String appId, Filter filter,
                                  String websocketMessageType, ZonedDateTime scheduledTime, String payload) {
		List<Filter> filters = new ArrayList<>();
		filters.add(filter);
		if (StringUtils.isNotEmpty(appId)) {
			filters.add(new Filter(Field.TAG, TAG_APP, Relation.EQUALS, appId));
		}
		// filter contains the profileId (userId) and for topic push it is null
		String userId = TAG_PROFILE.equals(filter.getKey()) ? filter.getValue() : null;

		NotificationRequestBuilder builder = NotificationRequestBuilder.aNotificationRequest().withFilters(filters)
				.withAppId(oneSignalAppId);

		Map<String, String> additionalData = new HashMap<>();
		additionalData.put(PAYLOAD, payload);
		additionalData.put(TYPE, (websocketMessageType != null ? websocketMessageType.toString() : null));
		additionalData.put(USER_ID, userId);
		builder.withData(additionalData);

		if (scheduledTime != null && scheduledTime.isAfter(DateTimeUtil.getCurrentDateTime())) {
			builder.withSendAfter(scheduledTime.toString());
		}

		for (Entry<ISOLanguage, String> message : messages.entrySet()) {
			//probably headers will be needed too - front-end will request it, if necessary
			builder.withContent(message.getKey().toString(), message.getValue());
		}

		NotificationRequest notificationRequest = builder.build();
		CreateNotificationResponse response = OneSignal.createNotification(oneSignalAppRestApiKey, notificationRequest);
		return response.getId();
	}

	public ViewDeviceResponse viewDevice(String oneSignalDeviceId) {
		return OneSignal.viewDevice(oneSignalAppRestApiKey, oneSignalAppId, oneSignalDeviceId);
	}

	private boolean editDevice(String oneSignalDeviceId, AddEditDeviceRequest editDeviceRequest) {
		SuccessResponse response = OneSignal.editDevice(oneSignalDeviceId, editDeviceRequest);
		return response.getSuccess();
	}

	public boolean setTags(String oneSignalDeviceId, Map<String, String> newTags) {
		ViewDeviceResponse response = this.viewDevice(oneSignalDeviceId);
		Map<String, String> tags = new HashMap<>();
		// essentially clear old tags, so we are always up to date
		response.getTags().forEach((k, v) -> tags.put(k, null));
		newTags.forEach((k, v) -> tags.put(k, v));

		AddEditDeviceRequest request = AddEditDeviceRequestBuilder.anAddEditDeviceRequest()
				.withAppId(oneSignalAppId).withTags(tags).build();
		return editDevice(oneSignalDeviceId, request);
	}
}
