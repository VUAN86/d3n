package de.ascendro.f4m.service.profile.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.json.model.ImageUrl;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.F4MEnumUtils;

public class Profile extends JsonObjectWrapper implements ImageUrl {

	public static final String TENANT_ROLE_PREFIX = "TENANT_";
	public static final String TENANT_ROLE_SEPARATOR = "_";

	public static final String USER_ID_PROPERTY = "userId";
	public static final String ROLES_PROPERTY = "roles";
	public static final String EMAILS_PROPERTY = "emails";
	public static final String EMAIL_PROPERTY = "email";
	public static final String PHONES_PROPERTY = "phones";
	public static final String PHONE_PROPERTY = "phone";
	public static final String VERIFICATION_STATUS_PROPERTY = "verificationStatus";
    public static final String LANGUAGE_PROPERTY = "language";
    public static final String PHOTO_ID_PROPERTY = "profilePhotoId";
    public static final String REGION_PROPERTY = "region";
	public static final String GOOGLE_PROPERTY = "google";
	public static final String NICKNAME_PROPERTY = "nickname";
	/**
	 * facebook access token
	 */
	public static final String FACEBOOK_PROPERTY = "facebook";
	/**
	 * Facebook removed username from it's latest API (starting at 2.0, currently 2.9); each ID maps to appropriate
	 * FB app (frontend knows appId) and can be accessible by concatenating facebook.com/ with ID
	 */
	public static final String FACEBOOK_IDS_PROPERTY = "facebookIds";
	public static final String INVITATION_AUTO_ACCEPT_PROPERTY = "invitationAutoAccept";
	public static final String CREATE_DATE_PROPERTY = "createDate";

	public static final String DEVICES_PROPERTY_NAME = "devices";
	public static final String DEVICE_UUID_PROPERTY_NAME = "deviceUUID";
	public static final String DEVICE_IMEI_PROPERTY_NAME = "IMEI";
	public static final String DEVICE_ONESIGNAL_PROPERTY_NAME = "oneSignalId";

	public static final String MONEY_GAMES_PROPERTY = "moneyGames";
	public static final String AUTO_SHARE_PROPERTY = "autoShare";
	public static final String SHOW_FULL_NAME_PROPERTY = "showFullName";
	public static final String GAME_SETTINGS_PROPERTY = "gameSettings";
	public static final String PUSH_NOTIFICATIONS_FRIEND_ACTIVITY_PROPERTY = "pushNotificationsFriendActivity";
	public static final String PUSH_NOTIFICATIONS_TOURNAMENT_PROPERTY = "pushNotificationsTournament";
	public static final String PUSH_NOTIFICATIONS_DUEL_PROPERTY = "pushNotificationsDuel";
	public static final String PUSH_NOTIFICATIONS_TOMBOLA_PROPERTY = "pushNotificationsTombola";
	public static final String PUSH_NOTIFICATIONS_NEWS_PROPERTY = "pushNotificationsNews";
	
	public static final String TENANTS_PROPERTY_NAME = "tenants";
	public static final String APPLICATIONS_PROPERTY_NAME = "applications";

	public static final String SUB_BLOB_NAMES_PROPERTY_NAME = "subBlobNames";

	public static final String PERSON_PROPERTY = "person";

	public static final String ADDRESS_PROPERTY = "address";

	public static final String RECOMMENDED_BY_PROPERTY = "recommendedBy";
	
	public static final String HANDICAP_PROPERTY = "handicap";

	public static final String ORIGIN_COUNTRY = "originCountry";
	
	public static final String[] NEVER_UPDATABLE_PROPERTIES = new String[] { USER_ID_PROPERTY, DEVICES_PROPERTY_NAME,
			CREATE_DATE_PROPERTY, ORIGIN_COUNTRY };
	private static final int EIGHTEEN = 18;

	public Profile(JsonElement profileJsonElement) {
		if (profileJsonElement != null) {
			this.jsonObject = profileJsonElement.getAsJsonObject();
		} else {
			this.jsonObject = null;
		}
	}

