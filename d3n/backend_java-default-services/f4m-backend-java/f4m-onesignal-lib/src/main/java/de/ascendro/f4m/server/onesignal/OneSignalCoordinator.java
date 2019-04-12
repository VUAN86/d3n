package de.ascendro.f4m.server.onesignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.currencyfair.onesignal.OneSignalException;

import de.ascendro.f4m.server.onesignal.model.OneSignalPushData;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.profile.model.Profile;

public class OneSignalCoordinator {

	private static final Logger LOGGER = LoggerFactory.getLogger(OneSignalCoordinator.class);

	private OneSignalWrapper wrapper;
	private CommonProfileAerospikeDao commonProfileAerospikeDao;


	@Inject
	public OneSignalCoordinator(OneSignalWrapper wrapper, CommonProfileAerospikeDao commonProfileAerospikeDao) {
		this.wrapper = wrapper;
		this.commonProfileAerospikeDao = commonProfileAerospikeDao;
	}
	
	public void cancelMobilePush(String[] notificationIds) {
		Stream.of(notificationIds).forEach(notificationId -> cancelNotification(notificationId));
	}

	private void cancelNotification(String notificationId) {
		try {
			wrapper.cancelNotification(notificationId);
		} catch (OneSignalException e) {
			// avoid exception in case cancel arrived too late [after being sent to recipients]; 
			// allow best-effort manner processing
			LOGGER.info("Received cancel notification but notification is already sent with notificationId {}", notificationId);
		}
	}

	public List<String> sendMobilePush(Map<ISOLanguage, String> messages, String[] appIds, String topic) {
		List<String> messageIds = new ArrayList<>(appIds.length);
		for (int i = 0; i < appIds.length; i++) {
			messageIds.add(wrapper.sendMobilePushToTag(topic, appIds[i],  messages));
		}
		return messageIds;
	}
	
	public List<String> sendMobilePush(OneSignalPushData data) {
		return sendMobilePush(data, this::sendMobilePushToUser);
	}

	public List<String> sendScheduledMobilePush(OneSignalPushData data) {
		return sendMobilePush(data, this::sendMobilePushScheduled);
	}

	public void setTags(String profileId, Map<String, String> tags) {
		Profile profile = commonProfileAerospikeDao.getProfile(profileId);
		profile.getDevicesAsJsonArray().forEach(e -> wrapper.setTags(e.getAsJsonObject()
				.get(Profile.DEVICE_ONESIGNAL_PROPERTY_NAME).getAsString(), tags));
	}

	private String sendMobilePushToUser(OneSignalPushData data, Map<ISOLanguage, String> messages, String appId) {
		return wrapper.sendMobilePushToUser(data.getUserId(), appId, messages, data.getType(), data.getPayload());
	}
	
	private String sendMobilePushScheduled(OneSignalPushData data, Map<ISOLanguage, String> messages, String appId) {
		return wrapper.sendMobilePushWithScheduledTime(data.getUserId(), appId, messages, data.getType(), data.getPayload(), data.getScheduleDateTime());
	}
	
	private List<String> sendMobilePush(OneSignalPushData data,
			TriFunction<OneSignalPushData, Map<ISOLanguage, String>, String, String> sender) {
		Map<ISOLanguage, String> messages = data.getMessages();

		String[] appIds = data.getAppIds();
		List<String> messageIds = new ArrayList<>(appIds.length);
		for (int i = 0; i < appIds.length; i++) {
			String appId = appIds[i];
			messageIds.add(sender.apply(data, messages, appId));
		}
		return messageIds;
	}


	@FunctionalInterface
	public interface TriFunction<T, U, S, R> {
		R apply(T t, U u, S s);
	}
}
