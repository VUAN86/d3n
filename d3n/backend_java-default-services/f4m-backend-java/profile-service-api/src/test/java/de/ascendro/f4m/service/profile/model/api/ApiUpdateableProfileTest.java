package de.ascendro.f4m.service.profile.model.api;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileUser;

public class ApiUpdateableProfileTest {

	@Test
	public void testPersonToProfile() {
		String firstName = " FirstName	";
		String lastName = "Last name";

		ProfileUser profileUser = new ProfileUser();
		profileUser.setFirstName(firstName);
		profileUser.setLastName(lastName);
		ApiProfilePerson person = new ApiProfilePerson(profileUser);
		ApiUpdateableProfile apiUpdateableProfile = new ApiUpdateableProfile(person, null, null);

		Profile profile = apiUpdateableProfile.toProfile();
		ProfileUser personWrapper = profile.getPersonWrapper();
		assertThat(personWrapper.getFirstName(), equalTo(profileUser.getFirstName()));
		assertThat(personWrapper.getLastName(), equalTo(lastName));
	}

	@Test
	public void testDefaultValues() throws Exception {
		@SuppressWarnings("unchecked")
		Profile profile = Profile.create("userId", ISOCountry.DE.toString());
		ApiProfile api = new ApiProfile(profile);
		ApiProfileSettingsPush settingsPush = api.getSettings().getPush();
		assertTrue(settingsPush.isPushNotificationsDuel());
		assertTrue(settingsPush.isPushNotificationsFriendActivity());
		assertTrue(settingsPush.isPushNotificationsNews());
		assertTrue(settingsPush.isPushNotificationsTombola());
		assertTrue(settingsPush.isPushNotificationsTournament());
		assertFalse(api.getSettings().isAutoShare());
		assertFalse(api.getSettings().isMoneyGames());
		assertTrue(api.getSettings().isShowFullName());
	}
}