	public Profile() {
		this.jsonObject = new JsonObject();
	}

	public void setUserId(String profileId) {
		jsonObject.addProperty(USER_ID_PROPERTY, profileId);
	}

	public String getUserId() {
		final JsonElement userIdElement = jsonObject.get(USER_ID_PROPERTY);

		final String userId;
		if (userIdElement != null) {
			userId = userIdElement.getAsString();
		} else {
			userId = null;
		}

		return userId;
	}

	public List<Pair<ProfileIdentifierType, String>> getIdentifiersByType(ProfileIdentifierType identifierType) {
		return getIdentifiers().stream() //
				.filter(i -> identifierType == i.getKey()) //
				.collect(Collectors.toList());
	}

	public List<Pair<ProfileIdentifierType, String>> getIdentifiers() {
		final List<Pair<ProfileIdentifierType, String>> profileIdentifiers = new ArrayList<>();

		final List<String> emails = getProfileEmails();
		if (emails != null && !emails.isEmpty()) {
			emails.forEach(
					e -> profileIdentifiers.add(pairFromIdentifier(ProfileIdentifierType.EMAIL, e.toLowerCase())));
		}

		final List<String> phones = getProfilePhones();
		if (phones != null && !phones.isEmpty()) {
			phones.forEach(e -> profileIdentifiers.add(pairFromIdentifier(ProfileIdentifierType.PHONE, e)));
		}

		final List<String> oneSignalDeviceIds = getProfileOneSignalDeviceIds();
		if (oneSignalDeviceIds != null && !oneSignalDeviceIds.isEmpty()) {
			oneSignalDeviceIds.forEach(e -> profileIdentifiers.add(pairFromIdentifier(ProfileIdentifierType.ONE_SIGNAL_ID, e)));
		}

		final String facebook = getProfileFacebook();
		if (facebook != null) {
			profileIdentifiers.add(pairFromIdentifier(ProfileIdentifierType.FACEBOOK, facebook));
		}

		final String google = getProfileGoogle();
		if (google != null) {
			profileIdentifiers.add(pairFromIdentifier(ProfileIdentifierType.GOOGLE, google));
		}
		
		if (getPersonWrapper() != null && StringUtils.isNotEmpty(getPersonWrapper().getNickname())) {
			profileIdentifiers.add(
					pairFromIdentifier(ProfileIdentifierType.NICKNAME, getPersonWrapper().getNickname().toLowerCase()));
		}

		return profileIdentifiers;
	}
	
	private Pair<ProfileIdentifierType, String> pairFromIdentifier(ProfileIdentifierType identifierType, String identifier) {
		return new ImmutablePair<>(identifierType, cleanupIdentifierForStorage(identifierType, identifier));
	}

	public static String cleanupIdentifierForStorage(ProfileIdentifierType identifierType, String identifier) {
		if (ProfileIdentifierType.EMAIL == identifierType || ProfileIdentifierType.NICKNAME == identifierType) {
			return identifier.toLowerCase();
		} else {
			return identifier;
		}
	}

	public ISOLanguage getLanguage() {
		String language = getPropertyAsString(LANGUAGE_PROPERTY);
		return ISOLanguage.fromString(language);
	}
	
	public void setLanguage(ISOLanguage language) {
		setProperty(LANGUAGE_PROPERTY, language == null ? null : language.getValue());
	}
	
	public String getPhotoId() {
		return getPropertyAsString(PHOTO_ID_PROPERTY);
	}
	
	public void setPhotoId(String photoId) {
		setProperty(PHOTO_ID_PROPERTY, photoId);
	}
	
	public String getRegion() {
		return getPropertyAsString(REGION_PROPERTY);
	}
	
	public void setRegion(String region) {
		setProperty(REGION_PROPERTY, region);
	}
	
	public String getProfileFacebook() {
		return getPropertyAsString(FACEBOOK_PROPERTY);
	}

