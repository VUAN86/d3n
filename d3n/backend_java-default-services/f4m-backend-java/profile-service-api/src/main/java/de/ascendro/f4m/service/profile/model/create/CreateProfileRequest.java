package de.ascendro.f4m.service.profile.model.create;

import java.util.ArrayList;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;

public class CreateProfileRequest implements JsonMessageContent {
	private String email;
	private String phone;
	private String facebook;
	private String google;

	public CreateProfileRequest() {
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getFacebook() {
		return facebook;
	}

	public void setFacebook(String facebook) {
		this.facebook = facebook;
	}

	public String getGoogle() {
		return google;
	}

	public void setGoogle(String google) {
		this.google = google;
	}

	public ProfileIdentifierType[] getTypes() {
		final List<ProfileIdentifierType> profileIdTypes = new ArrayList<>();

		if (email != null) {
			profileIdTypes.add(ProfileIdentifierType.EMAIL);
		}
		if (phone != null) {
			profileIdTypes.add(ProfileIdentifierType.PHONE);
		}
		if (facebook != null) {
			profileIdTypes.add(ProfileIdentifierType.FACEBOOK);
		}
		if (google != null) {
			profileIdTypes.add(ProfileIdentifierType.GOOGLE);
		}

		return profileIdTypes.isEmpty() ? null
				: profileIdTypes.toArray(new ProfileIdentifierType[profileIdTypes.size()]);
	}

	public String getValue(ProfileIdentifierType type) {
		final String value;

		switch (type) {
		case EMAIL:
			value = email;
			break;
		case PHONE:
			value = phone;
			break;
		case FACEBOOK:
			value = facebook;
			break;
		case GOOGLE:
			value = google;
			break;
		default:
			value = null;
		}

		return value;
	}

}
