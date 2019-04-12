package de.ascendro.f4m.service.profile.model.api;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileFacebookId;
import de.ascendro.f4m.service.profile.model.ProfileStats;

public class ApiProfile extends ApiUpdateableProfile {

	private String userId;
	private String email;
	private String phone;
	private Double handicap;
	private String createDate;
	private String profilePhotoId;
	private ApiProfileStats stats;
	private List<ProfileFacebookId> facebookIds;
	private String originCountry;
	
	public ApiProfile() {
		// Initialize empty object
		stats = new ApiProfileStats();
	}
	
	public ApiProfile(Profile profile) {
		super(profile);
		userId = profile.getUserId();
		List<String> emails = profile.getProfileEmails();
		email = CollectionUtils.isEmpty(emails) ? null : emails.iterator().next();
		List<String> phones = profile.getProfilePhones();
		phone = CollectionUtils.isEmpty(phones) ? null : phones.iterator().next();
		handicap = profile.getHandicap();
		createDate = profile.getPropertyAsString(Profile.CREATE_DATE_PROPERTY);
		profilePhotoId = profile.getPhotoId();
		stats = new ApiProfileStats();
		facebookIds = profile.getFacebookIds();
		originCountry = profile.getOriginCountry();
	}
	
	public String getUserId() {
		return userId;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public Double getHandicap() {
		return handicap;
	}

	public String getCreateDate() {
		return createDate;
	}

	public ApiProfileStats getStats() {
		return stats;
	}

	public void setStats(ProfileStats profileStats){
		stats = new ApiProfileStats(profileStats);
	}

	public String getProfilePhotoId() {
		return profilePhotoId;
	}

	public void setProfilePhotoId(String profilePhotoId) {
		this.profilePhotoId = profilePhotoId;
	}

	public List<ProfileFacebookId> getFacebookIds() {
		return facebookIds;
	}

	public String getOriginCountry() {
		return originCountry;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ApiProfile [");
		builder.append("userId=").append(userId);
		builder.append(", email=").append(email);
		builder.append(", phone=").append(phone);
		builder.append(", handicap=").append(handicap);
		builder.append(", createDate=").append(createDate);
		builder.append(", profilePhotoId=").append(profilePhotoId);
		builder.append(", settings=").append(getSettings());
		builder.append(", person=").append(getPerson());
		builder.append(", address=").append(getAddress());
		builder.append(", stats=").append(stats);
		builder.append("]");
		return builder.toString();
	}
}