	public List<ProfileFacebookId> getFacebookIds() {
		List<ProfileFacebookId> items = new ArrayList<>();
		JsonArray jsonArray = getPropertyAsJsonArray(FACEBOOK_IDS_PROPERTY);
		if(jsonArray != null) {
			jsonArray.forEach(item -> items.add(new ProfileFacebookId(item.getAsJsonObject())));
		}
		return items;
	}

	public String getProfileGoogle() {
		return getPropertyAsString(GOOGLE_PROPERTY);
	}

	public String getRecommendedBy() {
		return getPropertyAsString(RECOMMENDED_BY_PROPERTY);
	}
	
	public String getFullNameOrNickname() {
		return isShowFullName() ? getFullName() : getNickname();
	}
	
	public String getFullName() {
		ProfileUser person = getPersonWrapper();
		return person == null ? null : String.join(" ", StringUtils.trim(person.getFirstName()), StringUtils.trim(person.getLastName()));
	}
	
	public String getNickname() {
		ProfileUser person = getPersonWrapper();
		return person == null ? null : StringUtils.trim(person.getNickname());
	}

	public String getSearchName() {
		return StringUtils.lowerCase(getFullNameOrNickname());
	}
	
	public ProfileUser getPersonWrapper() {
		JsonObject person = getPropertyAsJsonObject(PERSON_PROPERTY);
		ProfileUser personWrapper = null;
		if (person != null && person.isJsonObject()) {
			personWrapper = new ProfileUser(person.getAsJsonObject());
		}
		return personWrapper;
	}

	public void setPersonWrapper(ProfileUser person) {
		setProperty(PERSON_PROPERTY, person.getJsonObject());
	}
	
	public ProfileSettingsGame getSettingsGameWrapper() {
		JsonObject settings = getPropertyAsJsonObject(GAME_SETTINGS_PROPERTY);
		ProfileSettingsGame settingsWrapper = null;
		if (settings != null && settings.isJsonObject()) {
			settingsWrapper = new ProfileSettingsGame(settings.getAsJsonObject());
		}
		return settingsWrapper;
	}

	public void setSettingsGameWrapper(ProfileSettingsGame settingsWrapper) {
		setProperty(GAME_SETTINGS_PROPERTY, settingsWrapper.getJsonObject());
	}
	
	public ProfileAddress getAddress() {
		JsonObject address = getPropertyAsJsonObject(ADDRESS_PROPERTY);
		return address == null || ! address.isJsonObject() ? null : new ProfileAddress(address);
	}
	
	public void setAddress(ProfileAddress address) {
		setProperty(ADDRESS_PROPERTY, address == null ? null : address.getJsonObject());
	}
	
	public String getCity() {
		String city = null;
		JsonObject address = getPropertyAsJsonObject(ADDRESS_PROPERTY);
		if (address != null && address.isJsonObject()) {
			ProfileAddress addressWrapper = new ProfileAddress(address);
			city = addressWrapper.getCity();
		}
		return city;
	}

	public void setCity(String city) {
		JsonObject address = getPropertyAsJsonObject(ADDRESS_PROPERTY);
		ProfileAddress addressWrapper = new ProfileAddress(address != null ? address : new JsonObject());
		addressWrapper.setCity(city);
		jsonObject.add(ADDRESS_PROPERTY, addressWrapper.getJsonObject());
	}

	public String getCountry() {
		String country = null;
		JsonObject address = getPropertyAsJsonObject(ADDRESS_PROPERTY);
		if (address != null && address.isJsonObject()) {
			ProfileAddress addressWrapper = new ProfileAddress(address);
			country = addressWrapper.getCountry();
		}
		return country;
	}

	public void setCountry(String country) {
		JsonObject address = getPropertyAsJsonObject(ADDRESS_PROPERTY);
		ProfileAddress addressWrapper = new ProfileAddress(address);
		addressWrapper.setCountry(country);
		jsonObject.add(ADDRESS_PROPERTY, addressWrapper.getJsonObject());
	}

