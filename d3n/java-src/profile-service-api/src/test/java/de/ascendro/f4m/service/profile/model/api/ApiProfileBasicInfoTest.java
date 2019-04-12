package de.ascendro.f4m.service.profile.model.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileUser;

public class ApiProfileBasicInfoTest {
	private Profile profile;

	@Before
	public void setUp() {
		ProfileUser user = new ProfileUser(); 
		user.setFirstName("first");
		user.setLastName("last");
		user.setNickname("nick");
		profile = new Profile();
		profile.setPersonWrapper(user);
	}

	@Test
	public void testPrivateInfoIsHidden() throws Exception {
		profile.setShowFullName(false);
		ApiProfileBasicInfo info = new ApiProfileBasicInfo(profile);
		assertThat(info.getFirstName(), nullValue());
		assertThat(info.getLastName(), nullValue());
		assertThat(info.getNickname(), equalTo("nick"));
		assertThat(info.getName(), equalTo("nick"));
	}

	@Test
	public void testPrivateInfoIsAvailable() throws Exception {
		ApiProfileBasicInfo info = new ApiProfileBasicInfo(profile);
		assertThat(info.getFirstName(), equalTo("first"));
		assertThat(info.getLastName(), equalTo("last"));
		assertThat(info.getNickname(), equalTo("nick"));
		assertThat(info.getName(), equalTo("first last"));
	}
}