	public String getPostalCode() {
		String postalCode = null;
		JsonObject address = getPropertyAsJsonObject(ADDRESS_PROPERTY);
		if (address != null && address.isJsonObject()) {
			ProfileAddress addressWrapper = new ProfileAddress(address);
			postalCode = addressWrapper.getPostalCode();
		}
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		JsonObject address = getPropertyAsJsonObject(ADDRESS_PROPERTY);
		ProfileAddress addressWrapper = new ProfileAddress(address);
		addressWrapper.setPostalCode(postalCode);
		jsonObject.add(ADDRESS_PROPERTY, addressWrapper.getJsonObject());
	}

	public String getStreet() {
		String street = null;
		JsonObject address = getPropertyAsJsonObject(ADDRESS_PROPERTY);
		if (address != null && address.isJsonObject()) {
			ProfileAddress addressWrapper = new ProfileAddress(address);
			street = addressWrapper.getStreet();
		}
		return street;
	}

	public void setStreet(String street) {
		JsonObject address = getPropertyAsJsonObject(ADDRESS_PROPERTY);
		ProfileAddress addressWrapper = new ProfileAddress(address);
		addressWrapper.setStreet(street);
		jsonObject.add(ADDRESS_PROPERTY, addressWrapper.getJsonObject());
	}

	@Override
	public String getImage() {
		StringBuilder sb = new StringBuilder();
		final JsonElement userIdElement = jsonObject.get(USER_ID_PROPERTY);

		if (userIdElement != null) {
			sb.append(IMAGE_URL_SEPARATOR);
			sb.append(ProfileMessageTypes.SERVICE_NAME);
			sb.append(IMAGE_URL_SEPARATOR);
			sb.append(userIdElement.getAsString());
			sb.append(IMAGE_EXTENSION);
		}
		return sb.toString();
	}
	
	public boolean isInvitationAutoAccept(){
		Boolean autoAccept = getPropertyAsBoolean(INVITATION_AUTO_ACCEPT_PROPERTY);
		return autoAccept == null ? false : autoAccept;
	}

	public void setInvitationAutoAccept(boolean autoAccept) {
		setProperty(INVITATION_AUTO_ACCEPT_PROPERTY, autoAccept);
	}

	public List<String> getProfilePhones() {
		return getProfilePropertiesFormObjectArray(PHONES_PROPERTY, PHONE_PROPERTY);
	}

	public List<String> getProfileOneSignalDeviceIds() {
		return getProfilePropertiesFormObjectArray(DEVICES_PROPERTY_NAME, DEVICE_ONESIGNAL_PROPERTY_NAME);
	}

	public List<String> getProfileEmails() {
		return getProfilePropertiesFormObjectArray(EMAILS_PROPERTY, EMAIL_PROPERTY);
	}

	public List<String> getProfilePropertiesFormObjectArray(String arrayPropertyName,
															String arrayElementObjectPropertyName) {
		final List<String> values = new ArrayList<>();

		final JsonElement propertyElement = jsonObject.get(arrayPropertyName);
		if (propertyElement != null && propertyElement.isJsonArray()) {
			for (JsonElement arrayElement : propertyElement.getAsJsonArray()) {
				if (arrayElement.isJsonObject()) {
					final JsonElement arrayElementObjectProperty = arrayElement.getAsJsonObject().get(
							arrayElementObjectPropertyName);
					if (arrayElementObjectProperty != null && arrayElementObjectProperty.isJsonPrimitive()) {
						values.add(arrayElementObjectProperty.getAsString());
					}
				}
			}
		}

		return values;
	}

	public void setProfileIdentifier(ProfileIdentifierType profileIdType, String identifier) {
		switch (profileIdType) {
		case EMAIL:
			final JsonObject email = new JsonObject();
			email.addProperty(EMAIL_PROPERTY, identifier);

			final JsonArray emails = new JsonArray();
			emails.add(email);

			jsonObject.add(EMAILS_PROPERTY, emails);

			break;
		case PHONE:
			final JsonObject phone = new JsonObject();
			phone.addProperty(VERIFICATION_STATUS_PROPERTY, VerificationStatus.notVerified.name());
			phone.addProperty(PHONE_PROPERTY, identifier);

			final JsonArray phones = new JsonArray();
			phones.add(phone);

			jsonObject.add(PHONES_PROPERTY, phones);
			break;
		case FACEBOOK:
			jsonObject.addProperty(FACEBOOK_PROPERTY, identifier);
			break;
		case GOOGLE:
			jsonObject.addProperty(GOOGLE_PROPERTY, identifier);
			break;
		case NICKNAME:
			jsonObject.addProperty(NICKNAME_PROPERTY, identifier);
			break;
		default:
			throw new IllegalArgumentException("Unknown identifier type:" + profileIdType);
		}
	}

	public JsonArray getDevicesAsJsonArray() {
		final JsonElement devicesElement = jsonObject.get(DEVICES_PROPERTY_NAME);

		final JsonArray devicesArray;
		if (devicesElement != null) {
			devicesArray = devicesElement.getAsJsonArray();
		} else {
			devicesArray = new JsonArray();
		}

		return devicesArray;
	}

	public Integer indexOfDeviceById(String deviceUUID) {
		final JsonArray devicesArray = getDevicesAsJsonArray();
		Integer foundDeviceIndex = null;
		for (int i = 0; i < devicesArray.size(); i++) {
			final JsonElement deviceElement = devicesArray.get(i);
			final JsonElement deviceIdElement = deviceElement.getAsJsonObject().get(DEVICE_UUID_PROPERTY_NAME);
			if (deviceIdElement != null) {
				if (deviceUUID.equalsIgnoreCase(deviceIdElement.getAsString())) {
					foundDeviceIndex = i;
					break;
				}
			}
		}
		return foundDeviceIndex;
	}

	public boolean mergeDeviceIntoDevicesArray(String deviceUUID, JsonElement deviceProperties, String IMEI, String oneSignalDeviceId) {
		final Integer foundDeviceIndex = indexOfDeviceById(deviceUUID);

		deviceProperties.getAsJsonObject().addProperty(DEVICE_UUID_PROPERTY_NAME, deviceUUID);
		deviceProperties.getAsJsonObject().addProperty(DEVICE_IMEI_PROPERTY_NAME, IMEI);
		deviceProperties.getAsJsonObject().addProperty(DEVICE_ONESIGNAL_PROPERTY_NAME, oneSignalDeviceId);
		boolean valueChanged;
		if (foundDeviceIndex != null) {
			JsonElement previous = getDevicesAsJsonArray().set(foundDeviceIndex, deviceProperties);
			valueChanged = Objects.equals(previous, deviceProperties);
		} else {
			JsonArray devicesArray = getDevicesAsJsonArray();
			if (devicesArray == null) {
				devicesArray = new JsonArray();
			}
			devicesArray.add(deviceProperties);
			jsonObject.add(DEVICES_PROPERTY_NAME, devicesArray);
			valueChanged = true;
		}
		return valueChanged;
	}

	@SuppressWarnings("unchecked")
	public static Profile create(String userId,String countryCode, Pair<ProfileIdentifierType, String>... profileIdentifiers) {
		final Profile profile = new Profile();
		profile.setUserId(userId);
		
		profile.setOriginCountry(countryCode);
		profile.setProperty(CREATE_DATE_PROPERTY, DateTimeUtil.getCurrentDateTime());

		if (profileIdentifiers != null) {
			Arrays.stream(profileIdentifiers).forEach(pi -> profile.setProfileIdentifier(pi.getLeft(), pi.getRight()));
		}
		profile.setPushNotificationsDuel(true);
		profile.setPushNotificationsFriendActivity(true);
		profile.setPushNotificationsNews(true);
		profile.setPushNotificationsTombola(true);
		profile.setPushNotificationsTournament(true);
		return profile;
	}

	public ZonedDateTime getCreateDate() {
		return getPropertyAsZonedDateTime(CREATE_DATE_PROPERTY);
	}
	
	public boolean addTenant(String tenantId) {
		Set<String> tenants = getTenants();
		if (tenants == null) {
			tenants = new HashSet<>();
		}
		boolean added = tenants.add(tenantId);
		setArray(TENANTS_PROPERTY_NAME, tenants);
		return added;
	}

	public boolean addApplication(String applicationId) {
		Set<String> applications = getApplications();
		if (applications == null) {
			applications = new HashSet<>();
		}
		boolean added = applications.add(applicationId);
		setArray(APPLICATIONS_PROPERTY_NAME, applications);
		return added;
	}

	public Set<String> getTenants() {
		return getPropertyAsStringSet(TENANTS_PROPERTY_NAME);
	}

	public Set<String> getApplications() {
		return getPropertyAsStringSet(APPLICATIONS_PROPERTY_NAME);
	}

	public void addSubBlobName(String subBlobName) {
		Set<String> applications = getSubBlobNames();
		if (applications == null) {
			applications = new HashSet<>();
		}
		applications.add(subBlobName);
		setArray(SUB_BLOB_NAMES_PROPERTY_NAME, applications);
	}

	public Set<String> getSubBlobNames() {
		return getPropertyAsStringSet(SUB_BLOB_NAMES_PROPERTY_NAME);
	}

	public void setSubBlobNames(Set<String> blobNames) {
		if (blobNames != null && !blobNames.isEmpty()) {
			final JsonArray blobNamesArray = new JsonArray();

			blobNames.forEach(n -> blobNamesArray.add(n));

			jsonObject.add(SUB_BLOB_NAMES_PROPERTY_NAME, blobNamesArray);
		} else {
			jsonObject.remove(SUB_BLOB_NAMES_PROPERTY_NAME);
		}
	}
	
	public void setHandicap(Double handicap){
		setProperty(HANDICAP_PROPERTY, handicap);
	}

	public Double getHandicap(){
		return getPropertyAsDoubleObject(HANDICAP_PROPERTY);
	}


	
	public String[] getRoles(String tenantId) {
		String[] roles = getPropertyAsStringArray(ROLES_PROPERTY);
		String tenantPrefix = TENANT_ROLE_PREFIX + tenantId + TENANT_ROLE_SEPARATOR;
		if (roles != null) {
			Set<String> result = new HashSet<>(roles.length);
			for (String role : roles) {
				if (! role.startsWith(TENANT_ROLE_PREFIX)) {
					result.add(role.toUpperCase()); // global role
				} else if (role.startsWith(tenantPrefix)) {
					result.add(StringUtils.removeStart(role, tenantPrefix).toUpperCase()); // tenant-specific role
				}
			}
			return result.toArray(new String[result.size()]);
		}
		return new String[]{};
	}
	
	public boolean isMoneyGames() {
		Boolean result = getPropertyAsBoolean(MONEY_GAMES_PROPERTY);
		return result == null ? false : result;
	}
	
	public void setMoneyGames(boolean moneyGames) {
		setProperty(MONEY_GAMES_PROPERTY, moneyGames);
	}
	
	public boolean isAutoShare() {
		Boolean result = getPropertyAsBoolean(AUTO_SHARE_PROPERTY);
		return result == null ? false : result;
	}
	
	public void setAutoShare(boolean autoShare) {
		setProperty(AUTO_SHARE_PROPERTY, autoShare);
	}
	
	public boolean isShowFullName() {
		Boolean result = getPropertyAsBoolean(SHOW_FULL_NAME_PROPERTY);
		return result == null ? true : result;
	}
	
	public void setShowFullName(boolean showFullName) {
		setProperty(SHOW_FULL_NAME_PROPERTY, showFullName);
	}
	
	public boolean isPushNotificationsDuel() {
		Boolean result = getPropertyAsBoolean(PUSH_NOTIFICATIONS_DUEL_PROPERTY);
		return result == null ? true : result;
	}

	public void setPushNotificationsDuel(boolean pushNotificationsDuel) {
		setProperty(PUSH_NOTIFICATIONS_DUEL_PROPERTY, pushNotificationsDuel);
	}
	
	public boolean isPushNotificationsFriendActivity() {
		Boolean result = getPropertyAsBoolean(PUSH_NOTIFICATIONS_FRIEND_ACTIVITY_PROPERTY);
		return result == null ? true : result;
	}
	
	public void setPushNotificationsFriendActivity(boolean pushNotificationsFriendActivity) {
		setProperty(PUSH_NOTIFICATIONS_FRIEND_ACTIVITY_PROPERTY, pushNotificationsFriendActivity);
	}
	
	public boolean isPushNotificationsTombola() {
		Boolean result = getPropertyAsBoolean(PUSH_NOTIFICATIONS_TOMBOLA_PROPERTY);
		return result == null ? true : result;
	}
	
	public void setPushNotificationsTombola(boolean pushNotificationsTombola) {
		setProperty(PUSH_NOTIFICATIONS_TOMBOLA_PROPERTY, pushNotificationsTombola);
	}
	
	public boolean isPushNotificationsTournament() {
		Boolean result = getPropertyAsBoolean(PUSH_NOTIFICATIONS_TOURNAMENT_PROPERTY);
		return result == null ? true : result;
	}
	
	public void setPushNotificationsTournament(boolean pushNotificationsTournament) {
		setProperty(PUSH_NOTIFICATIONS_TOURNAMENT_PROPERTY, pushNotificationsTournament);
	}

	public boolean isPushNotificationsNews() {
		Boolean result = getPropertyAsBoolean(PUSH_NOTIFICATIONS_NEWS_PROPERTY);
		return result == null ? true : result;
	}
	
	public void setPushNotificationsNews(boolean pushNotificationsNews) {
		setProperty(PUSH_NOTIFICATIONS_NEWS_PROPERTY, pushNotificationsNews);
	}
	
	public String getLatestVerifiedEmail() {
		JsonObject phone = getLatestVerifiedEmailOrPhoneJsonObject(EMAILS_PROPERTY);
		if (phone != null) {
			return phone.getAsJsonObject().get(EMAIL_PROPERTY).getAsString();
		} else {
			return null;
		}
	}

	public String getLatestVerifiedPhone() {
		JsonObject phone = getLatestVerifiedEmailOrPhoneJsonObject(PHONES_PROPERTY);
		if (phone != null) {
			return phone.getAsJsonObject().get(PHONE_PROPERTY).getAsString();
		} else {
			return null;
		}
	}

	public boolean isPhonesVerified() {
		return getLatestVerifiedEmailOrPhoneJsonObject(PHONES_PROPERTY) != null;
	}

	private JsonObject getLatestVerifiedEmailOrPhoneJsonObject(String propertyName) {
		final JsonArray phones = getArray(propertyName);
		JsonObject resultPhone = null;
		if (phones != null) {
			for (JsonElement phone : phones) {
				if (phone != null && phone.isJsonObject()) {
					JsonElement verificationStatus = phone.getAsJsonObject().get(VERIFICATION_STATUS_PROPERTY);
					if (verificationStatus != null && verificationStatus.isJsonPrimitive() 
							&& F4MEnumUtils.getEnum(VerificationStatus.class, verificationStatus.getAsString()) == VerificationStatus.verified) {
						resultPhone = phone.getAsJsonObject();
					}
				}
			}
		}
		return resultPhone;
	}

	public void removePhones() {
		jsonObject.remove(PHONES_PROPERTY);
	}

	public void setOriginCountry(String countryCode) {
		jsonObject.addProperty(ORIGIN_COUNTRY, countryCode);
	}

	public String getOriginCountry() {
		return getPropertyAsString(ORIGIN_COUNTRY);
	}
	
	public boolean isOver18() {
		boolean isOver18 = false;
		ProfileUser person = getPersonWrapper();
		if (person != null && person.getBirthDate() != null) {
			ZonedDateTime currentDateEnd = DateTimeUtil.getCurrentDateEnd();
			isOver18 = person.getBirthDate().isBefore(currentDateEnd.minusYears(EIGHTEEN));
		}
		return isOver18;
	}
	
}
